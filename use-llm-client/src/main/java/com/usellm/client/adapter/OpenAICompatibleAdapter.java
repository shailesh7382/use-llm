package com.usellm.client.adapter;

import com.usellm.core.exception.LLMException;
import com.usellm.core.model.*;
import com.usellm.core.port.LLMPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleAdapter implements LLMPort {

    @Qualifier("llmWebClient")
    private final WebClient webClient;

    @Override
    public Mono<ModelListResponse> listModels() {
        log.debug("Listing models from LLM server");
        return webClient.get()
                .uri("/models")
                .retrieve()
                .bodyToMono(ModelListResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .doOnError(e -> log.error("Error listing models: {}", e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Mono<LLMModel> getModel(String modelId) {
        log.debug("Getting model: {}", modelId);
        return webClient.get()
                .uri("/models/{model}", modelId)
                .retrieve()
                .bodyToMono(LLMModel.class)
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        log.debug("Sending completion request for model: {}", request.getModel());
        CompletionRequest nonStreaming = request;
        if (Boolean.TRUE.equals(request.getStream())) {
            nonStreaming = copyWithStream(request, false);
        }
        return webClient.post()
                .uri("/completions")
                .bodyValue(nonStreaming)
                .retrieve()
                .bodyToMono(CompletionResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .filter(e -> !(e instanceof WebClientResponseException.BadRequest)))
                .doOnError(e -> log.error("Completion error: {}", e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Flux<CompletionResponse> streamComplete(CompletionRequest request) {
        log.debug("Streaming completion for model: {}", request.getModel());
        CompletionRequest streaming = copyWithStream(request, true);
        return webClient.post()
                .uri("/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(streaming)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                .map(line -> line.substring(6))
                .flatMap(this::parseCompletionChunk)
                .doOnError(e -> log.error("Stream completion error: {}", e.getMessage()));
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        log.debug("Sending chat request for model: {}", request.getModel());
        ChatRequest nonStreaming = request;
        if (Boolean.TRUE.equals(request.getStream())) {
            nonStreaming = copyWithStream(request, false);
        }
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(nonStreaming)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .filter(e -> !(e instanceof WebClientResponseException.BadRequest)))
                .doOnError(e -> log.error("Chat error: {}", e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Flux<ChatResponse> streamChat(ChatRequest request) {
        log.debug("Streaming chat for model: {}", request.getModel());
        ChatRequest streaming = copyWithStream(request, true);
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(streaming)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                .map(line -> line.substring(6))
                .flatMap(this::parseChatChunk)
                .doOnError(e -> log.error("Stream chat error: {}", e.getMessage()));
    }

    private LLMException mapError(WebClientResponseException e) {
        log.error("LLM API error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
        return new LLMException(
                "LLM API error: " + e.getMessage(),
                e.getStatusCode().value(),
                "api_error"
        );
    }

    private CompletionRequest copyWithStream(CompletionRequest req, boolean stream) {
        return CompletionRequest.builder()
                .model(req.getModel())
                .prompt(req.getPrompt())
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .topP(req.getTopP())
                .stop(req.getStop())
                .stream(stream)
                .suffix(req.getSuffix())
                .build();
    }

    private ChatRequest copyWithStream(ChatRequest req, boolean stream) {
        return ChatRequest.builder()
                .model(req.getModel())
                .messages(req.getMessages())
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .topP(req.getTopP())
                .stop(req.getStop())
                .stream(stream)
                .frequencyPenalty(req.getFrequencyPenalty())
                .presencePenalty(req.getPresencePenalty())
                .build();
    }

    private Mono<CompletionResponse> parseCompletionChunk(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return Mono.just(mapper.readValue(json, CompletionResponse.class));
        } catch (Exception e) {
            log.warn("Failed to parse completion chunk: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<ChatResponse> parseChatChunk(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return Mono.just(mapper.readValue(json, ChatResponse.class));
        } catch (Exception e) {
            log.warn("Failed to parse chat chunk: {}", e.getMessage());
            return Mono.empty();
        }
    }
}
