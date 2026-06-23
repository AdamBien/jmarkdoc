package airhacks.jmarkdoc.control;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Computes the mirrored output path for a documented type and persists the
 * rendered Markdown to disk.
 *
 * <p>The package structure is mirrored into the directory layout: each segment
 * of the package name becomes one directory level rooted at the configured
 * output directory, and the file is named {@code <typeName>.md}. For example,
 * an output root of {@code build/api-md}, package {@code com.example.api}, and
 * type {@code UserService} produce
 * {@code build/api-md/com/example/api/UserService.md}.
 *
 * <p>{@link #pathFor(String, String)} is pure and deterministic: it performs no
 * I/O and depends only on its arguments and the configured output root. Path
 * segments are joined via {@link Path#resolve(String)} so the layout uses the
 * platform file separator and remains portable.
 */
public final class MarkdownWriter {

    private final Path outputRoot;

    /**
     * Creates a writer rooted at the supplied output directory.
     *
     * @param outputRoot the directory under which the mirrored package tree and
     *                   Markdown files are written
     */
    public MarkdownWriter(Path outputRoot) {
        this.outputRoot = outputRoot;
    }

    /**
     * Computes the relative Markdown file path for a type, mirroring its package
     * structure under the configured output root.
     *
     * <p>This method is pure and deterministic: it performs no I/O. The default
     * (empty) package yields no subdirectories, so the file is placed directly
     * under the output root.
     *
     * @param packageName the fully qualified package name (for example
     *                    {@code com.example.api}), or an empty string for the
     *                    default package
     * @param typeName    the simple type name (for example {@code UserService})
     * @return the output path {@code outputRoot/<package segments>/<typeName>.md}
     */
    public Path pathFor(String packageName, String typeName) {
        var directory = this.outputRoot;
        if (packageName != null && !packageName.isEmpty()) {
            for (var segment : packageName.split("\\.")) {
                directory = directory.resolve(segment);
            }
        }
        return directory.resolve(typeName + ".md");
    }

    /**
     * Writes the Markdown content to the given path as UTF-8, creating any
     * missing parent directories first.
     *
     * @param path     the destination file path (typically from
     *                 {@link #pathFor(String, String)})
     * @param markdown the Markdown content to write
     * @throws IOException if creating the parent directories or writing the file
     *                     fails
     */
    public void write(Path path, String markdown) throws IOException {
        var parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, markdown, StandardCharsets.UTF_8);
    }
}
