package airhacks.jmarkdoc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;

/**
 * Safe, null-tolerant access to the JavaDoc comment tree of a program element.
 *
 * <p>{@code DocReader} is the single choke point through which the rendering
 * layer reads JavaDoc. Every accessor is null-safe by construction: an element
 * with no JavaDoc, or a partial/missing comment tree, yields an empty result
 * ({@link Optional#empty()}, {@code ""}, an empty list, or an empty map) rather
 * than raising a {@link NullPointerException}. This guarantees that rendering
 * is robust to missing documentation (Requirement 11.1).
 */
final class DocReader {

    private final DocTrees docTrees;

    /**
     * Creates a reader over the supplied doc-tree facility.
     *
     * @param docTrees the {@link DocTrees} obtained from the
     *                 {@code DocletEnvironment}; may be {@code null}, in which
     *                 case every accessor returns an empty result
     */
    public DocReader(DocTrees docTrees) {
        this.docTrees = docTrees;
    }

    /**
     * Returns the comment tree of the supplied element, if present.
     *
     * <p>Never throws on a missing or unreadable comment: a {@code null}
     * element, an absent {@link DocTrees}, an element with no JavaDoc, or any
     * failure while resolving the comment tree all yield
     * {@link Optional#empty()}.
     *
     * @param element the element whose JavaDoc is requested; may be {@code null}
     * @return the comment tree wrapped in an {@link Optional}, or
     *         {@link Optional#empty()} when no comment is available
     */
    public Optional<DocCommentTree> commentTree(Element element) {
        if (this.docTrees == null || element == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(this.docTrees.getDocCommentTree(element));
        } catch (RuntimeException reading) {
            return Optional.empty();
        }
    }

    /**
     * Returns the full body text of the element's comment.
     *
     * @param element the element whose JavaDoc body is requested; may be
     *                {@code null}
     * @return the full body text, or {@code ""} when no comment is present
     */
    public String fullBody(Element element) {
        return commentTree(element)
                .map(tree -> textOf(tree.getFullBody()))
                .orElse("");
    }

    /**
     * Returns the block tags of the element's comment.
     *
     * @param element the element whose block tags are requested; may be
     *                {@code null}
     * @return the block tags, or an empty list when no comment is present
     */
    public List<? extends DocTree> blockTags(Element element) {
        return commentTree(element)
                .<List<? extends DocTree>>map(DocCommentTree::getBlockTags)
                .orElseGet(List::of);
    }

    /**
     * Groups the element's block tags by their tag name, preserving the source
     * order of occurrences within each name.
     *
     * <p>Known tags are keyed by their canonical name ({@code param},
     * {@code return}, {@code throws}, {@code exception}, {@code deprecated},
     * {@code see}, {@code since}, ...) and unknown/custom tags by the literal
     * name written in the source. The value for each name is the list of
     * rendered tag contents, one entry per occurrence, so the renderer can
     * handle known tags specifically and unknown ones generically
     * (Requirement 6.6).
     *
     * @param element the element whose block tags are requested; may be
     *                {@code null}
     * @return a map from tag name to the ordered list of contents, or an empty
     *         map when no comment is present
     */
    public Map<String, List<String>> blockTagsByName(Element element) {
        var grouped = new LinkedHashMap<String, List<String>>();
        for (var tag : blockTags(element)) {
            if (tag == null) {
                continue;
            }
            var name = tagName(tag);
            grouped.computeIfAbsent(name, ignored -> new ArrayList<>()).add(tagContent(tag));
        }
        return grouped;
    }

    /**
     * Derives the tag name of a block tag, using the literal source name for
     * unknown/custom tags and the canonical kind name for known tags.
     */
    private static String tagName(DocTree tag) {
        if (tag instanceof UnknownBlockTagTree unknown) {
            return unknown.getTagName();
        }
        var kind = tag.getKind();
        if (kind != null && kind.tagName != null) {
            return kind.tagName;
        }
        return kind == null ? "" : kind.name().toLowerCase();
    }

    /**
     * Extracts the textual content of a block tag, omitting the leading
     * {@code @name} marker so callers receive only the description.
     */
    private static String tagContent(DocTree tag) {
        return switch (tag) {
            case ParamTree param -> joinName(param.getName(), textOf(param.getDescription()));
            case ReturnTree ret -> textOf(ret.getDescription());
            case ThrowsTree thrown -> joinName(thrown.getExceptionName(), textOf(thrown.getDescription()));
            case DeprecatedTree deprecated -> textOf(deprecated.getBody());
            case SeeTree see -> textOf(see.getReference());
            case SinceTree since -> textOf(since.getBody());
            case UnknownBlockTagTree unknown -> textOf(unknown.getContent());
            default -> tag.toString();
        };
    }

    private static String joinName(Object name, String description) {
        var prefix = name == null ? "" : name.toString().trim();
        if (prefix.isEmpty()) {
            return description;
        }
        return description.isEmpty() ? prefix : prefix + " " + description;
    }

    /**
     * Renders a list of doc-tree fragments to plain text, tolerating a
     * {@code null} or empty list.
     */
    private static String textOf(List<? extends DocTree> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            return "";
        }
        var text = new StringBuilder();
        for (var fragment : fragments) {
            if (fragment != null) {
                text.append(fragment.toString());
            }
        }
        return text.toString().trim();
    }
}
