package com.usellm.api.service;

import com.usellm.core.exception.LLMException;
import com.usellm.core.model.Message;
import com.usellm.core.model.Role;
import com.usellm.memory.config.MemoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Validates and optionally repairs the role-ordering of a message sequence before it is
 * forwarded to the underlying LLM.
 *
 * <h3>Alignment rules</h3>
 * <ol>
 *   <li>At most one SYSTEM message, and it must appear first.</li>
 *   <li>Non-system messages must strictly alternate: USER → ASSISTANT → USER → …</li>
 *   <li>The last non-system message must be from the USER role.</li>
 * </ol>
 *
 * <h3>Strategies</h3>
 * <ul>
 *   <li>{@code STRICT}    – throw a 400 {@link LLMException} on any violation.</li>
 *   <li>{@code AUTO_FIX}  – merge consecutive same-role messages and reorder system messages.</li>
 *   <li>{@code WARN_ONLY} – log warnings but return the original list unchanged.</li>
 * </ul>
 */
@Service
public class MessageAlignmentService {

    private static final Logger log = LoggerFactory.getLogger(MessageAlignmentService.class);

    /** Message alignment enforcement strategies. */
    public enum AlignmentStrategy {
        /** Throw a {@link LLMException} when any alignment rule is violated. */
        STRICT,
        /**
         * Automatically repair violations:
         * <ul>
         *   <li>Multiple SYSTEM messages → merged into one in first position.</li>
         *   <li>Misplaced SYSTEM messages → moved to front.</li>
         *   <li>Consecutive same-role messages → merged with double newline separator.</li>
         * </ul>
         */
        AUTO_FIX,
        /** Log violations as warnings but forward the original list to the LLM unchanged. */
        WARN_ONLY
    }

    private final MemoryConfig memoryConfig;

    public MessageAlignmentService(MemoryConfig memoryConfig) {
        this.memoryConfig = memoryConfig;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Aligns messages using the strategy configured in {@code llm.memory.alignment-strategy}.
     */
    public List<Message> align(List<Message> messages) {
        AlignmentStrategy strategy;
        try {
            strategy = AlignmentStrategy.valueOf(memoryConfig.getAlignmentStrategy().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown alignment strategy '{}', falling back to AUTO_FIX", memoryConfig.getAlignmentStrategy());
            strategy = AlignmentStrategy.AUTO_FIX;
        }
        log.info("Aligning {} message(s) using strategy={}", messages != null ? messages.size() : 0, strategy);
        return align(messages, strategy);
    }

    /**
     * Aligns messages using an explicit strategy.
     *
     * @param messages the message list to inspect
     * @param strategy the enforcement level
     * @return an aligned (possibly new) list of messages
     * @throws LLMException if strategy is {@code STRICT} and violations are found
     */
    public List<Message> align(List<Message> messages, AlignmentStrategy strategy) {
        if (messages == null || messages.isEmpty()) {
            log.info("Alignment skipped: empty or null message list");
            return messages;
        }

        List<String> violations = validate(messages);

        if (violations.isEmpty()) {
            log.info("Message alignment OK: {} message(s) passed validation (strategy={})", messages.size(), strategy);
            return messages;
        }

        log.info("Alignment violations found: count={}, strategy={}, violations={}",
                violations.size(), strategy, violations);

        switch (strategy) {
            case STRICT:
                String details = String.join("; ", violations);
                log.error("Alignment violations (STRICT): {}", details);
                throw new LLMException("Message alignment error: " + details, 400, "alignment_error");

            case WARN_ONLY:
                violations.forEach(v -> log.warn("Alignment warning (WARN_ONLY): {}", v));
                log.info("Returning original {} message(s) unchanged (WARN_ONLY)", messages.size());
                return messages;

            case AUTO_FIX:
            default:
                violations.forEach(v -> log.info("Auto-fixing alignment violation: {}", v));
                List<Message> fixed = fix(messages);
                log.info("Alignment fixed (AUTO_FIX): {} → {} message(s)", messages.size(), fixed.size());
                return fixed;
        }
    }

    /**
     * Returns a (possibly empty) list of human-readable violation descriptions.
     * An empty list means the sequence is perfectly aligned.
     */
    public List<String> validate(List<Message> messages) {
        List<String> violations = new ArrayList<>();
        if (messages == null || messages.isEmpty()) return violations;

        // Rule 1 – system messages must only appear at position 0
        for (int i = 1; i < messages.size(); i++) {
            if (messages.get(i).getRole() == Role.SYSTEM) {
                violations.add("SYSTEM message at index " + i + " (must be first)");
            }
        }

        // Rule 2 – multiple system messages
        long systemCount = messages.stream().filter(m -> m.getRole() == Role.SYSTEM).count();
        if (systemCount > 1) {
            violations.add(systemCount + " SYSTEM messages found (only 1 allowed)");
        }

        // Rule 3 – non-system messages must alternate USER ↔ ASSISTANT
        List<Message> nonSystem = messages.stream()
                .filter(m -> m.getRole() != Role.SYSTEM)
                .collect(Collectors.toList());

        for (int i = 1; i < nonSystem.size(); i++) {
            Role prev = nonSystem.get(i - 1).getRole();
            Role curr = nonSystem.get(i).getRole();
            if (curr == prev) {
                violations.add("Consecutive " + curr.getValue() +
                        " messages at non-system positions " + (i - 1) + " and " + i);
            }
        }

        // Rule 4 – last non-system message must be USER
        if (!nonSystem.isEmpty()) {
            Role last = nonSystem.get(nonSystem.size() - 1).getRole();
            if (last != Role.USER) {
                violations.add("Last message has role '" + last.getValue() + "'; expected 'user'");
            }
        }

        if (violations.isEmpty()) {
            log.info("Validation passed: {} message(s), no violations", messages.size());
        } else {
            log.info("Validation found {} violation(s) in {} message(s)", violations.size(), messages.size());
        }
        return violations;
    }

    // -----------------------------------------------------------------------
    // Auto-fix logic
    // -----------------------------------------------------------------------

    private List<Message> fix(List<Message> messages) {
        List<Message> systemMessages = messages.stream()
                .filter(m -> m.getRole() == Role.SYSTEM)
                .collect(Collectors.toList());
        List<Message> nonSystem = messages.stream()
                .filter(m -> m.getRole() != Role.SYSTEM)
                .collect(Collectors.toList());

        List<Message> result = new ArrayList<>();

        // Step 1 – consolidate system messages
        if (!systemMessages.isEmpty()) {
            if (systemMessages.size() == 1) {
                result.add(systemMessages.get(0));
            } else {
                // Merge all system messages into one
                String merged = systemMessages.stream()
                        .map(Message::getContent)
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining("\n\n"));
                result.add(Message.system(merged));
            }
        }

        // Step 2 – merge consecutive same-role non-system messages
        List<Message> deduped = new ArrayList<>();
        for (Message msg : nonSystem) {
            if (!deduped.isEmpty() && deduped.get(deduped.size() - 1).getRole() == msg.getRole()) {
                Message prev = deduped.remove(deduped.size() - 1);
                String combined = joinContent(prev.getContent(), msg.getContent());
                deduped.add(Message.builder()
                        .role(prev.getRole())
                        .content(combined)
                        .name(prev.getName())
                        .timestamp(prev.getTimestamp())
                        .build());
            } else {
                deduped.add(msg);
            }
        }

        result.addAll(deduped);
        return result;
    }

    private static String joinContent(String a, String b) {
        String sa = a != null ? a.trim() : "";
        String sb = b != null ? b.trim() : "";
        if (sa.isEmpty()) return sb;
        if (sb.isEmpty()) return sa;
        return sa + "\n\n" + sb;
    }
}

