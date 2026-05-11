package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request body for {@code POST /api/v1/prompts/render}.
 *
 * <p>Renders a registered {@link com.usellm.core.model.PromptTemplate} into an ordered list of
 * {@link com.usellm.core.model.Message} objects without starting a conversation.
 * Useful for previewing what a template produces before sending it to an LLM.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptRenderRequestDto {

    /** Id of the template to render (required). */
    @NotBlank(message = "templateId is required")
    private String templateId;

    /**
     * Variable values keyed by placeholder name.
     * Example: {@code {"task": "Write a binary search in Java", "language": "Java"}}
     */
    private Map<String, String> variables;

    public PromptRenderRequestDto() {}

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
}

