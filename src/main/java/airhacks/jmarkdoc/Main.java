package airhacks.jmarkdoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

/// Executable entry point that runs the {@link MarkdownDoclet} in-process.
///
/// A doclet is not itself runnable — it is a callback the `javadoc` tool loads
/// and invokes. This launcher invokes the system documentation tool
/// programmatically with the doclet baked in, so the whole conversion runs with
/// `java -jar zbo/jmarkdoc.jar` and no doclet arguments.
///
/// Both paths are optional positional arguments:
///
/// ```
/// java -jar zbo/jmarkdoc.jar [sourceDir] [outputDir]
/// ```
///
/// defaulting to `src/main/java` and `target/site/apidocs`.
public interface Main {

    String DEFAULT_SOURCES = "src/main/java";
    String DEFAULT_OUTPUT = "target/site/apidocs";

    static void main(String... args) throws IOException {
        var sourceDir = Path.of(args.length > 0 ? args[0] : DEFAULT_SOURCES);
        var outputDir = Path.of(args.length > 1 ? args[1] : DEFAULT_OUTPUT);

        if (!Files.isDirectory(sourceDir)) {
            IO.println("source directory not found: " + sourceDir);
            System.exit(1);
        }

        List<Path> sources;
        try (var tree = Files.walk(sourceDir)) {
            sources = tree.filter(path -> path.toString().endsWith(".java")).toList();
        }
        if (sources.isEmpty()) {
            IO.println("no .java sources found under " + sourceDir);
            System.exit(1);
        }

        Files.createDirectories(outputDir);

        var tool = ToolProvider.getSystemDocumentationTool();
        try (var files = tool.getStandardFileManager(null, null, null)) {
            var units = files.getJavaFileObjectsFromPaths(sources);
            var task = tool.getTask(null, files, null, MarkdownDoclet.class,
                    List.of("--output", outputDir.toString()), units);
            if (!task.call()) {
                IO.println("documentation generation failed");
                System.exit(1);
            }
        }
        IO.println("Markdown written to " + outputDir);
    }
}
