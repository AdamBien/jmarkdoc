/**
 * jMarkDoc is a custom JavaDoc Doclet for Java 25 that generates clean Markdown
 * API documentation directly from Java source and JavaDoc comments.
 *
 * <p>The module depends only on the JDK: {@code jdk.javadoc} for the Doclet SPI
 * and doc-tree access, and {@code jdk.compiler} for {@code com.sun.source}.
 */
module jmarkdoc {
    requires jdk.javadoc;
    requires jdk.compiler;

    exports airhacks.jmarkdoc;
}
