package airhacks.jmarkdoc;

/**
 * A single field, enum constant, or record component, after extraction from the
 * program model.
 *
 * <p>This is a plain immutable value type with no behavior, used by the pure
 * rendering helpers (for example {@link MarkdownRenderer#formatField}) so they
 * can be exercised in isolation without invoking the compiler.
 *
 * @param name        the declared member name (for example {@code LOGGER})
 * @param typeName    the declared member type as written in the source model
 *                    (for example {@code java.lang.System.Logger}), or an empty
 *                    string when no type applies
 * @param description the member's JavaDoc body text, or an empty string when
 *                    the member has no documentation
 */
public record FieldDoc(String name, String typeName, String description) {
}
