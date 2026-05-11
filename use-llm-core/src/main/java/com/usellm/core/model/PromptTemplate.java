package com.usellm.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A reusable, parameterised prompt template that can be rendered into a sequence of
 * {@link Message} objects ready to send to any OpenAI-compatible LLM.
 *
 * <p>Template strings support <code>{{variableName}}</code> placeholder syntax which is
 * replaced with caller-supplied values at render time.
 *
 * <h3>System message construction</h3>
 * The system message is assembled from three optional fields in order:
 * <ol>
 *   <li><b>persona</b>      – one-line role description ("You are a …")</li>
 *   <li><b>systemPrompt</b> – detailed behaviour instructions</li>
 *   <li><b>outputFormat</b> – constraints on the response format</li>
 * </ol>
 *
 * <h3>Message sequence</h3>
 * <pre>
 * [SYSTEM] &lt;persona + systemPrompt + outputFormat&gt;
 * [USER]   &lt;example[0].userInput&gt;
 * [ASST]   &lt;example[0].assistantOutput&gt;
 * …  (additional few-shot pairs)
 * [USER]   &lt;rendered userPromptTemplate&gt;
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptTemplate {

    /** Unique template identifier (slug or UUID). */
    private String id;

    /** Display name shown in listings and UIs. */
    private String name;

    /** Short human-readable description of the template's purpose. */
    private String description;

    /**
     * Persona line prepended to the system message.
     * Example: "You are an expert Java engineer who writes clean, tested code."
     */
    private String persona;

    /**
     * Core system instructions appended after the persona.
     * May contain {{variable}} placeholders.
     */
    private String systemPrompt;

    /**
     * Template string for the final user message.
     * Usually the "task" or "question" the user wants answered.
     * May contain {{variable}} placeholders.
     */
    private String userPromptTemplate;

    /**
     * Output-format instructions appended at the end of the system message.
     * Example: "Respond in JSON. Do not add prose outside the JSON block."
     */
    private String outputFormat;

    /** Ordered few-shot examples injected between system and user messages. */
    private List<PromptExample> examples = new ArrayList<>();

    /** Declarations of all {{variable}} placeholders used in this template. */
    private List<PromptVariable> variables = new ArrayList<>();

    /** True for templates shipped with the application; they cannot be mutated via the API. */
    private boolean builtIn = false;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public PromptTemplate() {}

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String id, name, description, persona, systemPrompt, userPromptTemplate, outputFormat;
        private boolean builtIn = false;
        private List<PromptExample> examples = new ArrayList<>();
        private List<PromptVariable> variables = new ArrayList<>();

        private Builder() {}

        public Builder id(String v)                    { id = v; return this; }
        public Builder name(String v)                  { name = v; return this; }
        public Builder description(String v)           { description = v; return this; }
        public Builder persona(String v)               { persona = v; return this; }
        public Builder systemPrompt(String v)          { systemPrompt = v; return this; }
        public Builder userPromptTemplate(String v)    { userPromptTemplate = v; return this; }
        public Builder outputFormat(String v)          { outputFormat = v; return this; }
        public Builder builtIn(boolean v)              { builtIn = v; return this; }
        public Builder examples(List<PromptExample> v) { examples = v; return this; }
        public Builder variables(List<PromptVariable> v){ variables = v; return this; }

        public PromptTemplate build() {
            PromptTemplate t = new PromptTemplate();
            t.id = id; t.name = name; t.description = description;
            t.persona = persona; t.systemPrompt = systemPrompt;
            t.userPromptTemplate = userPromptTemplate; t.outputFormat = outputFormat;
            t.builtIn = builtIn; t.examples = examples; t.variables = variables;
            return t;
        }
    }

    // -----------------------------------------------------------------------
    // Getters / Setters
    // -----------------------------------------------------------------------

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
    public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    public List<PromptExample> getExamples() { return examples; }
    public void setExamples(List<PromptExample> examples) { this.examples = examples; }
    public List<PromptVariable> getVariables() { return variables; }
    public void setVariables(List<PromptVariable> variables) { this.variables = variables; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptTemplate)) return false;
        return Objects.equals(id, ((PromptTemplate) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "PromptTemplate{id='" + id + "', name='" + name + "', builtIn=" + builtIn + "}";
    }
}

