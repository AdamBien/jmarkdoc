package airhacks.jmarkdoc.boundary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

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
///
/// The `javadoc` tool refuses to invoke a doclet when source analysis reports
/// errors — including the common case of types that reference dependencies
/// (Jakarta EE, JAX-RS, third-party libraries) that are not on the classpath.
/// To document such sources without their dependencies, this launcher falls
/// back to a source-only mode that drives the same doclet over the elements the
/// compiler can still model, ignoring unresolved-symbol errors.
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

        if (runDocumentationTool(sources, outputDir)) {
            IO.println("Markdown written to " + outputDir);
            return;
        }

        // The javadoc tool aborted before the doclet ran — almost always because
        // sources reference types (annotations, supertypes) not on the classpath.
        // Retry without resolving those references.
        IO.println("javadoc analysis failed (likely unresolved dependencies); "
                + "retrying in source-only mode...");
        if (runSourceOnly(sources, outputDir)) {
            IO.println("Markdown written to " + outputDir + " (source-only mode)");
            return;
        }
        IO.println("documentation generation failed");
        System.exit(1);
    }

    /// Runs the standard `javadoc` tool with the doclet baked in. This is the
    /// primary path: when the sources fully resolve, it produces the canonical
    /// output. Returns `false` (without writing) when analysis errors stop the
    /// tool from invoking the doclet. Progress and analysis diagnostics are
    /// suppressed — when this path fails the source-only fallback takes over, so
    /// the unresolved-symbol noise would only mislead.
    private static boolean runDocumentationTool(List<Path> sources, Path outputDir) throws IOException {
        var tool = ToolProvider.getSystemDocumentationTool();
        DiagnosticListener<JavaFileObject> ignoreErrors = diagnostic -> { };
        try (var files = tool.getStandardFileManager(null, null, null);
                var discard = java.io.Writer.nullWriter()) {
            var units = files.getJavaFileObjectsFromPaths(sources);
            var task = tool.getTask(discard, files, ignoreErrors, MarkdownDoclet.class,
                    List.of("--output", outputDir.toString()), units);
            return task.call();
        }
    }

    /// Source-only fallback: compiles the sources with the system Java compiler,
    /// swallowing unresolved-symbol errors, then drives {@link MarkdownDoclet}
    /// directly over the {@link TypeElement}s the compiler can still model. The
    /// doclet renders signatures and JavaDoc as text and never resolves the
    /// missing types, so the absent dependencies do not affect the output.
    private static boolean runSourceOnly(List<Path> sources, Path outputDir) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, null)) {
            var units = files.getJavaFileObjectsFromPaths(sources);
            DiagnosticListener<JavaFileObject> ignoreErrors = diagnostic -> { };
            var task = (JavacTask) compiler.getTask(null, files, ignoreErrors,
                    List.of("-proc:none"), null, units);
            var docTrees = DocTrees.instance(task);

            var compilationUnits = task.parse();
            // Attribute as far as possible; unresolved references throw nothing,
            // they surface as (ignored) diagnostics and error types.
            task.analyze();

            var includedTypes = collectTypes(compilationUnits, docTrees);
            if (includedTypes.isEmpty()) {
                return false;
            }

            var environment = docletEnvironment(task, docTrees, files, includedTypes);
            var doclet = new MarkdownDoclet();
            doclet.init(Locale.getDefault(), silentReporter());
            applyOutput(doclet, outputDir);
            return doclet.run(environment);
        }
    }

    /// Walks every parsed compilation unit and resolves the {@link TypeElement}
    /// for each declared (top-level and nested) type, so nested types are
    /// documented in their own files exactly as the javadoc tool would.
    private static Set<TypeElement> collectTypes(Iterable<? extends com.sun.source.tree.CompilationUnitTree> units,
            DocTrees docTrees) {
        var types = new LinkedHashSet<TypeElement>();
        var scanner = new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void unused) {
                if (docTrees.getElement(getCurrentPath()) instanceof TypeElement type) {
                    types.add(type);
                }
                return super.visitClass(node, unused);
            }
        };
        for (var unit : units) {
            scanner.scan(unit, null);
        }
        return types;
    }

    /// Feeds the `--output` directory to the doclet through its public option,
    /// the same way the javadoc tool would during command-line processing.
    private static void applyOutput(MarkdownDoclet doclet, Path outputDir) {
        for (var option : doclet.getSupportedOptions()) {
            if (option.getNames().contains("--output")) {
                option.process("--output", List.of(outputDir.toString()));
            }
        }
    }

    /// Builds a minimal {@link DocletEnvironment} over the compiler model. Only
    /// the accessors the doclet actually uses carry data — included types, the
    /// element and type utilities, and the doc-comment trees; the rest return
    /// the most permissive sensible defaults.
    private static DocletEnvironment docletEnvironment(JavacTask task, DocTrees docTrees,
            JavaFileManager fileManager, Set<TypeElement> includedTypes) {
        var elementUtils = task.getElements();
        var typeUtils = task.getTypes();
        return new DocletEnvironment() {
            @Override
            public Set<? extends Element> getSpecifiedElements() {
                return includedTypes;
            }

            @Override
            public Set<? extends Element> getIncludedElements() {
                return includedTypes;
            }

            @Override
            public DocTrees getDocTrees() {
                return docTrees;
            }

            @Override
            public Elements getElementUtils() {
                return elementUtils;
            }

            @Override
            public Types getTypeUtils() {
                return typeUtils;
            }

            @Override
            public boolean isIncluded(Element element) {
                return includedTypes.contains(element);
            }

            @Override
            public boolean isSelected(Element element) {
                return true;
            }

            @Override
            public JavaFileManager getJavaFileManager() {
                return fileManager;
            }

            @Override
            public SourceVersion getSourceVersion() {
                return SourceVersion.latest();
            }

            @Override
            public ModuleMode getModuleMode() {
                return ModuleMode.ALL;
            }

            @Override
            public JavaFileObject.Kind getFileKind(TypeElement type) {
                return JavaFileObject.Kind.SOURCE;
            }
        };
    }

    /// A {@link Reporter} that forwards only errors to standard error; the
    /// doclet reports through it solely on output-write failures.
    private static Reporter silentReporter() {
        return new Reporter() {
            @Override
            public void print(Diagnostic.Kind kind, String message) {
                if (kind == Diagnostic.Kind.ERROR) {
                    IO.println(message);
                }
            }

            @Override
            public void print(Diagnostic.Kind kind, DocTreePath path, String message) {
                print(kind, message);
            }

            @Override
            public void print(Diagnostic.Kind kind, Element element, String message) {
                print(kind, message);
            }
        };
    }
}
