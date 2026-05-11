package com.usellm.memory.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<MessageEntity> messages = new ArrayList<>();

    public ConversationEntity() {}

    private ConversationEntity(Builder b) {
        this.conversationId = b.conversationId;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.metadata = b.metadata;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String conversationId, metadata;
        private Instant createdAt, updatedAt;
        private Builder() {}
        public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public ConversationEntity build() { return new ConversationEntity(this); }
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public List<MessageEntity> getMessages() { return messages; }
    public void setMessages(List<MessageEntity> messages) { this.messages = messages; }
}
