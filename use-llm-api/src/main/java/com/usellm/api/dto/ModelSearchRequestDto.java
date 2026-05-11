package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelSearchRequestDto {

    /**
     * Filter by keyword in model ID or description (optional)
     */
    private String query;

    /**
     * Filter by owner
     */
    private String ownedBy;

    /**
     * Maximum number of results
     */
    @Builder.Default
    private Integer limit = 50;
}
