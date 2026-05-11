package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    @NotBlank
    @JsonProperty("model")
    private String model;

    @NotEmpty
    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens = 1024;

    @JsonProperty("temperature")
    private Double temperature = 0.7;

    @JsonProperty("top_p")
    private Double topP = 1.0;

    @JsonProperty("stop")
    private List<String> stop;

    @JsonProperty("stream")
    private Boolean stream = false;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;

    public ChatRequest() {}

    private ChatRequest(Builder b) {
        this.model = b.model;
        this.messages = b.messages;
        this.maxTokens = b.maxTokens;
        this.temperature = b.temperature;
        this.topP = b.topP;
        this.stop = b.stop;
        this.stream = b.stream;
        this.frequencyPenalty = b.frequencyPenalty;
        this.presencePenalty = b.presencePenalty;
        this.logitBias = b.logitBias;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String model;
        private List<Message> messages;
        private Integer maxTokens = 1024;
        private Double temperature = 0.7;
        private Double topP = 1.0;
        private List<String> stop;
        private Boolean stream = false;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Map<String, Integer> logitBias;

        private Builder() {}
        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<Message> messages) { this.messages = messages; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder topP(Double topP) { this.topP = topP; return this; }
        public Builder stop(List<String> stop) { this.stop = stop; return this; }
        public Builder stream(Boolean stream) { this.stream = stream; return this; }
        public Builder frequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; return this; }
        public Builder presencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; return this; }
        public Builder logitBias(Map<String, Integer> logitBias) { this.logitBias = logitBias; return this; }
        public ChatRequest build() { return new ChatRequest(this); }
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public List<String> getStop() { return stop; }
    public void setStop(List<String> stop) { this.stop = stop; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public void setFrequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
    public Double getPresencePenalty() { return presencePenalty; }
    public void setPresencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; }
    public Map<String, Integer> getLogitBias() { return logitBias; }
    public void setLogitBias(Map<String, Integer> logitBias) { this.logitBias = logitBias; }
}
