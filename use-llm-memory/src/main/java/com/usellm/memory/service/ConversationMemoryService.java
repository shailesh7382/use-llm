package com.usellm.memory.service;

import com.usellm.core.model.Message;
import com.usellm.core.model.Role;
import com.usellm.core.port.MemoryPort;
import com.usellm.memory.config.MemoryConfig;
import com.usellm.memory.entity.ConversationEntity;
import com.usellm.memory.entity.MessageEntity;
import com.usellm.memory.repository.ConversationRepository;
import com.usellm.memory.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService implements MemoryPort {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MemoryConfig memoryConfig;

    // In-memory cache for fast access
    private final ConcurrentHashMap<String, LinkedList<Message>> cache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void addMessage(String conversationId, Message message) {
        log.debug("Adding message to conversation {}: role={}", conversationId, message.getRole());

        // Ensure conversation exists
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseGet(() -> {
                    ConversationEntity newConv = ConversationEntity.builder()
                            .conversationId(conversationId)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return conversationRepository.save(newConv);
                });

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        int nextPosition = messageRepository.findMaxPositionByConversationId(conversationId) + 1;
        int tokenCount = message.estimateTokens();

        MessageEntity entity = MessageEntity.builder()
                .conversation(conversation)
                .role(message.getRole().getValue())
                .content(message.getContent())
                .position(nextPosition)
                .tokenCount(tokenCount)
                .createdAt(Instant.now())
                .build();

        messageRepository.save(entity);

        // Update cache
        cache.computeIfAbsent(conversationId, k -> new LinkedList<>()).addLast(message);

        log.debug("Message added at position {} with ~{} tokens", nextPosition, tokenCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessages(String conversationId) {
        // Check cache first
        if (cache.containsKey(conversationId)) {
            return Collections.unmodifiableList(new ArrayList<>(cache.get(conversationId)));
        }

        List<MessageEntity> entities = messageRepository.findByConversationId(conversationId);
        List<Message> messages = entities.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());

        cache.put(conversationId, new LinkedList<>(messages));
        return Collections.unmodifiableList(messages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(String conversationId, int maxMessages) {
        List<Message> all = getMessages(conversationId);
        if (all.size() <= maxMessages) {
            return all;
        }
        // Always include system message if first message is system
        List<Message> trimmed = new ArrayList<>();
        if (!all.isEmpty() && all.get(0).getRole() == Role.SYSTEM) {
            trimmed.add(all.get(0));
            List<Message> rest = all.subList(1, all.size());
            int startIdx = Math.max(0, rest.size() - (maxMessages - 1));
            trimmed.addAll(rest.subList(startIdx, rest.size()));
        } else {
            int startIdx = Math.max(0, all.size() - maxMessages);
            trimmed.addAll(all.subList(startIdx, all.size()));
        }
        return Collections.unmodifiableList(trimmed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesWithinTokenBudget(String conversationId, int maxTokens) {
        List<Message> all = getMessages(conversationId);
        if (all.isEmpty()) return Collections.emptyList();

        List<Message> result = new LinkedList<>();
        int totalTokens = 0;

        // Always keep system message
        int startIdx = 0;
        if (!all.isEmpty() && all.get(0).getRole() == Role.SYSTEM) {
            Message systemMsg = all.get(0);
            result.add(systemMsg);
            totalTokens += systemMsg.estimateTokens();
            startIdx = 1;
        }

        // Add messages from the end (most recent first) until budget exhausted
        List<Message> nonSystem = all.subList(startIdx, all.size());
        LinkedList<Message> recent = new LinkedList<>();
        for (int i = nonSystem.size() - 1; i >= 0; i--) {
            Message msg = nonSystem.get(i);
            int msgTokens = msg.estimateTokens();
            if (totalTokens + msgTokens > maxTokens) break;
            recent.addFirst(msg);
            totalTokens += msgTokens;
        }
        result.addAll(recent);

        log.debug("Token-aware memory: returning {}/{} messages (~{} tokens)", result.size(), all.size(), totalTokens);
        return Collections.unmodifiableList(result);
    }

    @Override
    @Transactional
    public void clearConversation(String conversationId) {
        log.info("Clearing conversation {}", conversationId);
        messageRepository.findByConversationId(conversationId)
                .forEach(messageRepository::delete);
        cache.remove(conversationId);
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        log.info("Deleting conversation {}", conversationId);
        conversationRepository.deleteById(conversationId);
        cache.remove(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean conversationExists(String conversationId) {
        return cache.containsKey(conversationId) || conversationRepository.existsById(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getMessageCount(String conversationId) {
        if (cache.containsKey(conversationId)) {
            return cache.get(conversationId).size();
        }
        return messageRepository.countByConversationId(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public int estimateTotalTokens(String conversationId) {
        return getMessages(conversationId).stream()
                .mapToInt(Message::estimateTokens)
                .sum();
    }

    private Message toMessage(MessageEntity entity) {
        Role role;
        try {
            role = Role.valueOf(entity.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            role = Role.USER;
        }
        return Message.builder()
                .role(role)
                .content(entity.getContent())
                .timestamp(entity.getCreatedAt())
                .build();
    }
}
