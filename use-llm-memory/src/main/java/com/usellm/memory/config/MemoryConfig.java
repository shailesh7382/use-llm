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

    /**
     * Message alignment strategy applied before sending messages to the LLM.
     * Values: STRICT | AUTO_FIX | WARN_ONLY
     * <ul>
     *   <li>STRICT    – throw an exception if alignment rules are violated</li>
     *   <li>AUTO_FIX  – silently repair violations (merge consecutive same-role, reorder system)</li>
     *   <li>WARN_ONLY – log warnings but send messages as-is</li>
     * </ul>
     */
    private String alignmentStrategy = "AUTO_FIX";

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
    public String getAlignmentStrategy() { return alignmentStrategy; }
    public void setAlignmentStrategy(String alignmentStrategy) { this.alignmentStrategy = alignmentStrategy; }
}
