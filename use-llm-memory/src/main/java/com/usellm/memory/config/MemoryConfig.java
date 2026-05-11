package com.usellm.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm.memory")
@Data
public class MemoryConfig {

    /**
     * Maximum number of messages to keep in sliding window.
     */
    private int maxMessages = 20;

    /**
     * Maximum token budget for context window.
     */
    private int maxTokens = 4096;

    /**
     * Strategy: SLIDING_WINDOW, TOKEN_AWARE, or SUMMARY
     */
    private String strategy = "TOKEN_AWARE";

    /**
     * System prompt to prepend to every conversation.
     */
    private String systemPrompt = "You are a helpful AI assistant.";

    /**
     * Whether to persist memory to DB (true) or keep in-memory only (false).
     */
    private boolean persistent = true;
}
