/**
 * Generates Markdown API documentation from Java source via the standard
 * {@code javadoc} tool.
 *
 * <p>The component plugs into the {@code jdk.javadoc.doclet.Doclet} SPI and
 * emits one Markdown file per type, laid out in a directory tree that mirrors
 * the package structure. The doc-comment model is kept independent of the
 * traversal and rendering logic so that the supported tag set and the Markdown
 * output format can evolve separately.
 */
package airhacks.jmarkdoc;
