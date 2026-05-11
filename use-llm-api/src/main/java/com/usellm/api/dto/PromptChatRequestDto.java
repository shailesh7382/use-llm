package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request body for {@code POST /api/v1/prompts/chat}.
 *
 * <p>A convenience endpoint that:
 * <ol>
 *   <li>Renders the named template into a message list.</li>
 *   <li>Initialises (or continues) a conversation with the rendered system prompt and
 *       few-shot examples stored in memory.</li>
 *   <li>Sends the rendered user message to the LLM.</li>
 *   <li>Returns a {@link ChatResponseDto} identical to a regular chat completion.</li>
 * </ol>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptChatRequestDto {

    /** Id of the template to render and use as a prompt. */
    @NotBlank(message = "templateId is required")
    private String templateId;

    /**
     * Variable values for the template.
     * Example: {@code {"task": "Write a merge sort in Python"}}
     */
    private Map<String, String> variables;

    /**
     * Optional conversation id.  Supply an existing id to continue a conversation;
     * omit to start a new one (auto-generated).
     */
    private String conversationId;

    /**
     * Model to use.  Falls back to {@code llm.client.default-model} when not supplied.
     */
    private String model;

    /** Max tokens for the LLM response.  Defaults to 1024. */
    private Integer maxTokens = 1024;

    /** Sampling temperature.  Defaults to 0.7. */
    private Double temperature = 0.7;

    public PromptChatRequestDto() {}

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
}

