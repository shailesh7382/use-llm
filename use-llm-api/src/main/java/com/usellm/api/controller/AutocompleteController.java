package com.usellm.api.controller;

import com.usellm.api.dto.AutocompleteRequestDto;
import com.usellm.api.service.AutocompleteService;
import com.usellm.core.model.CompletionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/completions")
@RequiredArgsConstructor
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    /**
     * Text completion (non-streaming).
     * POST /api/v1/completions
     */
    @PostMapping
    public Mono<ResponseEntity<CompletionResponse>> complete(
            @RequestBody @Valid AutocompleteRequestDto request) {
        return autocompleteService.complete(request)
                .map(ResponseEntity::ok);
    }

    /**
     * Text completion with streaming (Server-Sent Events).
     * POST /api/v1/completions/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CompletionResponse> streamComplete(
            @RequestBody @Valid AutocompleteRequestDto request) {
        return autocompleteService.streamComplete(request);
    }
}
