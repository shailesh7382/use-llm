package com.usellm.api.service;

import com.usellm.api.dto.ChatRequestDto;
import com.usellm.api.dto.ChatResponseDto;
import com.usellm.api.dto.PromptChatRequestDto;
import com.usellm.client.config.LLMClientConfig;
import com.usellm.core.model.*;
import com.usellm.core.port.LLMPort;
import com.usellm.core.port.MemoryPort;
import com.usellm.memory.config.MemoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LLMPort llmPort;
    private final MemoryPort memoryPort;
    private final MemoryConfig memoryConfig;
    private final LLMClientConfig llmClientConfig;
    private final MessageAlignmentService alignmentService;
    private final PromptBuilderService promptBuilderService;

    public ChatService(LLMPort llmPort,
                       MemoryPort memoryPort,
                       MemoryConfig memoryConfig,
                       LLMClientConfig llmClientConfig,
                       MessageAlignmentService alignmentService,
                       PromptBuilderService promptBuilderService) {
        this.llmPort = llmPort;
        this.memoryPort = memoryPort;
        this.memoryConfig = memoryConfig;
        this.llmClientConfig = llmClientConfig;
        this.alignmentService = alignmentService;
        this.promptBuilderService = promptBuilderService;
    }

    /** Chat completion with memory. */
    public Mono<ChatResponseDto> chat(ChatRequestDto requestDto) {
        String conversationId = resolveConversationId(requestDto.getConversationId());
        log.info("Chat request: conversationId={}, model={}", conversationId, requestDto.getModel());

        initializeConversationIfNew(conversationId, requestDto.getSystemPrompt());
        memoryPort.addMessage(conversationId, Message.user(requestDto.getMessage()));

        List<Message> contextMessages = alignmentService.align(buildContextMessages(conversationId));

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
                    String assistantContent = extractContent(response);
                    memoryPort.addMessage(conversationId, Message.assistant(assistantContent));
                    return buildResponseDto(conversationId, response, assistantContent);
                })
                .doOnError(e -> log.error("Chat error for conversation {}: {}", conversationId, e.getMessage()));
    }

    /** Streaming chat with memory (SSE). */
    public Flux<ChatResponseDto> streamChat(ChatRequestDto requestDto) {
        String conversationId = resolveConversationId(requestDto.getConversationId());
        log.info("Streaming chat: conversationId={}, model={}", conversationId, requestDto.getModel());

        initializeConversationIfNew(conversationId, requestDto.getSystemPrompt());
        memoryPort.addMessage(conversationId, Message.user(requestDto.getMessage()));

        List<Message> contextMessages = alignmentService.align(buildContextMessages(conversationId));

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
                    if (delta != null) fullResponse.append(delta);
                    return ChatResponseDto.builder()
                            .conversationId(conversationId)
                            .model(requestDto.getModel())
                            .content(delta)
                            .build();
                })
                .doOnComplete(() -> {
                    if (fullResponse.length() > 0) {
                        memoryPort.addMessage(conversationId, Message.assistant(fullResponse.toString()));
                    }
                })
                .doOnError(e -> log.error("Stream chat error for {}: {}", conversationId, e.getMessage()));
    }

    /**
     * Prompt-template–driven chat.
     *
     * <ol>
     *   <li>Renders the template into messages (system + few-shot + user).</li>
     *   <li>Stores system message and any few-shot pairs into the conversation memory.</li>
     *   <li>Sends the full context (aligned) to the LLM.</li>
     *   <li>Stores and returns the assistant reply.</li>
     * </ol>
     */
    public Mono<ChatResponseDto> promptChat(PromptChatRequestDto requestDto) {
        String conversationId = resolveConversationId(requestDto.getConversationId());
        String model = requestDto.getModel() != null && !requestDto.getModel().isBlank()
                ? requestDto.getModel()
                : llmClientConfig.getDefaultModel();

        log.info("PromptChat request: templateId={}, conversationId={}, model={}",
                requestDto.getTemplateId(), conversationId, model);

        // Render template → ordered messages
        List<Message> rendered = promptBuilderService.render(
                requestDto.getTemplateId(), requestDto.getVariables());

        // The last message must be the USER message (the actual query).
        // Everything before it is seeded into memory as conversation context.
        Message userMessage = null;
        int seedEndIdx = rendered.size();
        for (int i = rendered.size() - 1; i >= 0; i--) {
            if (rendered.get(i).getRole() == Role.USER) {
                userMessage = rendered.get(i);
                seedEndIdx = i;
                break;
            }
        }

        // Only initialise the conversation if it does not already exist
        if (!memoryPort.conversationExists(conversationId)) {
            for (int i = 0; i < seedEndIdx; i++) {
                memoryPort.addMessage(conversationId, rendered.get(i));
            }
        }

        if (userMessage == null) {
            // No user message in template → treat all rendered messages as context seed,
            // no LLM call possible. Return an explanatory error.
            throw new com.usellm.core.exception.LLMException(
                    "Template '" + requestDto.getTemplateId() +
                    "' did not produce a USER message. Set userPromptTemplate.", 400, "template_error");
        }

        memoryPort.addMessage(conversationId, userMessage);

        List<Message> contextMessages = alignmentService.align(buildContextMessages(conversationId));

        ChatRequest chatRequest = ChatRequest.builder()
                .model(model)
                .messages(contextMessages)
                .maxTokens(requestDto.getMaxTokens() != null ? requestDto.getMaxTokens() : 1024)
                .temperature(requestDto.getTemperature() != null ? requestDto.getTemperature() : 0.7)
                .stream(false)
                .build();

        final String finalConvId = conversationId;
        return llmPort.chat(chatRequest)
                .map(response -> {
                    String assistantContent = extractContent(response);
                    memoryPort.addMessage(finalConvId, Message.assistant(assistantContent));
                    return buildResponseDto(finalConvId, response, assistantContent);
                })
                .doOnError(e -> log.error("PromptChat error for conversation {}: {}", finalConvId, e.getMessage()));
    }

    /** Get conversation history. */
    public List<Message> getHistory(String conversationId) {
        return memoryPort.getMessages(conversationId);
    }

    /** Clear conversation memory (messages only). */
    public void clearConversation(String conversationId) {
        log.info("Clearing conversation {}", conversationId);
        memoryPort.clearConversation(conversationId);
    }

    /** Delete a conversation entirely. */
    public void deleteConversation(String conversationId) {
        log.info("Deleting conversation {}", conversationId);
        memoryPort.deleteConversation(conversationId);
    }

    /** List conversations ordered by recent activity. */
    public List<ConversationSummary> listConversations(int limit) {
        return memoryPort.listConversations(limit);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String resolveConversationId(String provided) {
        return (provided != null && !provided.isBlank()) ? provided : UUID.randomUUID().toString();
    }

    private void initializeConversationIfNew(String conversationId, String customSystemPrompt) {
        if (!memoryPort.conversationExists(conversationId)) {
            String systemPrompt = (customSystemPrompt != null && !customSystemPrompt.isBlank())
                    ? customSystemPrompt
                    : memoryConfig.getSystemPrompt();
            memoryPort.addMessage(conversationId, Message.system(systemPrompt));
        }
    }

    private List<Message> buildContextMessages(String conversationId) {
        return switch (memoryConfig.getStrategy().toUpperCase()) {
            case "SLIDING_WINDOW" -> memoryPort.getRecentMessages(conversationId, memoryConfig.getMaxMessages());
            case "TOKEN_AWARE"    -> memoryPort.getMessagesWithinTokenBudget(conversationId, memoryConfig.getMaxTokens());
            default               -> memoryPort.getMessages(conversationId);
        };
    }

    private String extractContent(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatResponse.ChatChoice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) return choice.getMessage().getContent();
        }
        return "";
    }

    private String extractDelta(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatResponse.ChatChoice choice = response.getChoices().get(0);
            if (choice.getDelta() != null) return choice.getDelta().getContent();
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
