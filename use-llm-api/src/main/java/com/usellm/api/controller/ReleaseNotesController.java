package com.usellm.api.controller;

import com.usellm.api.dto.ReleaseNotesRequestDto;
import com.usellm.api.dto.ReleaseNotesResponseDto;
import com.usellm.api.service.ReleaseNotesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/release-notes")
public class ReleaseNotesController {

    private final ReleaseNotesService releaseNotesService;

    public ReleaseNotesController(ReleaseNotesService releaseNotesService) {
        this.releaseNotesService = releaseNotesService;
    }

    @PostMapping
    public Mono<ResponseEntity<ReleaseNotesResponseDto>> generate(
            @RequestBody @Valid ReleaseNotesRequestDto request) {
        return releaseNotesService.generate(request).map(ResponseEntity::ok);
    }
}
