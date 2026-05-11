package com.usellm.api.service;

import com.usellm.api.dto.ChatRequestDto;
import com.usellm.api.dto.ChatResponseDto;
import com.usellm.client.config.LLMClientConfig;
import com.usellm.core.model.*;
import com.usellm.core.port.LLMPort;
import com.usellm.core.port.MemoryPort;
import com.usellm.memory.config.MemoryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final LLMPort llmPort;
    private final MemoryPort memoryPort;
    private final MemoryConfig memoryConfig;
    private final LLMClientConfig clientConfig;

    /**
     * Perform a chat interaction with memory management.
     */
    public Mono<ChatResponseDto> chat(ChatRequestDto requestDto) {
        String conversationId = resolveConversationId(requestDto.getConversationId());
        log.info("Chat request: conversationId={}, model={}", conversationId, requestDto.getModel());

        // Initialize conversation with system prompt if new
        initializeConversationIfNew(conversationId, requestDto.getSystemPrompt());

        // Add user message to memory
        Message userMessage = Message.user(requestDto.getMessage());
        memoryPort.addMessage(conversationId, userMessage);

        // Build context-aware message list from memory
        List<Message> contextMessages = buildContextMessages(conversationId);

        ChatRequest chatRequest = ChatRequest.builder()
                .model(requestDto.getModel())
                .messages(contextMessages)
                .maxTokens(requestDto.getMaxTokens())
                .temperature(requestDto.getTemperature())
                .stop(requestDto.getStop())
                .stream(false)
                .build();

        return llmPort.chat(chatRequest)
                .map(response -> {
                    // Extract assistant reply and persist to memory
                    String assistantContent = extractContent(response);
                    Message assistantMessage = Message.assistant(assistantContent);
                    memoryPort.addMessage(conversationId, assistantMessage);

                    return buildResponseDto(conversationId, response, assistantContent);
                })
                .doOnError(e -> log.error("Chat error for conversation {}: {}", conversationId, e.getMessage()));
    }

    /**
     * Streaming chat with memory management.
     */
    public Flux<ChatResponseDto> streamChat(ChatRequestDto requestDto) {
        String conversationId = resolveConversationId(requestDto.getConversationId());
        log.info("Streaming chat: conversationId={}, model={}", conversationId, requestDto.getModel());

        initializeConversationIfNew(conversationId, requestDto.getSystemPrompt());

        Message userMessage = Message.user(requestDto.getMessage());
        memoryPort.addMessage(conversationId, userMessage);

        List<Message> contextMessages = buildContextMessages(conversationId);

        ChatRequest chatRequest = ChatRequest.builder()
                .model(requestDto.getModel())
                .messages(contextMessages)
                .maxTokens(requestDto.getMaxTokens())
                .temperature(requestDto.getTemperature())
                .stop(requestDto.getStop())
                .stream(true)
                .build();

        StringBuilder fullResponse = new StringBuilder();

        return llmPort.streamChat(chatRequest)
                .map(chunk -> {
                    String delta = extractDelta(chunk);
                    if (delta != null) {
                        fullResponse.append(delta);
                    }
                    return ChatResponseDto.builder()
                            .conversationId(conversationId)
                            .model(requestDto.getModel())
                            .content(delta)
                            .build();
                })
                .doOnComplete(() -> {
                    // Persist full response to memory after stream completes
                    if (fullResponse.length() > 0) {
                        memoryPort.addMessage(conversationId, Message.assistant(fullResponse.toString()));
                        log.debug("Persisted streamed response to memory for {}", conversationId);
                    }
                })
                .doOnError(e -> log.error("Stream chat error for {}: {}", conversationId, e.getMessage()));
    }

    /**
     * Get conversation history.
     */
    public List<Message> getHistory(String conversationId) {
        return memoryPort.getMessages(conversationId);
    }

    /**
     * Clear conversation memory.
     */
    public void clearConversation(String conversationId) {
        log.info("Clearing conversation {}", conversationId);
        memoryPort.clearConversation(conversationId);
    }

    /**
     * Delete a conversation entirely.
     */
    public void deleteConversation(String conversationId) {
        log.info("Deleting conversation {}", conversationId);
        memoryPort.deleteConversation(conversationId);
    }

    private String resolveConversationId(String provided) {
        return (provided != null && !provided.isBlank()) ? provided : UUID.randomUUID().toString();
    }

    private void initializeConversationIfNew(String conversationId, String customSystemPrompt) {
        if (!memoryPort.conversationExists(conversationId)) {
            String systemPrompt = (customSystemPrompt != null && !customSystemPrompt.isBlank())
                    ? customSystemPrompt
                    : memoryConfig.getSystemPrompt();
            memoryPort.addMessage(conversationId, Message.system(systemPrompt));
            log.debug("Initialized new conversation {} with system prompt", conversationId);
        }
    }

    private List<Message> buildContextMessages(String conversationId) {
        String strategy = memoryConfig.getStrategy();
        return switch (strategy.toUpperCase()) {
            case "SLIDING_WINDOW" -> memoryPort.getRecentMessages(conversationId, memoryConfig.getMaxMessages());
            case "TOKEN_AWARE" -> memoryPort.getMessagesWithinTokenBudget(conversationId, memoryConfig.getMaxTokens());
            default -> memoryPort.getMessages(conversationId);
        };
    }

    private String extractContent(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatResponse.ChatChoice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }

    private String extractDelta(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatResponse.ChatChoice choice = response.getChoices().get(0);
            if (choice.getDelta() != null) {
                return choice.getDelta().getContent();
            }
        }
        return null;
    }

    private ChatResponseDto buildResponseDto(String conversationId, ChatResponse response, String content) {
        CompletionResponse.Usage usage = response.getUsage();
        return ChatResponseDto.builder()
                .conversationId(conversationId)
                .responseId(response.getId())
                .model(response.getModel())
                .content(content)
                .promptTokens(usage != null ? usage.getPromptTokens() : null)
                .completionTokens(usage != null ? usage.getCompletionTokens() : null)
                .totalTokens(usage != null ? usage.getTotalTokens() : null)
                .finishReason(response.getChoices() != null && !response.getChoices().isEmpty()
                        ? response.getChoices().get(0).getFinishReason() : null)
                .memorySize(memoryPort.getMessageCount(conversationId))
                .estimatedMemoryTokens(memoryPort.estimateTotalTokens(conversationId))
                .build();
    }
}
