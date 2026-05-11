package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
