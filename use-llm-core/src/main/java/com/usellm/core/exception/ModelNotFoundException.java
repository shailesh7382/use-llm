package com.usellm.core.exception;

public class ModelNotFoundException extends LLMException {

    public ModelNotFoundException(String modelId) {
        super("Model not found: " + modelId, 404, "model_not_found");
    }
}
