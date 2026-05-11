package com.usellm.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm.client")
public class LLMClientConfig {

    /**
     * Base URL of the OpenAI-compatible LLM server (e.g., http://localhost:11434/v1 for Ollama)
     */
    private String baseUrl = "http://localhost:11434/v1";

    /**
     * API key (use "ollama" for Ollama, or your actual key for OpenAI)
     */
    private String apiKey = "ollama";

    /**
     * Default model to use
     */
    private String defaultModel = "llama3";

    /**
     * Request timeout in seconds
     */
    private int timeoutSeconds = 120;

    /**
     * Maximum retry attempts
     */
    private int maxRetries = 3;

    /**
     * Retry delay in milliseconds
     */
    private long retryDelayMs = 1000;

    /**
     * Connection pool max connections
     */
    private int maxConnections = 20;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
}
