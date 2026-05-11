package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLMModel {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private Long created;

    @JsonProperty("owned_by")
    private String ownedBy;

    @JsonProperty("description")
    private String description;

    @JsonProperty("context_length")
    private Integer contextLength;

    public LLMModel() {
    }

    public LLMModel(String id, String object, Long created, String ownedBy,
                    String description, Integer contextLength) {
        this.id = id;
        this.object = object;
        this.created = created;
        this.ownedBy = ownedBy;
        this.description = description;
        this.contextLength = contextLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String object;
        private Long created;
        private String ownedBy;
        private String description;
        private Integer contextLength;

        private Builder() {
        }

        public Builder id(String id) { this.id = id; return this; }
        public Builder object(String object) { this.object = object; return this; }
        public Builder created(Long created) { this.created = created; return this; }
        public Builder ownedBy(String ownedBy) { this.ownedBy = ownedBy; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder contextLength(Integer contextLength) { this.contextLength = contextLength; return this; }

        public LLMModel build() {
            return new LLMModel(id, object, created, ownedBy, description, contextLength);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    public String getOwnedBy() { return ownedBy; }
    public void setOwnedBy(String ownedBy) { this.ownedBy = ownedBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getContextLength() { return contextLength; }
    public void setContextLength(Integer contextLength) { this.contextLength = contextLength; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LLMModel)) return false;
        LLMModel that = (LLMModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return "LLMModel{id='" + id + "', ownedBy='" + ownedBy + "'}"; }
}
