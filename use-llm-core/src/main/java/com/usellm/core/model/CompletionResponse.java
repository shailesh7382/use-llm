package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private Long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<CompletionChoice> choices;

    @JsonProperty("usage")
    private Usage usage;

    public CompletionResponse() {}

    private CompletionResponse(Builder b) {
        this.id = b.id;
        this.object = b.object;
        this.created = b.created;
        this.model = b.model;
        this.choices = b.choices;
        this.usage = b.usage;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id, object, model;
        private Long created;
        private List<CompletionChoice> choices;
        private Usage usage;

        private Builder() {}
        public Builder id(String id) { this.id = id; return this; }
        public Builder object(String object) { this.object = object; return this; }
        public Builder created(Long created) { this.created = created; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder choices(List<CompletionChoice> choices) { this.choices = choices; return this; }
        public Builder usage(Usage usage) { this.usage = usage; return this; }
        public CompletionResponse build() { return new CompletionResponse(this); }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<CompletionChoice> getChoices() { return choices; }
    public void setChoices(List<CompletionChoice> choices) { this.choices = choices; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompletionChoice {
        @JsonProperty("text")
        private String text;

        @JsonProperty("index")
        private Integer index;

        @JsonProperty("finish_reason")
        private String finishReason;

        public CompletionChoice() {}

        private CompletionChoice(Builder b) {
            this.text = b.text;
            this.index = b.index;
            this.finishReason = b.finishReason;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String text, finishReason;
            private Integer index;
            private Builder() {}
            public Builder text(String text) { this.text = text; return this; }
            public Builder index(Integer index) { this.index = index; return this; }
            public Builder finishReason(String finishReason) { this.finishReason = finishReason; return this; }
            public CompletionChoice build() { return new CompletionChoice(this); }
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Usage() {}

        private Usage(Builder b) {
            this.promptTokens = b.promptTokens;
            this.completionTokens = b.completionTokens;
            this.totalTokens = b.totalTokens;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private Integer promptTokens, completionTokens, totalTokens;
            private Builder() {}
            public Builder promptTokens(Integer promptTokens) { this.promptTokens = promptTokens; return this; }
            public Builder completionTokens(Integer completionTokens) { this.completionTokens = completionTokens; return this; }
            public Builder totalTokens(Integer totalTokens) { this.totalTokens = totalTokens; return this; }
            public Usage build() { return new Usage(this); }
        }

        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    }
}
