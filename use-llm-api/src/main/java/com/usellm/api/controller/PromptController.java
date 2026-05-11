package com.usellm.api.controller;

import com.usellm.api.dto.*;
import com.usellm.api.service.ChatService;
import com.usellm.api.service.PromptBuilderService;
import com.usellm.core.model.Message;
import com.usellm.core.model.PromptTemplate;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for the Prompt Template and Message-Alignment subsystem.
 *
 * <h3>Endpoint summary</h3>
 * <pre>
 * GET    /api/v1/prompts/templates            – list all templates
 * GET    /api/v1/prompts/templates/{id}       – get a template by id
 * POST   /api/v1/prompts/templates            – create a custom template
 * PUT    /api/v1/prompts/templates/{id}       – update a custom template
 * DELETE /api/v1/prompts/templates/{id}       – delete a custom template
 *
 * POST   /api/v1/prompts/render               – render a template to a message list (preview)
 * POST   /api/v1/prompts/chat                 – render a template and start/continue a chat session
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    private static final Logger log = LoggerFactory.getLogger(PromptController.class);

    private final PromptBuilderService promptBuilderService;
    private final ChatService chatService;

    public PromptController(PromptBuilderService promptBuilderService, ChatService chatService) {
        this.promptBuilderService = promptBuilderService;
        this.chatService = chatService;
    }

    // -----------------------------------------------------------------------
    // Template CRUD
    // -----------------------------------------------------------------------

    /**
     * Returns all registered templates (built-in + custom).
     *
     * @param builtIn optional filter: {@code true} = built-in only, {@code false} = custom only
     */
    @GetMapping("/templates")
    public ResponseEntity<List<PromptTemplateDto>> listTemplates(
            @RequestParam(required = false) Boolean builtIn) {

        List<PromptTemplate> all = promptBuilderService.listAll();
        if (builtIn != null) {
            all = all.stream()
                    .filter(t -> t.isBuiltIn() == builtIn)
                    .collect(Collectors.toList());
        }
        List<PromptTemplateDto> dtos = all.stream()
                .map(PromptTemplateDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Returns a single template by its id.
     * Responds with 404 if the template does not exist (handled by
     * {@link com.usellm.api.exception.GlobalExceptionHandler}).
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<PromptTemplateDto> getTemplate(@PathVariable String id) {
        PromptTemplate template = promptBuilderService.findById(id)
                .orElseThrow(() -> new com.usellm.core.exception.LLMException(
                        "Prompt template not found: " + id, 404, "not_found"));
        return ResponseEntity.ok(PromptTemplateDto.from(template));
    }

    /**
     * Creates a new custom template.
     *
     * <p>The supplied {@code id} is optional; one is auto-generated when absent.
     * Built-in template ids cannot be reused.
     */
    @PostMapping("/templates")
    public ResponseEntity<PromptTemplateDto> createTemplate(
            @RequestBody @Valid PromptTemplateDto dto) {

        PromptTemplate saved = promptBuilderService.register(dto.toDomain());
        log.info("Created prompt template: id={}", saved.getId());
        return ResponseEntity.status(201).body(PromptTemplateDto.from(saved));
    }

    /**
     * Updates a custom template.  Built-in templates cannot be modified.
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<PromptTemplateDto> updateTemplate(
            @PathVariable String id,
            @RequestBody @Valid PromptTemplateDto dto) {

        PromptTemplate updated = promptBuilderService.update(id, dto.toDomain());
        log.info("Updated prompt template: id={}", id);
        return ResponseEntity.ok(PromptTemplateDto.from(updated));
    }

    /**
     * Deletes a custom template.  Built-in templates cannot be deleted.
     * Returns 204 on success, 404 when not found.
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        boolean deleted = promptBuilderService.delete(id);
        if (!deleted) {
            throw new com.usellm.core.exception.LLMException(
                    "Prompt template not found: " + id, 404, "not_found");
        }
        log.info("Deleted prompt template: id={}", id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------------

    /**
     * Renders a registered template into an ordered list of
     * {@link com.usellm.core.model.Message} objects without starting a conversation.
     *
     * <p>Useful for previewing prompt output, validating variable values, and estimating
     * token usage before committing to an LLM request.
     *
     * <pre>
     * POST /api/v1/prompts/render
     * {
     *   "templateId": "code-assistant",
     *   "variables": { "task": "Write a binary search in Java" }
     * }
     * </pre>
     */
    @PostMapping("/render")
    public ResponseEntity<PromptRenderResponseDto> render(
            @RequestBody @Valid PromptRenderRequestDto request) {

        PromptTemplate template = promptBuilderService.findById(request.getTemplateId())
                .orElseThrow(() -> new com.usellm.core.exception.LLMException(
                        "Prompt template not found: " + request.getTemplateId(), 404, "not_found"));

        Map<String, String> resolved = promptBuilderService.resolveVariables(
                template, request.getVariables());
        List<Message> messages = promptBuilderService.buildMessages(template, request.getVariables());

        PromptRenderResponseDto response = PromptRenderResponseDto.of(
                template.getId(), template.getName(), messages, resolved);

        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // Prompt-driven chat
    // -----------------------------------------------------------------------

    /**
     * Renders a template, seeds the conversation with the resulting system prompt and
     * few-shot examples, then sends the rendered user message to the LLM and returns
     * the assistant reply.
     *
     * <p>Conversation state is stored in memory and can be continued by passing the
     * returned {@code conversationId} in subsequent requests to
     * {@code POST /api/v1/chat/completions}.
     *
     * <pre>
     * POST /api/v1/prompts/chat
     * {
     *   "templateId": "code-assistant",
     *   "variables": { "task": "Explain Java virtual threads" },
     *   "model": "llama3",
     *   "maxTokens": 512
     * }
     * </pre>
     */
    @PostMapping("/chat")
    public Mono<ResponseEntity<ChatResponseDto>> promptChat(
            @RequestBody @Valid PromptChatRequestDto request) {

        return chatService.promptChat(request).map(ResponseEntity::ok);
    }
}

