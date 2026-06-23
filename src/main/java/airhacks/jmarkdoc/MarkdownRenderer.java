package airhacks.jmarkdoc;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure rendering helpers that turn program elements and their JavaDoc comment
 * trees into Markdown strings.
 *
 * <p>This class performs no I/O and holds no mutable static state. The
 * tag- and Agent-Notes-formatting helpers are added by later tasks; for now it
 * provides the Markdown {@link #escape(String)} function plus the type-heading,
 * field, and method-signature formatters.
 */
public final class MarkdownRenderer {

    /**
     * Markdown-significant characters that, when present in literal text, must
     * be neutralized so a Markdown renderer treats them as ordinary text rather
     * than as formatting control characters.
     *
     * <p>The backslash ({@code \}) is included so that escaping is reversible:
     * a literal backslash in the source becomes {@code \\}, and removing the
     * escaping backslashes recovers the original sequence exactly.
     */
    private static final String SPECIAL_CHARACTERS = "\\`*_{}[]()#+-.!|<>";

    private MarkdownRenderer() {
        // Utility methods only for now; instance state is introduced by later tasks.
    }

    /**
     * Escapes Markdown-significant characters in the supplied text by prefixing
     * each one with a backslash, so the characters render as literal text.
     *
     * <p>The function is pure: it is side-effect free, deterministic, and total.
     * A {@code null} or empty input yields an empty string. Because every
     * special character (including the backslash itself) is backslash-escaped,
     * removing the escaping backslashes recovers the original string exactly
     * (round-trip).
     *
     * @param raw the text to escape; may be {@code null}
     * @return the escaped text, or an empty string when {@code raw} is
     *         {@code null} or empty
     */
    public static String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        var escaped = new StringBuilder(raw.length() + 8);
        for (var i = 0; i < raw.length(); i++) {
            var current = raw.charAt(i);
            if (SPECIAL_CHARACTERS.indexOf(current) >= 0) {
                escaped.append('\\');
            }
            escaped.append(current);
        }
        return escaped.toString();
    }

    /**
     * Renders the heading block that opens a type's Markdown document: a
     * level-1 heading with the fully qualified name, a package line, and a kind
     * line. The package name and kind value are rendered as inline code.
     *
     * <p>The function is pure: deterministic and free of I/O. {@code null}
     * arguments are treated as empty strings. The fully qualified name is a
     * structural identifier and is rendered verbatim in the heading; the
     * package and kind values are emitted inside inline-code spans, so they are
     * not Markdown-escaped.
     *
     * <p>Example output:
     * <pre>
     * # com.example.api.UserService
     *
     * **Package:** `com.example.api`
     *
     * **Kind:** `class`
     * </pre>
     *
     * @param qualifiedName the fully qualified name of the type
     * @param packageName   the package name of the type
     * @param kind          the type kind, one of {@code class}, {@code interface},
     *                      {@code record}, {@code enum}, or {@code annotation}
     * @return the rendered Markdown heading block
     */
    public static String formatType(String qualifiedName, String packageName, String kind) {
        var block = new StringBuilder();
        block.append("# ").append(nullToEmpty(qualifiedName)).append("\n\n");
        block.append("**Package:** `").append(nullToEmpty(packageName)).append("`\n\n");
        block.append("**Kind:** `").append(nullToEmpty(kind)).append("`\n");
        return block.toString();
    }

    /**
     * Renders a single field entry as a level-3 heading: the field name and,
     * when present, the field type, both formatted as inline code, followed by
     * the escaped description on its own line when present.
     *
     * <p>The function is pure: deterministic and free of I/O. A {@code null}
     * field, or {@code null}/empty components, are tolerated. The name and type
     * are emitted inside inline-code spans and so are not escaped; the
     * description is literal prose and is escaped with {@link #escape(String)}.
     *
     * <p>Example output:
     * <pre>
     * ### `LOGGER` (`java.lang.System.Logger`)
     *
     * Shared logger for the service.
     * </pre>
     *
     * @param field the field to render
     * @return the rendered Markdown field entry
     */
    public static String formatField(FieldDoc field) {
        if (field == null) {
            return "### ``\n";
        }
        var entry = new StringBuilder();
        entry.append("### `").append(nullToEmpty(field.name())).append("`");
        var typeName = field.typeName();
        if (typeName != null && !typeName.isEmpty()) {
            entry.append(" (`").append(typeName).append("`)");
        }
        entry.append("\n");
        var description = field.description();
        if (description != null && !description.isEmpty()) {
            entry.append("\n").append(escape(description)).append("\n");
        }
        return entry.toString();
    }

    /**
     * Renders a method (or constructor) signature: optional modifiers, an
     * optional return type, the member name, and the ordered, parenthesized
     * parameter list. Each parameter is rendered as its declared type followed
     * by its declared name, with parameters separated by {@code ", "} and kept
     * in source declaration order.
     *
     * <p>The result is the bare signature text (no surrounding inline-code
     * backticks or heading marker); callers such as {@code formatMethod} wrap it
     * as needed. Because the signature is intended to live inside an
     * inline-code span, its parts are rendered verbatim rather than escaped.
     *
     * <p>The function is pure: deterministic and free of I/O. {@code null}
     * collections and {@code null} components are tolerated and treated as
     * empty. For a constructor, pass an empty or {@code null} return type.
     *
     * <p>Example results:
     * <pre>
     * public User findById(long id)
     * UserService(javax.sql.DataSource dataSource)
     * </pre>
     *
     * @param modifiers  the declared modifiers in source order (for example
     *                   {@code [public, static]}); may be {@code null} or empty
     * @param returnType the declared return type; may be {@code null} or empty
     *                   (for constructors)
     * @param name       the member name
     * @param parameters the parameters in declaration order; may be {@code null}
     *                   or empty
     * @return the rendered signature text
     */
    public static String formatSignature(List<String> modifiers, String returnType, String name,
            List<ParamDoc> parameters) {
        var signature = new StringBuilder();
        if (modifiers != null) {
            for (var modifier : modifiers) {
                if (modifier != null && !modifier.isEmpty()) {
                    signature.append(modifier).append(' ');
                }
            }
        }
        if (returnType != null && !returnType.isEmpty()) {
            signature.append(returnType).append(' ');
        }
        signature.append(nullToEmpty(name)).append('(');
        if (parameters != null) {
            for (var i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    signature.append(", ");
                }
                var parameter = parameters.get(i);
                if (parameter != null) {
                    signature.append(nullToEmpty(parameter.typeName()))
                            .append(' ')
                            .append(nullToEmpty(parameter.name()));
                }
            }
        }
        signature.append(')');
        return signature.toString();
    }

    /**
     * Renders a complete method (or constructor) entry: the signature as a
     * level-3 heading formatted as inline code, the escaped body description,
     * and the {@code #### Parameters}, {@code #### Returns}, and
     * {@code #### Throws} detail sections, followed by the Agent Notes block.
     * Sections are emitted in this fixed order and any section with no content
     * is omitted entirely.
     *
     * <p>The pre-rendered {@link MemberDoc#signature()} (typically produced by
     * {@link #formatSignature}) is emitted verbatim inside an inline-code span
     * and is therefore not escaped. Prose — the body description and each
     * parameter, return, and throws description — is literal text and is run
     * through {@link #escape(String)}; parameter names, parameter types, and
     * exception types live inside inline-code spans and so are emitted
     * verbatim. The Agent Notes block is delegated to
     * {@link #renderAgentNotes(AgentNotes)}, which omits itself when no
     * contract content is present.
     *
     * <p>The function is pure: deterministic, total, and free of I/O. A
     * {@code null} {@code method} renders an empty inline-code heading;
     * {@code null}/empty collections and components are tolerated and treated as
     * absent, so a member with no JavaDoc renders just its signature heading
     * (Requirement 6.7).
     *
     * <p>Example output:
     * <pre>
     * ### `public User findById(long id)`
     *
     * Looks up a user by id.
     *
     * #### Parameters
     *
     * - `id` (`long`) — the user identifier
     *
     * #### Returns
     *
     * the matching user
     *
     * #### Throws
     *
     * - `NotFoundException` — when no user exists
     *
     * #### Agent Notes
     *
     * - **Preconditions:** id must be positive
     * </pre>
     *
     * @param method the member model to render
     * @return the rendered Markdown method entry
     */
    public static String formatMethod(MemberDoc method) {
        if (method == null) {
            return "### ``\n";
        }
        var entry = new StringBuilder();
        entry.append("### `").append(nullToEmpty(method.signature())).append("`\n");
        var description = method.description();
        if (description != null && !description.isEmpty()) {
            entry.append("\n").append(escape(description)).append("\n");
        }
        appendParameters(entry, method.params());
        appendReturns(entry, method.returns());
        appendThrows(entry, method.thrown());
        var agentNotes = renderAgentNotes(method.agentNotes());
        if (!agentNotes.isEmpty()) {
            entry.append("\n").append(agentNotes);
        }
        return entry.toString();
    }

    /**
     * Appends the {@code #### Parameters} section listing each parameter as
     * {@code - `name` (`type`) — description}. The type segment is omitted when
     * the parameter type is absent, and the em-dash description is omitted when
     * the parameter has no documentation. The whole section is omitted when no
     * parameters are present.
     */
    private static void appendParameters(StringBuilder entry, List<ParamDoc> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        var section = new StringBuilder();
        for (var parameter : parameters) {
            if (parameter == null) {
                continue;
            }
            section.append("- `").append(nullToEmpty(parameter.name())).append('`');
            var typeName = parameter.typeName();
            if (typeName != null && !typeName.isEmpty()) {
                section.append(" (`").append(typeName).append("`)");
            }
            var description = parameter.description();
            if (description != null && !description.isEmpty()) {
                section.append(" — ").append(escape(description));
            }
            section.append('\n');
        }
        if (section.length() > 0) {
            entry.append("\n#### Parameters\n\n").append(section);
        }
    }

    /**
     * Appends the {@code #### Returns} section with the escaped return
     * description, when a {@code @return} text is present.
     */
    private static void appendReturns(StringBuilder entry, Optional<String> returns) {
        if (returns == null || returns.isEmpty()) {
            return;
        }
        entry.append("\n#### Returns\n\n").append(escape(returns.get())).append('\n');
    }

    /**
     * Appends the {@code #### Throws} section listing each thrown exception as
     * {@code - `ExceptionType` — description}, with the em-dash description
     * omitted when the exception has no documentation. The whole section is
     * omitted when no thrown exceptions are present.
     */
    private static void appendThrows(StringBuilder entry, List<ThrowsDoc> thrown) {
        if (thrown == null || thrown.isEmpty()) {
            return;
        }
        var section = new StringBuilder();
        for (var entryDoc : thrown) {
            if (entryDoc == null) {
                continue;
            }
            section.append("- `").append(nullToEmpty(entryDoc.exceptionType())).append('`');
            var description = entryDoc.description();
            if (description != null && !description.isEmpty()) {
                section.append(" — ").append(escape(description));
            }
            section.append('\n');
        }
        if (section.length() > 0) {
            entry.append("\n#### Throws\n\n").append(section);
        }
    }

    /**
     * Renders a single JavaDoc block tag into a self-contained Markdown
     * fragment, mapping each recognized tag kind to a consistent shape and
     * falling back to a generic, non-lossy form for unknown or custom tags.
     *
     * <p>Recognized block tags (the {@code tagName} comparison is
     * case-insensitive and a leading {@code @} is optional):
     * <ul>
     *   <li>{@code @param} &rarr; {@code - `name` — description} (the first
     *       whitespace-delimited token of {@code content} is the parameter
     *       name, the remainder is the description)</li>
     *   <li>{@code @return} / {@code @returns} &rarr; {@code **Returns:** description}</li>
     *   <li>{@code @throws} / {@code @exception} &rarr; {@code - `ExceptionType` — description}</li>
     *   <li>{@code @see} &rarr; {@code **See:** content}</li>
     *   <li>{@code @deprecated} &rarr; {@code **Deprecated:** description}</li>
     * </ul>
     * Any other tag name is rendered generically as
     * {@code **tagName** content} so its name and content always survive
     * (Requirement 6.6 — no tag is silently dropped).
     *
     * <p>Inline tags embedded in {@code content} are always processed, never
     * emitted as raw tag syntax: {@code {@code x}} becomes {@code `x`} and
     * {@code {@link X}} becomes a readable reference. Literal prose in the
     * content is run through {@link #escape(String)}; inline-code contents are
     * emitted verbatim. The leading {@code @} of a tag name is stripped, so no
     * raw block-tag syntax (for example {@code @param}) ever appears in the
     * output.
     *
     * <p>The function is pure: deterministic, total, and free of I/O.
     * {@code null} arguments are treated as empty strings.
     *
     * @param tagName the JavaDoc tag name, with or without a leading {@code @}
     *                (for example {@code @param} or {@code param}); may be
     *                {@code null}
     * @param content the tag content (for {@code @param} and {@code @throws}
     *                this begins with the name/type token); may be {@code null}
     * @return the rendered Markdown fragment for the tag
     */
    public static String formatTag(String tagName, String content) {
        var name = normalizeTagName(tagName);
        return switch (name) {
            case "param" -> formatNamedListTag(content);
            case "throws", "exception" -> formatNamedListTag(content);
            case "return", "returns" -> "**Returns:** " + processInlineTags(content);
            case "see" -> "**See:** " + processInlineTags(content);
            case "deprecated" -> "**Deprecated:** " + processInlineTags(content);
            case "" -> processInlineTags(content);
            default -> "**" + escape(displayTagName(tagName)) + "** " + processInlineTags(content);
        };
    }

    /**
     * Renders the Agent Notes contract block for a method as a level-4
     * {@code #### Agent Notes} section, listing one bullet per present contract
     * section in the canonical fixed order, or an empty string when no contract
     * content is present.
     *
     * <p>The nine recognized contract sections are rendered, when present, in
     * this exact order with these exact labels (Requirement 7.2):
     * Preconditions, Postconditions, Side effects, Idempotency, Authorization,
     * Transactions, Concurrency, Thread-safety, Error handling. Each present
     * section is emitted as {@code - **Label:** <escaped content>}, where the
     * content is the verbatim text from the source comment run through
     * {@link #escape(String)} so it renders as literal prose and round-trips.
     *
     * <p>The renderer never invents content: a section appears in the output if
     * and only if it is present in {@code notes}. When {@link AgentNotes#isEmpty()}
     * is {@code true} (all nine absent), the whole section is omitted and an
     * empty string is returned (Requirement 7.4).
     *
     * <p>The function is pure: deterministic, total, and free of I/O. A
     * {@code null} {@code notes} argument is treated as an empty set of
     * contract sections and yields an empty string.
     *
     * <p>Example output:
     * <pre>
     * #### Agent Notes
     *
     * - **Preconditions:** id must be positive
     * - **Thread-safety:** safe for concurrent use
     * </pre>
     *
     * @param notes the extracted contract sections; may be {@code null}
     * @return the rendered Agent Notes Markdown section, or an empty string when
     *         no contract content is present
     */
    public static String renderAgentNotes(AgentNotes notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        var block = new StringBuilder("#### Agent Notes\n\n");
        appendContractBullet(block, "Preconditions", notes.preconditions());
        appendContractBullet(block, "Postconditions", notes.postconditions());
        appendContractBullet(block, "Side effects", notes.sideEffects());
        appendContractBullet(block, "Idempotency", notes.idempotency());
        appendContractBullet(block, "Authorization", notes.authorization());
        appendContractBullet(block, "Transactions", notes.transactions());
        appendContractBullet(block, "Concurrency", notes.concurrency());
        appendContractBullet(block, "Thread-safety", notes.threadSafety());
        appendContractBullet(block, "Error handling", notes.errorHandling());
        return block.toString();
    }

    /**
     * Appends a single contract bullet ({@code - **Label:** <escaped content>})
     * to {@code block} when {@code content} is present. The label is fixed
     * structural text emitted verbatim; the content is literal prose and is
     * escaped with {@link #escape(String)}.
     */
    private static void appendContractBullet(StringBuilder block, String label, Optional<String> content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        block.append("- **").append(label).append(":** ")
                .append(escape(content.get()))
                .append('\n');
    }

    /**
     * Renders a tag whose content leads with an identifier token (a parameter
     * name for {@code @param}, an exception type for {@code @throws}) as a
     * Markdown list item: the leading token as inline code, then the remaining
     * description after an em dash.
     */
    private static String formatNamedListTag(String content) {
        var trimmed = nullToEmpty(content).strip();
        if (trimmed.isEmpty()) {
            return "-";
        }
        var split = firstWhitespaceIndex(trimmed);
        var token = split < 0 ? trimmed : trimmed.substring(0, split);
        var description = split < 0 ? "" : trimmed.substring(split + 1).strip();
        var item = new StringBuilder("- `").append(token).append('`');
        if (!description.isEmpty()) {
            item.append(" — ").append(processInlineTags(description));
        }
        return item.toString();
    }

    /**
     * Converts inline JavaDoc tags ({@code {@code ...}}, {@code {@literal ...}},
     * {@code {@link ...}}, {@code {@linkplain ...}}) embedded in {@code text}
     * into Markdown, escaping the surrounding literal prose. Inline-code
     * contents are emitted verbatim; everything else is escaped. Any other
     * inline tag is replaced by its (escaped) body so no raw {@code {@...}}
     * syntax survives.
     */
    private static String processInlineTags(String text) {
        var source = nullToEmpty(text);
        if (source.isEmpty()) {
            return "";
        }
        var out = new StringBuilder(source.length() + 8);
        var index = 0;
        while (index < source.length()) {
            var open = source.indexOf("{@", index);
            if (open < 0) {
                out.append(escape(source.substring(index)));
                break;
            }
            out.append(escape(source.substring(index, open)));
            var close = source.indexOf('}', open);
            if (close < 0) {
                // Unterminated inline tag: treat the remainder as literal prose.
                out.append(escape(source.substring(open)));
                break;
            }
            var inner = source.substring(open + 2, close);
            var split = firstWhitespaceIndex(inner);
            var inlineName = split < 0 ? inner : inner.substring(0, split);
            var body = split < 0 ? "" : inner.substring(split + 1).strip();
            switch (inlineName.toLowerCase(Locale.ROOT)) {
                case "code", "literal" -> {
                    if (!body.isEmpty()) {
                        out.append('`').append(body).append('`');
                    }
                }
                case "link", "linkplain" -> out.append(formatLinkReference(body));
                default -> out.append(escape(body));
            }
            index = close + 1;
        }
        return out.toString();
    }

    /**
     * Renders the body of an inline {@code {@link}} / {@code {@linkplain}} tag
     * as a readable reference: when a label follows the reference it is shown as
     * escaped text, otherwise the reference itself is shown as inline code with
     * {@code #} normalized to {@code .} for readability.
     */
    private static String formatLinkReference(String body) {
        if (body.isEmpty()) {
            return "";
        }
        var split = firstWhitespaceIndex(body);
        var reference = split < 0 ? body : body.substring(0, split);
        var label = split < 0 ? "" : body.substring(split + 1).strip();
        if (!label.isEmpty()) {
            return escape(label);
        }
        return "`" + reference.replace('#', '.') + "`";
    }

    /**
     * Normalizes a tag name for matching: trims surrounding whitespace, drops a
     * single leading {@code @}, and lowercases the result. A {@code null} tag
     * name becomes the empty string.
     */
    private static String normalizeTagName(String tagName) {
        return stripLeadingAt(tagName).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the tag name for display (leading {@code @} removed, original
     * case preserved). Used for the generic rendering of unknown tags.
     */
    private static String displayTagName(String tagName) {
        return stripLeadingAt(tagName);
    }

    private static String stripLeadingAt(String tagName) {
        var trimmed = nullToEmpty(tagName).strip();
        return trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
    }

    /**
     * Returns the index of the first whitespace character in {@code value}, or
     * {@code -1} when the string contains no whitespace.
     */
    private static int firstWhitespaceIndex(String value) {
        for (var i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the supplied string, or an empty string when it is {@code null}.
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
