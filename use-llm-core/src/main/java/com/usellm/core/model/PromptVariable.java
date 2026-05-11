package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Defines a named variable that can be substituted into a {@link PromptTemplate}.
 * Variables are referenced in template strings using {{name}} syntax.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptVariable {

    /** Variable name used in {{name}} placeholders. */
    private String name;

    /** Human-readable description of what this variable represents. */
    private String description;

    /** Default value used when no value is supplied at render time. */
    private String defaultValue;

    /** Whether this variable must be supplied by the caller. */
    private boolean required = false;

    public PromptVariable() {}

    public PromptVariable(String name, String description, String defaultValue, boolean required) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.required = required;
    }

    /** Factory: create a mandatory variable. */
    public static PromptVariable required(String name, String description) {
        return new PromptVariable(name, description, null, true);
    }

    /** Factory: create an optional variable with a default. */
    public static PromptVariable optional(String name, String description, String defaultValue) {
        return new PromptVariable(name, description, defaultValue, false);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    @Override
    public String toString() {
        return "PromptVariable{name='" + name + "', required=" + required + "}";
    }
}

