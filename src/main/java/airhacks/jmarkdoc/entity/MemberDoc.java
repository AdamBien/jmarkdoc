package airhacks.jmarkdoc.entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import airhacks.jmarkdoc.control.MarkdownRenderer;

/**
 * A single documented method or constructor, after extraction from the program
 * model and its JavaDoc comment tree.
 *
 * <p>This is a plain immutable value type with no behavior, used by the pure
 * rendering helper {@link MarkdownRenderer#formatMethod(MemberDoc)} so it can be
 * exercised in isolation without invoking the compiler. The lifecycle layer
 * (the doclet's {@code run}) assembles a {@code MemberDoc} from the
 * {@code javax.lang.model} element and its {@code DocCommentTree}.
 *
 * @param signature   the rendered signature text (as produced by
 *                    {@link MarkdownRenderer#formatSignature}); rendered inside
 *                    an inline-code span and therefore not escaped
 * @param description the JavaDoc body text, or an empty string when the member
 *                    has no documentation
 * @param params      the {@code @param} entries in declaration order; may be
 *                    empty
 * @param returns     the {@code @return} text, if present
 * @param thrown      the {@code @throws} / {@code @exception} entries; may be
 *                    empty
 * @param deprecated  whether the member carries a {@code @deprecated} tag
 * @param deprecatedNote the {@code @deprecated} description, if present
 * @param agentNotes  the extracted Agent Notes contract sections
 * @param customTags  unknown/custom block tags, grouped by tag name
 */
public record MemberDoc(
        String signature,
        String description,
        List<ParamDoc> params,
        Optional<String> returns,
        List<ThrowsDoc> thrown,
        boolean deprecated,
        Optional<String> deprecatedNote,
        AgentNotes agentNotes,
        Map<String, List<String>> customTags) {
}
