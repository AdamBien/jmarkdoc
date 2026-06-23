package airhacks.jmarkdoc.entity;

import airhacks.jmarkdoc.control.MarkdownRenderer;

/**
 * A single thrown exception entry for a method or constructor, sourced from a
 * {@code @throws} or {@code @exception} block tag after extraction from the
 * program model.
 *
 * <p>This is a plain immutable value type with no behavior, used by the pure
 * rendering helpers (for example {@link MarkdownRenderer#formatMethod}) so they
 * can be exercised in isolation without invoking the compiler.
 *
 * @param exceptionType the declared exception type as written in the source
 *                      model (for example {@code java.io.IOException})
 * @param description   the {@code @throws} description text, or an empty string
 *                      when the exception has no documentation
 */
public record ThrowsDoc(String exceptionType, String description) {
}
