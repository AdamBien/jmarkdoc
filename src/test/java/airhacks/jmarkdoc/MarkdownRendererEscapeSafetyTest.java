package airhacks.jmarkdoc;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

/**
 * Property-based tests for the Markdown escaping safety of
 * {@link MarkdownRenderer#escape(String)}.
 */
class MarkdownRendererEscapeSafetyTest {

    /**
     * The set of Markdown control characters the renderer escapes. Mirrors the
     * private {@code SPECIAL_CHARACTERS} constant in {@link MarkdownRenderer}.
     */
    private static final String SPECIAL_CHARACTERS = "\\`*_{}[]()#+-.!|<>";

    // Feature: jmarkdoc, property 1 — escaped output contains no unescaped Markdown control characters
    /**
     * Property 1 — escaped output contains no UNESCAPED Markdown control
     * characters for any input string: every occurrence of a special character
     * in the escaped output is immediately preceded by an escaping backslash.
     *
     * <p>Validates: Requirement 4.3 / Requirement 9.
     */
    @Property(tries = 200)
    void escapedOutputHasNoUnescapedControlCharacters(
            @ForAll @StringLength(min = 0, max = 200) String raw) {
        var escaped = MarkdownRenderer.escape(raw);
        // Scan the escaped output consuming escape sequences: a backslash
        // escapes the character that immediately follows it. Any special
        // character that is NOT the escaped second half of a "\X" pair is an
        // unescaped Markdown control character and violates the property.
        var index = 0;
        while (index < escaped.length()) {
            var current = escaped.charAt(index);
            if (current == '\\') {
                // Escaping backslash: it must actually escape a following char.
                check(
                        index + 1 < escaped.length(),
                        "Dangling escaping backslash at end of output. raw=["
                                + raw + "] escaped=[" + escaped + "]");
                // Skip the escape sequence (backslash + the escaped character).
                index += 2;
                continue;
            }
            check(
                    !isSpecial(current),
                    "Unescaped Markdown control character '" + current
                            + "' found in escaped output. raw=[" + raw
                            + "] escaped=[" + escaped + "]");
            index++;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean isSpecial(char c) {
        return SPECIAL_CHARACTERS.indexOf(c) >= 0;
    }
}
