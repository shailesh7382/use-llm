package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    @JsonProperty("role")
    private Role role;

    @JsonProperty("content")
    private String content;

    @JsonProperty("name")
    private String name;

    private Instant timestamp = Instant.now();

    public Message() {
    }

    public Message(Role role, String content, String name, Instant timestamp) {
        this.role = role;
        this.content = content;
        this.name = name;
        this.timestamp = timestamp;
    }

    // Approximate token count estimation (4 chars ≈ 1 token)
    public int estimateTokens() {
        if (content == null) return 0;
        return Math.max(1, content.length() / 4);
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content, null, Instant.now());
    }

    public static Message user(String content) {
        return new Message(Role.USER, content, null, Instant.now());
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content, null, Instant.now());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Role role;
        private String content;
        private String name;
        private Instant timestamp = Instant.now();

        private Builder() {
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Message build() {
            return new Message(role, content, name, timestamp);
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(role, message.role)
                && Objects.equals(content, message.content)
                && Objects.equals(name, message.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, name);
    }

    @Override
    public String toString() {
        return "Message{role=" + role + ", content='" + content + "'}";
    }
}
