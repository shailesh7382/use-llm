package com.usellm.api.controller;

import com.usellm.api.dto.AutocompleteRequestDto;
import com.usellm.api.service.AutocompleteService;
import com.usellm.core.model.CompletionResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/completions")
public class AutocompleteController {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteController.class);

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    /** Text completion (non-streaming). POST /api/v1/completions */
    @PostMapping
    public Mono<ResponseEntity<CompletionResponse>> complete(
            @RequestBody @Valid AutocompleteRequestDto request) {
        return autocompleteService.complete(request).map(ResponseEntity::ok);
    }

    /** Streaming text completion (SSE). POST /api/v1/completions/stream */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CompletionResponse> streamComplete(
            @RequestBody @Valid AutocompleteRequestDto request) {
        return autocompleteService.streamComplete(request);
    }
}
