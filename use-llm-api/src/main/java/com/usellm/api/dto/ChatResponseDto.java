package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.usellm.core.model.Message;

import java.util.List;

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

    public ChatResponseDto() {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String conversationId, responseId, model, content, finishReason;
        private Integer promptTokens, completionTokens, totalTokens, memorySize, estimatedMemoryTokens;
        private List<Message> history;
        private Builder() {}
        public Builder conversationId(String v) { conversationId = v; return this; }
        public Builder responseId(String v) { responseId = v; return this; }
        public Builder model(String v) { model = v; return this; }
        public Builder content(String v) { content = v; return this; }
        public Builder finishReason(String v) { finishReason = v; return this; }
        public Builder promptTokens(Integer v) { promptTokens = v; return this; }
        public Builder completionTokens(Integer v) { completionTokens = v; return this; }
        public Builder totalTokens(Integer v) { totalTokens = v; return this; }
        public Builder memorySize(Integer v) { memorySize = v; return this; }
        public Builder estimatedMemoryTokens(Integer v) { estimatedMemoryTokens = v; return this; }
        public Builder history(List<Message> v) { history = v; return this; }
        public ChatResponseDto build() {
            ChatResponseDto d = new ChatResponseDto();
            d.conversationId = conversationId; d.responseId = responseId; d.model = model;
            d.content = content; d.finishReason = finishReason; d.promptTokens = promptTokens;
            d.completionTokens = completionTokens; d.totalTokens = totalTokens;
            d.memorySize = memorySize; d.estimatedMemoryTokens = estimatedMemoryTokens;
            d.history = history;
            return d;
        }
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String v) { this.conversationId = v; }
    public String getResponseId() { return responseId; }
    public void setResponseId(String v) { this.responseId = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer v) { this.promptTokens = v; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer v) { this.completionTokens = v; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer v) { this.totalTokens = v; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String v) { this.finishReason = v; }
    public Integer getMemorySize() { return memorySize; }
    public void setMemorySize(Integer v) { this.memorySize = v; }
    public Integer getEstimatedMemoryTokens() { return estimatedMemoryTokens; }
    public void setEstimatedMemoryTokens(Integer v) { this.estimatedMemoryTokens = v; }
    public List<Message> getHistory() { return history; }
    public void setHistory(List<Message> v) { this.history = v; }
}
