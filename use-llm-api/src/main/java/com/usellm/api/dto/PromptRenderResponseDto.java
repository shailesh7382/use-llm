package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.usellm.core.model.Message;

import java.util.List;
import java.util.Map;

/**
 * Response body for {@code POST /api/v1/prompts/render}.
 *
 * <p>Contains the fully rendered message sequence together with diagnostic metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptRenderResponseDto {

    /** Id of the template that was rendered. */
    private String templateId;

    /** Display name of the template. */
    private String templateName;

    /** Ordered list of messages (SYSTEM → USER/ASSISTANT examples → USER prompt). */
    private List<Message> messages;

    /** The variable values that were actually applied during rendering. */
    private Map<String, String> resolvedVariables;

    /** Approximate total token count across all rendered messages. */
    private int estimatedTokens;

    public PromptRenderResponseDto() {}

    public static PromptRenderResponseDto of(
            String templateId,
            String templateName,
            List<Message> messages,
            Map<String, String> resolvedVariables) {

        PromptRenderResponseDto dto = new PromptRenderResponseDto();
        dto.templateId = templateId;
        dto.templateName = templateName;
        dto.messages = messages;
        dto.resolvedVariables = resolvedVariables;
        dto.estimatedTokens = messages.stream()
                .mapToInt(Message::estimateTokens)
                .sum();
        return dto;
    }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Map<String, String> getResolvedVariables() { return resolvedVariables; }
    public void setResolvedVariables(Map<String, String> resolvedVariables) { this.resolvedVariables = resolvedVariables; }
    public int getEstimatedTokens() { return estimatedTokens; }
    public void setEstimatedTokens(int estimatedTokens) { this.estimatedTokens = estimatedTokens; }
}

