package com.usellm.memory.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_conversation_position", columnList = "conversation_id, position")
})
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MessageEntity() {}

    private MessageEntity(Builder b) {
        this.id = b.id;
        this.conversation = b.conversation;
        this.role = b.role;
        this.content = b.content;
        this.position = b.position;
        this.tokenCount = b.tokenCount;
        this.createdAt = b.createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private ConversationEntity conversation;
        private String role, content;
        private Integer position, tokenCount;
        private Instant createdAt;
        private Builder() {}
        public Builder id(Long id) { this.id = id; return this; }
        public Builder conversation(ConversationEntity conversation) { this.conversation = conversation; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder position(Integer position) { this.position = position; return this; }
        public Builder tokenCount(Integer tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public MessageEntity build() { return new MessageEntity(this); }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConversationEntity getConversation() { return conversation; }
    public void setConversation(ConversationEntity conversation) { this.conversation = conversation; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
