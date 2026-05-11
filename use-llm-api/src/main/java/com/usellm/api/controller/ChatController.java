package com.usellm.api.controller;

import com.usellm.api.dto.ChatRequestDto;
import com.usellm.api.dto.ChatResponseDto;
import com.usellm.api.service.ChatService;
import com.usellm.core.model.Message;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Chat completion with memory.
     * POST /api/v1/chat/completions
     */
    @PostMapping("/completions")
    public Mono<ResponseEntity<ChatResponseDto>> chat(
            @RequestBody @Valid ChatRequestDto request) {
        return chatService.chat(request)
                .map(ResponseEntity::ok);
    }

    /**
     * Streaming chat with memory (Server-Sent Events).
     * POST /api/v1/chat/completions/stream
     */
    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponseDto> streamChat(
            @RequestBody @Valid ChatRequestDto request) {
        return chatService.streamChat(request);
    }

    /**
     * Get conversation history.
     * GET /api/v1/chat/conversations/{conversationId}/history
     */
    @GetMapping("/conversations/{conversationId}/history")
    public ResponseEntity<List<Message>> getHistory(@PathVariable String conversationId) {
        return ResponseEntity.ok(chatService.getHistory(conversationId));
    }

    /**
     * Clear conversation memory (keeps conversation, removes messages).
     * DELETE /api/v1/chat/conversations/{conversationId}/messages
     */
    @DeleteMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, String>> clearConversation(@PathVariable String conversationId) {
        chatService.clearConversation(conversationId);
        return ResponseEntity.ok(Map.of("status", "cleared", "conversationId", conversationId));
    }

    /**
     * Delete a conversation entirely.
     * DELETE /api/v1/chat/conversations/{conversationId}
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable String conversationId) {
        chatService.deleteConversation(conversationId);
        return ResponseEntity.ok(Map.of("status", "deleted", "conversationId", conversationId));
    }
}
