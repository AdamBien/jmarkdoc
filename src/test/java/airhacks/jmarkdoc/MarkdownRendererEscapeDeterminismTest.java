package airhacks.jmarkdoc;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

/**
 * Property-based tests for the determinism and total null-safety of
 * {@link MarkdownRenderer#escape(String)}.
 */
class MarkdownRendererEscapeDeterminismTest {

    // Feature: jmarkdoc, property 2 — escape is total and deterministic
    /**
     * Property 2 — escape is total and deterministic: the same input always
     * yields identical output and never throws, including for {@code null} and
     * empty input.
     *
     * <p>For every generated string the escaping is invoked twice and the two
     * results must be equal (determinism) and the call must never throw
     * (totality). The {@code null} and empty cases are not generatable through
     * {@code @ForAll}, so they are asserted explicitly: both must return the
     * empty string without throwing.
     *
     * <p>Validates: Requirements 5.2, 5.3.
     */
    @Property(tries = 200)
    void escapeIsTotalAndDeterministic(
            @ForAll @StringLength(min = 0, max = 200) String raw) {
        // Explicit null/empty null-safety checks (cannot be generated via @ForAll).
        check("".equals(escapeNeverThrows(null)),
                "escape(null) must return the empty string");
        check("".equals(escapeNeverThrows("")),
                "escape(\"\") must return the empty string");

        // Determinism: the same input yields identical output across calls.
        var first = escapeNeverThrows(raw);
        var second = escapeNeverThrows(raw);
        check(first.equals(second),
                "escape is not deterministic for raw=[" + raw + "]: first=["
                        + first + "] second=[" + second + "]");
    }

    /**
     * Invokes {@link MarkdownRenderer#escape(String)} and fails the property if
     * it throws, converting totality violations into descriptive assertions.
     */
    private static String escapeNeverThrows(String raw) {
        try {
            return MarkdownRenderer.escape(raw);
        } catch (RuntimeException failure) {
            throw new AssertionError(
                    "escape must never throw, but threw for raw=[" + raw + "]: "
                            + failure, failure);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
