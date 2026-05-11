package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionRequest {

    @NotBlank
    @JsonProperty("model")
    private String model;

    @NotNull
    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("max_tokens")
    private Integer maxTokens = 256;

    @JsonProperty("temperature")
    private Double temperature = 0.7;

    @JsonProperty("top_p")
    private Double topP = 1.0;

    @JsonProperty("stop")
    private List<String> stop;

    @JsonProperty("stream")
    private Boolean stream = false;

    @JsonProperty("suffix")
    private String suffix;

    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;

    public CompletionRequest() {
    }

    private CompletionRequest(Builder b) {
        this.model = b.model;
        this.prompt = b.prompt;
        this.maxTokens = b.maxTokens;
        this.temperature = b.temperature;
        this.topP = b.topP;
        this.stop = b.stop;
        this.stream = b.stream;
        this.suffix = b.suffix;
        this.logitBias = b.logitBias;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String model;
        private String prompt;
        private Integer maxTokens = 256;
        private Double temperature = 0.7;
        private Double topP = 1.0;
        private List<String> stop;
        private Boolean stream = false;
        private String suffix;
        private Map<String, Integer> logitBias;

        private Builder() {}

        public Builder model(String model) { this.model = model; return this; }
        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder topP(Double topP) { this.topP = topP; return this; }
        public Builder stop(List<String> stop) { this.stop = stop; return this; }
        public Builder stream(Boolean stream) { this.stream = stream; return this; }
        public Builder suffix(String suffix) { this.suffix = suffix; return this; }
        public Builder logitBias(Map<String, Integer> logitBias) { this.logitBias = logitBias; return this; }

        public CompletionRequest build() { return new CompletionRequest(this); }
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
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
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public Map<String, Integer> getLogitBias() { return logitBias; }
    public void setLogitBias(Map<String, Integer> logitBias) { this.logitBias = logitBias; }
}
