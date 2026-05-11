package com.usellm.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm.client")
@Data
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
}
