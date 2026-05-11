package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single few-shot example pair (user input → assistant output) used in a
 * {@link PromptTemplate} to demonstrate the desired response style.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptExample {

    /** The example user input. */
    private String userInput;

    /** The ideal assistant response for the given user input. */
    private String assistantOutput;

    /** Optional description explaining what this example demonstrates. */
    private String description;

    public PromptExample() {}

    public PromptExample(String userInput, String assistantOutput, String description) {
        this.userInput = userInput;
        this.assistantOutput = assistantOutput;
        this.description = description;
    }

    public static PromptExample of(String userInput, String assistantOutput) {
        return new PromptExample(userInput, assistantOutput, null);
    }

    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }
    public String getAssistantOutput() { return assistantOutput; }
    public void setAssistantOutput(String assistantOutput) { this.assistantOutput = assistantOutput; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "PromptExample{userInput='" + userInput + "'}";
    }
}

