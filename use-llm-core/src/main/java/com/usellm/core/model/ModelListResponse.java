package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelListResponse {

    @JsonProperty("object")
    private String object;

    @JsonProperty("data")
    private List<LLMModel> data;

    public ModelListResponse() {}

    public ModelListResponse(String object, List<LLMModel> data) {
        this.object = object;
        this.data = data;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String object;
        private List<LLMModel> data;
        private Builder() {}
        public Builder object(String object) { this.object = object; return this; }
        public Builder data(List<LLMModel> data) { this.data = data; return this; }
        public ModelListResponse build() { return new ModelListResponse(object, data); }
    }

    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    public List<LLMModel> getData() { return data; }
    public void setData(List<LLMModel> data) { this.data = data; }
}
