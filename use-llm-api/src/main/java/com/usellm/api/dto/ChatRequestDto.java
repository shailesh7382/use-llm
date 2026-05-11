package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequestDto {

    /** Session/conversation ID for memory continuity. Auto-generated if not provided. */
    private String conversationId;

    @NotBlank
    private String model;

    @NotBlank
    private String message;

    private Double temperature = 0.7;
    private Integer maxTokens = 1024;
    private Boolean stream = false;
    private String systemPrompt;
    private List<String> stop;

    public ChatRequestDto() {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String conversationId, model, message, systemPrompt;
        private Double temperature = 0.7;
        private Integer maxTokens = 1024;
        private Boolean stream = false;
        private List<String> stop;
        private Builder() {}
        public Builder conversationId(String v) { conversationId = v; return this; }
        public Builder model(String v) { model = v; return this; }
        public Builder message(String v) { message = v; return this; }
        public Builder temperature(Double v) { temperature = v; return this; }
        public Builder maxTokens(Integer v) { maxTokens = v; return this; }
        public Builder stream(Boolean v) { stream = v; return this; }
        public Builder systemPrompt(String v) { systemPrompt = v; return this; }
        public Builder stop(List<String> v) { stop = v; return this; }
        public ChatRequestDto build() {
            ChatRequestDto d = new ChatRequestDto();
            d.conversationId = conversationId; d.model = model; d.message = message;
            d.temperature = temperature; d.maxTokens = maxTokens; d.stream = stream;
            d.systemPrompt = systemPrompt; d.stop = stop;
            return d;
        }
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public List<String> getStop() { return stop; }
    public void setStop(List<String> stop) { this.stop = stop; }
}
