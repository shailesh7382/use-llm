package com.usellm.api.service;

import com.usellm.api.dto.ModelSearchRequestDto;
import com.usellm.core.exception.ModelNotFoundException;
import com.usellm.core.model.LLMModel;
import com.usellm.core.model.ModelListResponse;
import com.usellm.core.port.LLMPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelSearchService {

    private final LLMPort llmPort;

    public Mono<List<LLMModel>> searchModels(ModelSearchRequestDto request) {
        log.debug("Searching models with query={}, ownedBy={}", request.getQuery(), request.getOwnedBy());
        return llmPort.listModels()
                .map(ModelListResponse::getData)
                .map(models -> {
                    List<LLMModel> filtered = models;
                    if (request.getQuery() != null && !request.getQuery().isBlank()) {
                        String q = request.getQuery().toLowerCase();
                        filtered = filtered.stream()
                                .filter(m -> (m.getId() != null && m.getId().toLowerCase().contains(q))
                                        || (m.getDescription() != null && m.getDescription().toLowerCase().contains(q)))
                                .collect(Collectors.toList());
                    }
                    if (request.getOwnedBy() != null && !request.getOwnedBy().isBlank()) {
                        String owner = request.getOwnedBy().toLowerCase();
                        filtered = filtered.stream()
                                .filter(m -> m.getOwnedBy() != null && m.getOwnedBy().toLowerCase().contains(owner))
                                .collect(Collectors.toList());
                    }
                    if (request.getLimit() != null && request.getLimit() > 0) {
                        filtered = filtered.stream().limit(request.getLimit()).collect(Collectors.toList());
                    }
                    log.debug("Found {} models matching criteria", filtered.size());
                    return filtered;
                });
    }

    public Mono<LLMModel> getModelById(String modelId) {
        return llmPort.getModel(modelId)
                .switchIfEmpty(Mono.error(new ModelNotFoundException(modelId)));
    }
}
