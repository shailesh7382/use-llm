package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionRequest {

    @NotBlank
    @JsonProperty("model")
    private String model;

    @NotNull
    @JsonProperty("prompt")
    private String prompt;

    @Builder.Default
    @JsonProperty("max_tokens")
    private Integer maxTokens = 256;

    @Builder.Default
    @JsonProperty("temperature")
    private Double temperature = 0.7;

    @Builder.Default
    @JsonProperty("top_p")
    private Double topP = 1.0;

    @JsonProperty("stop")
    private List<String> stop;

    @Builder.Default
    @JsonProperty("stream")
    private Boolean stream = false;

    @JsonProperty("suffix")
    private String suffix;

    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;
}
