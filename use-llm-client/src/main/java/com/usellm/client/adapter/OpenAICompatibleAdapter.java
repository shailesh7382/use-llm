package com.usellm.client.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usellm.core.exception.LLMException;
import com.usellm.core.model.*;
import com.usellm.core.port.LLMPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class OpenAICompatibleAdapter implements LLMPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleAdapter.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAICompatibleAdapter(@Qualifier("llmWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<ModelListResponse> listModels() {
        log.info("Requesting model list from LLM server");
        return webClient.get()
                .uri("/models")
                .retrieve()
                .bodyToMono(ModelListResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .doOnSuccess(resp -> log.info("Model list received: count={}",
                        resp.getData() != null ? resp.getData().size() : 0))
                .doOnError(e -> log.error("Error listing models: {}", e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Mono<LLMModel> getModel(String modelId) {
        log.info("Requesting model details: modelId={}", modelId);
        return webClient.get()
                .uri("/models/{model}", modelId)
                .retrieve()
                .bodyToMono(LLMModel.class)
                .doOnSuccess(m -> log.info("Model details received: id={}, ownedBy={}", m.getId(), m.getOwnedBy()))
                .doOnError(e -> log.error("Error fetching model {}: {}", modelId, e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request) {
        log.info("Sending completion request: model={}, maxTokens={}, temperature={}",
                request.getModel(), request.getMaxTokens(), request.getTemperature());
        CompletionRequest nonStreaming = Boolean.TRUE.equals(request.getStream())
                ? copyWithStream(request, false) : request;
        return webClient.post()
                .uri("/completions")
                .bodyValue(nonStreaming)
                .retrieve()
                .bodyToMono(CompletionResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .filter(e -> !(e instanceof WebClientResponseException.BadRequest))
                        .doBeforeRetry(signal -> log.info("Retrying completion request (attempt {}): model={}",
                                signal.totalRetries() + 1, request.getModel())))
                .doOnSuccess(resp -> log.info("Completion response received: model={}, usage={}",
                        resp.getModel() != null ? resp.getModel() : request.getModel(), resp.getUsage()))
                .doOnError(e -> log.error("Completion error: {}", e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Flux<CompletionResponse> streamComplete(CompletionRequest request) {
        log.info("Sending streaming completion request: model={}, maxTokens={}", request.getModel(), request.getMaxTokens());
        CompletionRequest streaming = copyWithStream(request, true);
        return webClient.post()
                .uri("/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(streaming)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(s -> log.info("Streaming completion SSE connection established: model={}", request.getModel()))
                .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                .map(line -> line.substring(6))
                .flatMap(this::parseCompletionChunk)
                .doOnComplete(() -> log.info("Streaming completion finished: model={}", request.getModel()))
                .doOnError(e -> log.error("Stream completion error: {}", e.getMessage()));
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        log.info("Sending chat request: model={}, messages={}, maxTokens={}, temperature={}",
                request.getModel(),
                request.getMessages() != null ? request.getMessages().size() : 0,
                request.getMaxTokens(), request.getTemperature());
        ChatRequest nonStreaming = Boolean.TRUE.equals(request.getStream())
                ? copyWithStream(request, false) : request;
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(nonStreaming)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .filter(e -> !(e instanceof WebClientResponseException.BadRequest))
                        .doBeforeRetry(signal -> log.info("Retrying chat request (attempt {}): model={}",
                                signal.totalRetries() + 1, request.getModel())))
                .doOnSuccess(resp -> log.info("Chat response received: model={}, finishReason={}, usage={}",
                        resp.getModel() != null ? resp.getModel() : request.getModel(),
                        resp.getChoices() != null && !resp.getChoices().isEmpty()
                                ? resp.getChoices().get(0).getFinishReason() : "n/a",
                        resp.getUsage()))
                .doOnError(e -> log.error("Chat error: {}", e.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::mapError);
    }

    @Override
    public Flux<ChatResponse> streamChat(ChatRequest request) {
        log.info("Sending streaming chat request: model={}, messages={}, maxTokens={}",
                request.getModel(),
                request.getMessages() != null ? request.getMessages().size() : 0,
                request.getMaxTokens());
        ChatRequest streaming = copyWithStream(request, true);
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(streaming)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(s -> log.info("Streaming chat SSE connection established: model={}", request.getModel()))
                .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                .map(line -> line.substring(6))
                .flatMap(this::parseChatChunk)
                .doOnComplete(() -> log.info("Streaming chat finished: model={}", request.getModel()))
                .doOnError(e -> log.error("Stream chat error: {}", e.getMessage()));
    }

    private LLMException mapError(WebClientResponseException e) {
        log.error("LLM API error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
        return new LLMException("LLM API error: " + e.getMessage(), e.getStatusCode().value(), "api_error");
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
            return Mono.just(objectMapper.readValue(json, CompletionResponse.class));
        } catch (Exception e) {
            log.warn("Failed to parse completion chunk: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<ChatResponse> parseChatChunk(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, ChatResponse.class));
        } catch (Exception e) {
            log.warn("Failed to parse chat chunk: {}", e.getMessage());
            return Mono.empty();
        }
    }
}
