package com.usellm.core.exception;

public class LLMException extends RuntimeException {

    private final int statusCode;
    private final String errorType;

    public LLMException(String message) {
        super(message);
        this.statusCode = 500;
        this.errorType = "internal_error";
    }

    public LLMException(String message, int statusCode, String errorType) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public LLMException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.errorType = "internal_error";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }
}
