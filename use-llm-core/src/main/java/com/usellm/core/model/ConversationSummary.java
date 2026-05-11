package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationSummary {

    private String conversationId;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer messageCount;

    public ConversationSummary() {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String conversationId;
        private Instant createdAt;
        private Instant updatedAt;
        private Integer messageCount;

        private Builder() {}

        public Builder conversationId(String v) { conversationId = v; return this; }
        public Builder createdAt(Instant v) { createdAt = v; return this; }
        public Builder updatedAt(Instant v) { updatedAt = v; return this; }
        public Builder messageCount(Integer v) { messageCount = v; return this; }

        public ConversationSummary build() {
            ConversationSummary d = new ConversationSummary();
            d.conversationId = conversationId;
            d.createdAt = createdAt;
            d.updatedAt = updatedAt;
            d.messageCount = messageCount;
            return d;
        }
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
}
