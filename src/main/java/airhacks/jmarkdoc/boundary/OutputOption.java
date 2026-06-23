package airhacks.jmarkdoc.boundary;

import java.nio.file.Path;
import java.util.List;

import jdk.javadoc.doclet.Doclet;

/**
 * Models the {@code --output <directory>} command-line option of the Doclet.
 *
 * <p>The option accepts exactly one argument: the directory into which the
 * generated Markdown files are written. When the option is omitted, the
 * Doclet writes to the current directory ({@code "."}).
 */
public final class OutputOption implements Doclet.Option {

    private Path directory = Path.of(".");

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "Output directory for Markdown";
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("--output");
    }

    @Override
    public String getParameters() {
        return "<directory>";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }
        var argument = arguments.getFirst();
        if (argument == null || argument.isBlank()) {
            return false;
        }
        this.directory = Path.of(argument);
        return true;
    }

    /**
     * Returns the parsed output directory.
     *
     * @return the configured output directory, defaulting to {@code "."}
     */
    public Path directory() {
        return this.directory;
    }
}
