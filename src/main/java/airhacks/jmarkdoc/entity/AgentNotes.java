package airhacks.jmarkdoc.entity;

import java.util.Optional;

import airhacks.jmarkdoc.control.MarkdownRenderer;

/**
 * The fixed, ordered set of nine contract sections extracted from a method's
 * JavaDoc comment for the Agent Notes block (Requirement 7).
 *
 * <p>This is a plain immutable value type with no behavior beyond
 * {@link #isEmpty()}, used by the pure rendering helper
 * {@link MarkdownRenderer#renderAgentNotes(AgentNotes)} so it can be exercised
 * in isolation without invoking the compiler.
 *
 * <p>Each section is {@link Optional}: it is present only when the source
 * JavaDoc actually contains the corresponding contract content. The renderer
 * never invents content — a section is rendered only when, and exactly as, it
 * appears here.
 *
 * @param preconditions  the {@code Preconditions} contract content, if present
 * @param postconditions the {@code Postconditions} contract content, if present
 * @param sideEffects    the {@code Side effects} contract content, if present
 * @param idempotency    the {@code Idempotency} contract content, if present
 * @param authorization  the {@code Authorization} contract content, if present
 * @param transactions   the {@code Transactions} contract content, if present
 * @param concurrency    the {@code Concurrency} contract content, if present
 * @param threadSafety   the {@code Thread-safety} contract content, if present
 * @param errorHandling  the {@code Error handling} contract content, if present
 */
public record AgentNotes(
        Optional<String> preconditions,
        Optional<String> postconditions,
        Optional<String> sideEffects,
        Optional<String> idempotency,
        Optional<String> authorization,
        Optional<String> transactions,
        Optional<String> concurrency,
        Optional<String> threadSafety,
        Optional<String> errorHandling) {

    /**
     * Indicates whether no contract section is present, in which case the
     * renderer omits the Agent Notes section entirely.
     *
     * @return {@code true} when all nine contract sections are absent
     */
    public boolean isEmpty() {
        return preconditions.isEmpty()
                && postconditions.isEmpty()
                && sideEffects.isEmpty()
                && idempotency.isEmpty()
                && authorization.isEmpty()
                && transactions.isEmpty()
                && concurrency.isEmpty()
                && threadSafety.isEmpty()
                && errorHandling.isEmpty();
    }
}
