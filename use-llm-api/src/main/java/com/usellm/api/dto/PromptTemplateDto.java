package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.usellm.core.model.PromptExample;
import com.usellm.core.model.PromptTemplate;
import com.usellm.core.model.PromptVariable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Full representation of a {@link PromptTemplate} used for create / update / list responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptTemplateDto {

    private String id;
    private String name;
    private String description;

    /** One-sentence role the AI should adopt (prepended to the system message). */
    private String persona;

    /** Core behavioural instructions appended after the persona. Supports {{variable}} syntax. */
    private String systemPrompt;

    /**
     * Template string for the final user message.
     * Supports {{variable}} syntax.
     * Example: {@code "Translate the following text from {{source_lang}} to {{target_lang}}:\n\n{{text}}"}
     */
    private String userPromptTemplate;

    /** Format constraints appended to the end of the system message. */
    private String outputFormat;

    /** Few-shot examples shown to the model between the system and the user message. */
    private List<PromptExample> examples = new ArrayList<>();

    /** Variable declarations for all {{placeholders}} used in this template. */
    private List<PromptVariable> variables = new ArrayList<>();

    /** Whether this is a read-only built-in template. */
    private Boolean builtIn;

    private Instant createdAt;
    private Instant updatedAt;

    public PromptTemplateDto() {}

    // -----------------------------------------------------------------------
    // Conversion helpers
    // -----------------------------------------------------------------------

    /** Converts a domain {@link PromptTemplate} into a DTO. */
    public static PromptTemplateDto from(PromptTemplate t) {
        PromptTemplateDto dto = new PromptTemplateDto();
        dto.id = t.getId();
        dto.name = t.getName();
        dto.description = t.getDescription();
        dto.persona = t.getPersona();
        dto.systemPrompt = t.getSystemPrompt();
        dto.userPromptTemplate = t.getUserPromptTemplate();
        dto.outputFormat = t.getOutputFormat();
        dto.examples = t.getExamples();
        dto.variables = t.getVariables();
        dto.builtIn = t.isBuiltIn();
        dto.createdAt = t.getCreatedAt();
        dto.updatedAt = t.getUpdatedAt();
        return dto;
    }

    /** Converts this DTO into a domain {@link PromptTemplate}. */
    public PromptTemplate toDomain() {
        PromptTemplate t = new PromptTemplate();
        t.setId(id);
        t.setName(name);
        t.setDescription(description);
        t.setPersona(persona);
        t.setSystemPrompt(systemPrompt);
        t.setUserPromptTemplate(userPromptTemplate);
        t.setOutputFormat(outputFormat);
        t.setExamples(examples != null ? examples : new ArrayList<>());
        t.setVariables(variables != null ? variables : new ArrayList<>());
        return t;
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
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

