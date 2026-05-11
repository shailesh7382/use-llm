package com.usellm.api.service;

import com.usellm.api.config.PromptProperties;
import com.usellm.core.exception.LLMException;
import com.usellm.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages the registry of {@link PromptTemplate} objects and renders them into ordered
 * {@link Message} sequences ready to be sent to the LLM.
 *
 * <h3>Template registry</h3>
 * <ul>
 *   <li>Built-in templates are loaded from {@code llm.prompt.built-in-templates} at startup
 *       and cannot be modified or deleted via the API.</li>
 *   <li>Custom templates can be created, updated, and deleted at runtime; they are held
 *       in an in-memory {@link ConcurrentHashMap}.</li>
 * </ul>
 *
 * <h3>Variable substitution</h3>
 * Placeholders use the <code>{{variableName}}</code> syntax.  At render time the service:
 * <ol>
 *   <li>Applies caller-supplied values.</li>
 *   <li>Falls back to declared {@link PromptVariable#getDefaultValue()} when no value is supplied.</li>
 *   <li>Throws a 400 {@link LLMException} for required variables that have no value.</li>
 *   <li>Leaves unknown placeholders in place and logs a warning.</li>
 * </ol>
 *
 * <h3>Rendered message sequence</h3>
 * <pre>
 * [SYSTEM] persona + "\n\n" + systemPrompt + "\n\n## Output Format\n" + outputFormat
 * [USER]   example[0].userInput        (repeated for each few-shot pair)
 * [ASST]   example[0].assistantOutput
 * …
 * [USER]   rendered userPromptTemplate
 * </pre>
 */
@Service
public class PromptBuilderService {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilderService.class);

    /** Matches {{variableName}} placeholders. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^}]+?)}}");

    private final Map<String, PromptTemplate> registry = new ConcurrentHashMap<>();
    private final PromptProperties properties;

    public PromptBuilderService(PromptProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.getBuiltInTemplates() == null) return;
        for (PromptProperties.TemplateConfig cfg : properties.getBuiltInTemplates()) {
            PromptTemplate t = fromConfig(cfg);
            t.setBuiltIn(true);
            registry.put(t.getId(), t);
        }
        log.info("PromptBuilderService: loaded {} built-in template(s): {}",
                registry.size(), registry.keySet());
    }

    // -----------------------------------------------------------------------
    // Registry CRUD
    // -----------------------------------------------------------------------

    /**
     * Registers (or replaces) a custom template.  Built-in templates with the same id
     * cannot be overwritten.
     */
    public PromptTemplate register(PromptTemplate template) {
        if (template.getId() == null || template.getId().isBlank()) {
            template.setId(UUID.randomUUID().toString());
        }
        PromptTemplate existing = registry.get(template.getId());
        if (existing != null && existing.isBuiltIn()) {
            throw new LLMException(
                    "Cannot overwrite built-in template '" + template.getId() + "'", 409, "conflict");
        }
        template.setBuiltIn(false);
        if (template.getCreatedAt() == null) template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        registry.put(template.getId(), template);
        log.info("Registered prompt template: id={}", template.getId());
        return template;
    }

    /**
     * Updates an existing custom template.
     *
     * @throws LLMException 404 if not found, 409 if built-in
     */
    public PromptTemplate update(String id, PromptTemplate updated) {
        PromptTemplate existing = registry.get(id);
        if (existing == null) {
            throw new LLMException("Prompt template not found: " + id, 404, "not_found");
        }
        if (existing.isBuiltIn()) {
            throw new LLMException("Cannot modify built-in template '" + id + "'", 409, "conflict");
        }
        updated.setId(id);
        updated.setBuiltIn(false);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(Instant.now());
        registry.put(id, updated);
        log.info("Updated prompt template: id={}", id);
        return updated;
    }

    /**
     * Deletes a custom template.
     *
     * @return {@code true} if deleted, {@code false} if the id was not found
     * @throws LLMException 409 if built-in
     */
    public boolean delete(String id) {
        PromptTemplate existing = registry.get(id);
        if (existing == null) return false;
        if (existing.isBuiltIn()) {
            throw new LLMException("Cannot delete built-in template '" + id + "'", 409, "conflict");
        }
        registry.remove(id);
        log.info("Deleted prompt template: id={}", id);
        return true;
    }

    /** Returns a template by id, or empty if not found. */
    public Optional<PromptTemplate> findById(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    /** Returns an unmodifiable snapshot of all registered templates. */
    public List<PromptTemplate> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Renders the template identified by {@code templateId} into an ordered message list.
     *
     * @param templateId   id of the template to render
     * @param variables    caller-supplied variable values (may be null)
     * @throws LLMException 404 if template not found, 400 if a required variable is missing
     */
    public List<Message> render(String templateId, Map<String, String> variables) {
        PromptTemplate template = findById(templateId)
                .orElseThrow(() -> new LLMException(
                        "Prompt template not found: " + templateId, 404, "not_found"));
        return buildMessages(template, variables);
    }

    /**
     * Builds the full message sequence for a given template and variable values.
     *
     * <p>The returned list contains:
     * <ol>
     *   <li>SYSTEM message (if {@code persona}, {@code systemPrompt}, or {@code outputFormat} is set)</li>
     *   <li>Alternating USER / ASSISTANT few-shot example pairs</li>
     *   <li>USER message rendered from {@code userPromptTemplate}</li>
     * </ol>
     */
    public List<Message> buildMessages(PromptTemplate template, Map<String, String> variables) {
        Map<String, String> resolved = resolveVariables(template, variables);
        List<Message> messages = new ArrayList<>();

        // 1 – system message
        String systemContent = buildSystemContent(template, resolved);
        if (systemContent != null && !systemContent.isBlank()) {
            messages.add(Message.system(systemContent));
        }

        // 2 – few-shot examples (interleaved USER / ASSISTANT)
        if (template.getExamples() != null) {
            for (PromptExample example : template.getExamples()) {
                if (example.getUserInput() != null && !example.getUserInput().isBlank()) {
                    messages.add(Message.user(substitute(example.getUserInput(), resolved)));
                }
                if (example.getAssistantOutput() != null && !example.getAssistantOutput().isBlank()) {
                    messages.add(Message.assistant(substitute(example.getAssistantOutput(), resolved)));
                }
            }
        }

        // 3 – user message (rendered user prompt template)
        if (template.getUserPromptTemplate() != null && !template.getUserPromptTemplate().isBlank()) {
            String userContent = substitute(template.getUserPromptTemplate(), resolved);
            messages.add(Message.user(userContent));
        }

        log.debug("Rendered template '{}' into {} messages with {} variables",
                template.getId(), messages.size(), resolved.size());
        return messages;
    }

    /**
     * Returns the resolved variables map for diagnostic / logging purposes.
     */
    public Map<String, String> resolveVariables(PromptTemplate template, Map<String, String> provided) {
        Map<String, String> resolved = new LinkedHashMap<>(provided != null ? provided : Collections.emptyMap());

        if (template.getVariables() != null) {
            for (PromptVariable varDef : template.getVariables()) {
                if (!resolved.containsKey(varDef.getName())) {
                    if (varDef.getDefaultValue() != null) {
                        resolved.put(varDef.getName(), varDef.getDefaultValue());
                    } else if (varDef.isRequired()) {
                        throw new LLMException(
                                "Missing required template variable: '" + varDef.getName() + "'",
                                400, "template_variable_missing");
                    }
                }
            }
        }

        // Warn about any remaining unresolved placeholders after substitution attempt
        return resolved;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Assembles the system message text from persona + systemPrompt + outputFormat. */
    private String buildSystemContent(PromptTemplate t, Map<String, String> vars) {
        StringBuilder sb = new StringBuilder();

        appendSection(sb, t.getPersona(), vars, null);
        appendSection(sb, t.getSystemPrompt(), vars, null);
        appendSection(sb, t.getOutputFormat(), vars, "## Output Format");

        return sb.length() > 0 ? sb.toString() : null;
    }

    private void appendSection(StringBuilder sb, String raw, Map<String, String> vars, String heading) {
        if (raw == null || raw.isBlank()) return;
        String rendered = substitute(raw, vars);
        if (rendered.isBlank()) return;
        if (sb.length() > 0) sb.append("\n\n");
        if (heading != null) {
            sb.append(heading).append("\n");
        }
        sb.append(rendered);
    }

    /**
     * Replaces all {@code {{varName}}} placeholders in {@code template} with values from
     * {@code vars}.  Unrecognised placeholders are left in place with a warning.
     */
    String substitute(String template, Map<String, String> vars) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String key = m.group(1).trim();
            String value = vars.get(key);
            if (value == null) {
                log.warn("Unresolved placeholder '{{{}}}' in template text", key);
                m.appendReplacement(result, Matcher.quoteReplacement(m.group(0)));
            } else {
                m.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    private PromptTemplate fromConfig(PromptProperties.TemplateConfig cfg) {
        PromptTemplate t = new PromptTemplate();
        t.setId(cfg.getId());
        t.setName(cfg.getName());
        t.setDescription(cfg.getDescription());
        t.setPersona(cfg.getPersona());
        t.setSystemPrompt(cfg.getSystemPrompt());
        t.setUserPromptTemplate(cfg.getUserPromptTemplate());
        t.setOutputFormat(cfg.getOutputFormat());
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());

        if (cfg.getVariables() != null) {
            t.setVariables(cfg.getVariables().stream()
                    .map(vc -> new PromptVariable(
                            vc.getName(), vc.getDescription(), vc.getDefaultValue(), vc.isRequired()))
                    .collect(Collectors.toList()));
        }

        if (cfg.getExamples() != null) {
            t.setExamples(cfg.getExamples().stream()
                    .map(ec -> new PromptExample(
                            ec.getUserInput(), ec.getAssistantOutput(), ec.getDescription()))
                    .collect(Collectors.toList()));
        }

        return t;
    }
}

