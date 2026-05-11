package com.usellm.core.port;

import com.usellm.core.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port interface for LLM interactions.
 * Implementations connect to OpenAI-compatible endpoints.
 */
public interface LLMPort {

    Mono<ModelListResponse> listModels();

    Mono<LLMModel> getModel(String modelId);

    Mono<CompletionResponse> complete(CompletionRequest request);

    Flux<CompletionResponse> streamComplete(CompletionRequest request);

    Mono<ChatResponse> chat(ChatRequest request);

    Flux<ChatResponse> streamChat(ChatRequest request);
}
