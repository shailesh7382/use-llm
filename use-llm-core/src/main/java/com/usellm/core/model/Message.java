package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    @JsonProperty("role")
    private Role role;

    @JsonProperty("content")
    private String content;

    @JsonProperty("name")
    private String name;

    @Builder.Default
    private Instant timestamp = Instant.now();

    // Approximate token count estimation (4 chars ≈ 1 token)
    public int estimateTokens() {
        if (content == null) return 0;
        return Math.max(1, content.length() / 4);
    }

    public static Message system(String content) {
        return Message.builder().role(Role.SYSTEM).content(content).build();
    }

    public static Message user(String content) {
        return Message.builder().role(Role.USER).content(content).build();
    }

    public static Message assistant(String content) {
        return Message.builder().role(Role.ASSISTANT).content(content).build();
    }
}
