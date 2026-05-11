package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutocompleteRequestDto {

    @NotBlank
    private String model;

    @NotBlank
    private String prompt;

    private Integer maxTokens = 128;
    private Double temperature = 0.3;
    private Boolean stream = false;
    private String suffix;
    private List<String> stop;

    public AutocompleteRequestDto() {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String model, prompt, suffix;
        private Integer maxTokens = 128;
        private Double temperature = 0.3;
        private Boolean stream = false;
        private List<String> stop;
        private Builder() {}
        public Builder model(String v) { model = v; return this; }
        public Builder prompt(String v) { prompt = v; return this; }
        public Builder maxTokens(Integer v) { maxTokens = v; return this; }
        public Builder temperature(Double v) { temperature = v; return this; }
        public Builder stream(Boolean v) { stream = v; return this; }
        public Builder suffix(String v) { suffix = v; return this; }
        public Builder stop(List<String> v) { stop = v; return this; }
        public AutocompleteRequestDto build() {
            AutocompleteRequestDto d = new AutocompleteRequestDto();
            d.model = model; d.prompt = prompt; d.maxTokens = maxTokens;
            d.temperature = temperature; d.stream = stream; d.suffix = suffix; d.stop = stop;
            return d;
        }
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public List<String> getStop() { return stop; }
    public void setStop(List<String> stop) { this.stop = stop; }
}
