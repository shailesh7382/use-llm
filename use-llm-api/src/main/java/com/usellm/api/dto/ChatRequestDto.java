package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequestDto {

    /**
     * Session/conversation ID for memory continuity. Auto-generated if not provided.
     */
    private String conversationId;

    @NotBlank
    private String model;

    @NotBlank
    private String message;

    @Builder.Default
    private Double temperature = 0.7;

    @Builder.Default
    private Integer maxTokens = 1024;

    @Builder.Default
    private Boolean stream = false;

    private String systemPrompt;

    private List<String> stop;
}
