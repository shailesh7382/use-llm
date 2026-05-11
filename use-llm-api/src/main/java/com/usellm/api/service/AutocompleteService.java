package com.usellm.api.service;

import com.usellm.api.dto.AutocompleteRequestDto;
import com.usellm.core.model.CompletionRequest;
import com.usellm.core.model.CompletionResponse;
import com.usellm.core.port.LLMPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AutocompleteService {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);

    private final LLMPort llmPort;

    public AutocompleteService(LLMPort llmPort) {
        this.llmPort = llmPort;
    }

    public Mono<CompletionResponse> complete(AutocompleteRequestDto request) {
        log.info("Completion request: model={}, promptLength={}, maxTokens={}, temperature={}",
                request.getModel(), request.getPrompt().length(), request.getMaxTokens(), request.getTemperature());
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stop(request.getStop())
                .suffix(request.getSuffix())
                .stream(false)
                .build();
        return llmPort.complete(completionRequest)
                .doOnSuccess(resp -> {
                    String model = resp.getModel() != null ? resp.getModel() : request.getModel();
                    int choices = resp.getChoices() != null ? resp.getChoices().size() : 0;
                    log.info("Completion response received: model={}, choices={}, usage={}",
                            model, choices, resp.getUsage());
                })
                .doOnError(e -> log.error("Completion failed for model={}: {}", request.getModel(), e.getMessage()));
    }

    public Flux<CompletionResponse> streamComplete(AutocompleteRequestDto request) {
        log.info("Streaming completion request: model={}, promptLength={}, maxTokens={}",
                request.getModel(), request.getPrompt().length(), request.getMaxTokens());
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stop(request.getStop())
                .suffix(request.getSuffix())
                .stream(true)
                .build();
        return llmPort.streamComplete(completionRequest)
                .doOnSubscribe(s -> log.info("Streaming completion started: model={}", request.getModel()))
                .doOnComplete(() -> log.info("Streaming completion finished: model={}", request.getModel()))
                .doOnError(e -> log.error("Streaming completion error for model={}: {}", request.getModel(), e.getMessage()));
    }
}
