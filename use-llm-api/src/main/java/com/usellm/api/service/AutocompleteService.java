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
        log.debug("Autocomplete request for model={}, prompt.length={}", request.getModel(), request.getPrompt().length());
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stop(request.getStop())
                .suffix(request.getSuffix())
                .stream(false)
                .build();
        return llmPort.complete(completionRequest);
    }

    public Flux<CompletionResponse> streamComplete(AutocompleteRequestDto request) {
        log.debug("Streaming autocomplete request for model={}", request.getModel());
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stop(request.getStop())
                .suffix(request.getSuffix())
                .stream(true)
                .build();
        return llmPort.streamComplete(completionRequest);
    }
}
