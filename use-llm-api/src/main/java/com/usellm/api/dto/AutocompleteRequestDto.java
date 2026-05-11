package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutocompleteRequestDto {

    @NotBlank
    private String model;

    @NotBlank
    private String prompt;

    @Builder.Default
    private Integer maxTokens = 128;

    @Builder.Default
    private Double temperature = 0.3;

    @Builder.Default
    private Boolean stream = false;

    private String suffix;

    private List<String> stop;
}
