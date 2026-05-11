package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelSearchRequestDto {

    /** Filter by keyword in model ID or description (optional) */
    private String query;

    /** Filter by owner */
    private String ownedBy;

    /** Maximum number of results */
    private Integer limit = 50;

    public ModelSearchRequestDto() {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String query, ownedBy;
        private Integer limit = 50;
        private Builder() {}
        public Builder query(String v) { query = v; return this; }
        public Builder ownedBy(String v) { ownedBy = v; return this; }
        public Builder limit(Integer v) { limit = v; return this; }
        public ModelSearchRequestDto build() {
            ModelSearchRequestDto d = new ModelSearchRequestDto();
            d.query = query; d.ownedBy = ownedBy; d.limit = limit;
            return d;
        }
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getOwnedBy() { return ownedBy; }
    public void setOwnedBy(String ownedBy) { this.ownedBy = ownedBy; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
