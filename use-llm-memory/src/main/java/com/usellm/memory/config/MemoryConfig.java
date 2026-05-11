package com.usellm.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm.memory")
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
     * Strategy: SLIDING_WINDOW, TOKEN_AWARE, or ALL
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

    public int getMaxMessages() { return maxMessages; }
    public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
}
