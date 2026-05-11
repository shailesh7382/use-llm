package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.usellm.core.model.Message;
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
public class ChatResponseDto {

    private String conversationId;
    private String responseId;
    private String model;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String finishReason;
    private Integer memorySize;
    private Integer estimatedMemoryTokens;
    private List<Message> history;
}
