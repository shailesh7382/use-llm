package com.usellm.memory.service;

import com.usellm.core.model.ConversationSummary;
import com.usellm.core.model.Message;
import com.usellm.core.model.Role;
import com.usellm.core.port.MemoryPort;
import com.usellm.memory.config.MemoryConfig;
import com.usellm.memory.entity.ConversationEntity;
import com.usellm.memory.entity.MessageEntity;
import com.usellm.memory.repository.ConversationRepository;
import com.usellm.memory.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService implements MemoryPort {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MemoryConfig memoryConfig;

    // In-memory cache for fast access
    private final ConcurrentHashMap<String, LinkedList<Message>> cache = new ConcurrentHashMap<>();

    public ConversationMemoryService(ConversationRepository conversationRepository,
                                     MessageRepository messageRepository,
                                     MemoryConfig memoryConfig) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memoryConfig = memoryConfig;
    }

    @Override
    @Transactional
    public void addMessage(String conversationId, Message message) {
        boolean isNew = !cache.containsKey(conversationId) && !conversationRepository.existsById(conversationId);

        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseGet(() -> {
                    log.info("Creating new conversation entity: conversationId={}", conversationId);
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

        cache.computeIfAbsent(conversationId, k -> new LinkedList<>()).addLast(message);

        log.info("Message added: conversationId={}, role={}, position={}, estimatedTokens={}, isNewConversation={}",
                conversationId, message.getRole(), nextPosition, tokenCount, isNew);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessages(String conversationId) {
        if (cache.containsKey(conversationId)) {
            List<Message> cached = Collections.unmodifiableList(new ArrayList<>(cache.get(conversationId)));
            log.info("Messages retrieved from cache: conversationId={}, count={}", conversationId, cached.size());
            return cached;
        }

        log.info("Cache miss — loading messages from DB: conversationId={}", conversationId);
        List<MessageEntity> entities = messageRepository.findByConversationId(conversationId);
        List<Message> messages = entities.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());

        cache.put(conversationId, new LinkedList<>(messages));
        log.info("Messages loaded from DB and cached: conversationId={}, count={}", conversationId, messages.size());
        return Collections.unmodifiableList(messages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(String conversationId, int maxMessages) {
        List<Message> all = getMessages(conversationId);
        if (all.size() <= maxMessages) {
            log.info("Sliding-window: conversationId={}, all {} message(s) fit within window={}",
                    conversationId, all.size(), maxMessages);
            return all;
        }
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
        log.info("Sliding-window trimmed: conversationId={}, from {} to {} message(s) (window={})",
                conversationId, all.size(), trimmed.size(), maxMessages);
        return Collections.unmodifiableList(trimmed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesWithinTokenBudget(String conversationId, int maxTokens) {
        List<Message> all = getMessages(conversationId);
        if (all.isEmpty()) {
            log.info("Token-aware memory: conversationId={}, no messages found", conversationId);
            return Collections.emptyList();
        }

        List<Message> result = new LinkedList<>();
        int totalTokens = 0;

        int startIdx = 0;
        if (!all.isEmpty() && all.get(0).getRole() == Role.SYSTEM) {
            Message systemMsg = all.get(0);
            result.add(systemMsg);
            totalTokens += systemMsg.estimateTokens();
            startIdx = 1;
        }

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

        log.info("Token-aware memory: conversationId={}, returning {}/{} message(s), ~{} tokens (budget={})",
                conversationId, result.size(), all.size(), totalTokens, maxTokens);
        return Collections.unmodifiableList(result);
    }

    @Override
    @Transactional
    public void clearConversation(String conversationId) {
        log.info("Clearing conversation messages: conversationId={}", conversationId);
        int count = messageRepository.findByConversationId(conversationId).size();
        messageRepository.findByConversationId(conversationId)
                .forEach(messageRepository::delete);
        cache.remove(conversationId);
        log.info("Conversation messages cleared: conversationId={}, removedMessages={}", conversationId, count);
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        log.info("Deleting conversation: conversationId={}", conversationId);
        conversationRepository.deleteById(conversationId);
        cache.remove(conversationId);
        log.info("Conversation deleted: conversationId={}", conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean conversationExists(String conversationId) {
        boolean inCache = cache.containsKey(conversationId);
        boolean inDb = !inCache && conversationRepository.existsById(conversationId);
        boolean exists = inCache || inDb;
        log.info("Conversation existence check: conversationId={}, exists={} (cacheHit={}, dbHit={})",
                conversationId, exists, inCache, inDb);
        return exists;
    }

    @Override
    @Transactional(readOnly = true)
    public int getMessageCount(String conversationId) {
        int count;
        if (cache.containsKey(conversationId)) {
            count = cache.get(conversationId).size();
            log.info("Message count from cache: conversationId={}, count={}", conversationId, count);
        } else {
            count = messageRepository.countByConversationId(conversationId);
            log.info("Message count from DB: conversationId={}, count={}", conversationId, count);
        }
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public int estimateTotalTokens(String conversationId) {
        int total = getMessages(conversationId).stream()
                .mapToInt(Message::estimateTokens)
                .sum();
        log.info("Estimated total tokens: conversationId={}, estimatedTokens={}", conversationId, total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummary> listConversations(int limit) {
        int safeLimit = limit > 0 ? limit : 50;
        log.info("Listing conversations: requestedLimit={}, effectiveLimit={}", limit, safeLimit);
        List<ConversationSummary> result = conversationRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .limit(safeLimit)
                .map(conv -> ConversationSummary.builder()
                        .conversationId(conv.getConversationId())
                        .createdAt(conv.getCreatedAt())
                        .updatedAt(conv.getUpdatedAt())
                        .messageCount(getMessageCount(conv.getConversationId()))
                        .build())
                .collect(Collectors.toList());
        log.info("Conversations listed: count={}", result.size());
        return result;
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
