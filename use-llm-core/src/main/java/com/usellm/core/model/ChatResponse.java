package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private Long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<ChatChoice> choices;

    @JsonProperty("usage")
    private CompletionResponse.Usage usage;

    public ChatResponse() {}

    private ChatResponse(Builder b) {
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
        private List<ChatChoice> choices;
        private CompletionResponse.Usage usage;

        private Builder() {}
        public Builder id(String id) { this.id = id; return this; }
        public Builder object(String object) { this.object = object; return this; }
        public Builder created(Long created) { this.created = created; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder choices(List<ChatChoice> choices) { this.choices = choices; return this; }
        public Builder usage(CompletionResponse.Usage usage) { this.usage = usage; return this; }
        public ChatResponse build() { return new ChatResponse(this); }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChatChoice> getChoices() { return choices; }
    public void setChoices(List<ChatChoice> choices) { this.choices = choices; }
    public CompletionResponse.Usage getUsage() { return usage; }
    public void setUsage(CompletionResponse.Usage usage) { this.usage = usage; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatChoice {
        @JsonProperty("index")
        private Integer index;

        @JsonProperty("message")
        private Message message;

        @JsonProperty("delta")
        private Message delta;

        @JsonProperty("finish_reason")
        private String finishReason;

        public ChatChoice() {}

        private ChatChoice(Builder b) {
            this.index = b.index;
            this.message = b.message;
            this.delta = b.delta;
            this.finishReason = b.finishReason;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private Integer index;
            private Message message, delta;
            private String finishReason;
            private Builder() {}
            public Builder index(Integer index) { this.index = index; return this; }
            public Builder message(Message message) { this.message = message; return this; }
            public Builder delta(Message delta) { this.delta = delta; return this; }
            public Builder finishReason(String finishReason) { this.finishReason = finishReason; return this; }
            public ChatChoice build() { return new ChatChoice(this); }
        }

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public Message getDelta() { return delta; }
        public void setDelta(Message delta) { this.delta = delta; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }
}
