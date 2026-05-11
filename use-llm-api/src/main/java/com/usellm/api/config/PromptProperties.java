package com.usellm.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the prompt template and message-alignment subsystem.
 * Bound from the {@code llm.prompt} prefix in application.yml.
 *
 * <p>Built-in templates are loaded at startup and are read-only via the REST API.
 * Custom templates created at runtime are stored in the in-memory registry and are lost
 * on restart unless external persistence is added.
 */
@Configuration
@ConfigurationProperties(prefix = "llm.prompt")
public class PromptProperties {

    /**
     * Default alignment strategy for all chat interactions.
     * Overridable per-request.  Values: STRICT | AUTO_FIX | WARN_ONLY.
     */
    private String alignmentStrategy = "AUTO_FIX";

    /** Prompt templates loaded from configuration at startup. */
    private List<TemplateConfig> builtInTemplates = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Nested config types
    // -----------------------------------------------------------------------

    public static class TemplateConfig {
        private String id;
        private String name;
        private String description;
        private String persona;
        private String systemPrompt;
        private String userPromptTemplate;
        private String outputFormat;
        private List<ExampleConfig> examples = new ArrayList<>();
        private List<VariableConfig> variables = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPersona() { return persona; }
        public void setPersona(String persona) { this.persona = persona; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getUserPromptTemplate() { return userPromptTemplate; }
        public void setUserPromptTemplate(String t) { this.userPromptTemplate = t; }
        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
        public List<ExampleConfig> getExamples() { return examples; }
        public void setExamples(List<ExampleConfig> examples) { this.examples = examples; }
        public List<VariableConfig> getVariables() { return variables; }
        public void setVariables(List<VariableConfig> variables) { this.variables = variables; }
    }

    public static class ExampleConfig {
        private String userInput;
        private String assistantOutput;
        private String description;

        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        public String getAssistantOutput() { return assistantOutput; }
        public void setAssistantOutput(String assistantOutput) { this.assistantOutput = assistantOutput; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class VariableConfig {
        private String name;
        private String description;
        private String defaultValue;
        private boolean required = false;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }

    // -----------------------------------------------------------------------
    // Root getters / setters
    // -----------------------------------------------------------------------

    public String getAlignmentStrategy() { return alignmentStrategy; }
    public void setAlignmentStrategy(String alignmentStrategy) { this.alignmentStrategy = alignmentStrategy; }
    public List<TemplateConfig> getBuiltInTemplates() { return builtInTemplates; }
    public void setBuiltInTemplates(List<TemplateConfig> builtInTemplates) { this.builtInTemplates = builtInTemplates; }
}

