package airhacks.jmarkdoc.entity;

import airhacks.jmarkdoc.control.MarkdownRenderer;

/**
 * A single method or constructor parameter, after extraction from the program
 * model.
 *
 * <p>This is a plain immutable value type with no behavior, used by the pure
 * rendering helpers (for example {@link MarkdownRenderer#formatSignature}) so
 * they can be exercised in isolation without invoking the compiler.
 *
 * @param name        the declared parameter name (for example {@code id})
 * @param typeName    the declared parameter type as written in the source model
 *                    (for example {@code long} or {@code java.util.List<String>})
 * @param description the {@code @param} description text, or an empty string
 *                    when the parameter has no documentation
 */
public record ParamDoc(String name, String typeName, String description) {
}
