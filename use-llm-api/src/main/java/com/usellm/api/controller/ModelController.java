package com.usellm.api.controller;

import com.usellm.api.dto.ModelSearchRequestDto;
import com.usellm.api.service.ModelSearchService;
import com.usellm.core.model.LLMModel;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private static final Logger log = LoggerFactory.getLogger(ModelController.class);

    private final ModelSearchService modelSearchService;

    public ModelController(ModelSearchService modelSearchService) {
        this.modelSearchService = modelSearchService;
    }

    /** List all available models. GET /api/v1/models */
    @GetMapping
    public Mono<ResponseEntity<List<LLMModel>>> listModels(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String ownedBy,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {

        ModelSearchRequestDto searchRequest = ModelSearchRequestDto.builder()
                .query(query)
                .ownedBy(ownedBy)
                .limit(limit)
                .build();
        return modelSearchService.searchModels(searchRequest).map(ResponseEntity::ok);
    }

    /** Search models with a POST body. POST /api/v1/models/search */
    @PostMapping("/search")
    public Mono<ResponseEntity<List<LLMModel>>> searchModels(
            @RequestBody @Valid ModelSearchRequestDto request) {
        return modelSearchService.searchModels(request).map(ResponseEntity::ok);
    }

    /** Get a specific model by ID. GET /api/v1/models/{modelId} */
    @GetMapping("/{modelId}")
    public Mono<ResponseEntity<LLMModel>> getModel(@PathVariable String modelId) {
        return modelSearchService.getModelById(modelId).map(ResponseEntity::ok);
    }
}
