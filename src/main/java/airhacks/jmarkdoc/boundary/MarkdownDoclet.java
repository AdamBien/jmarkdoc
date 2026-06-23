package airhacks.jmarkdoc.boundary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import airhacks.jmarkdoc.control.DocReader;
import airhacks.jmarkdoc.control.ElementCollector;
import airhacks.jmarkdoc.control.MarkdownRenderer;
import airhacks.jmarkdoc.control.MarkdownWriter;
import airhacks.jmarkdoc.entity.AgentNotes;
import airhacks.jmarkdoc.entity.FieldDoc;
import airhacks.jmarkdoc.entity.MemberDoc;
import airhacks.jmarkdoc.entity.ParamDoc;
import airhacks.jmarkdoc.entity.ThrowsDoc;

/**
 * The jMarkDoc entry point: a {@link Doclet} that renders one Markdown file per
 * documented type, mirroring the package structure under the {@code --output}
 * directory.
 *
 * <p>This class owns only the doclet lifecycle and the orchestration of the
 * collaborators — discovery ({@link ElementCollector}), JavaDoc access
 * ({@link DocReader}), rendering ({@link MarkdownRenderer}), and writing
 * ({@link MarkdownWriter}). All formatting decisions live in the pure
 * {@code MarkdownRenderer}; all I/O lives in {@code MarkdownWriter}.
 */
public final class MarkdownDoclet implements Doclet {

    private final OutputOption outputOption = new OutputOption();
    private final ElementCollector collector = new ElementCollector();
    private Reporter reporter;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return Doclets.DOCLET_NAME;
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(this.outputOption);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        var docReader = new DocReader(environment.getDocTrees());
        var writer = new MarkdownWriter(this.outputOption.directory());
        var packages = environment.getElementUtils();
        try {
            for (var type : this.collector.includedTypes(environment)) {
                var packageName = packages.getPackageOf(type).getQualifiedName().toString();
                var markdown = renderType(type, docReader);
                writer.write(writer.pathFor(packageName, type.getSimpleName().toString()), markdown);
            }
            return true;
        } catch (IOException writing) {
            if (this.reporter != null) {
                this.reporter.print(javax.tools.Diagnostic.Kind.ERROR,
                        "jMarkDoc failed to write output: " + writing.getMessage());
            }
            return false;
        }
    }

    /**
     * Assembles the complete Markdown document for one type: the heading block,
     * the class-level description, and the member sections in a fixed order.
     */
    private String renderType(TypeElement type, DocReader docReader) {
        var qualifiedName = type.getQualifiedName().toString();
        var packageName = enclosingPackageName(type);
        var document = new StringBuilder();
        document.append(MarkdownRenderer.formatType(qualifiedName, packageName, kindOf(type)));

        var description = docReader.fullBody(type);
        if (!description.isEmpty()) {
            document.append('\n').append(MarkdownRenderer.escape(description)).append('\n');
        }

        appendFields(document, type, docReader);
        appendEnumConstants(document, type, docReader);
        appendRecordComponents(document, type, docReader);
        appendExecutables(document, "## Constructors", this.collector.constructors(type), type, docReader);
        appendExecutables(document, "## Methods", this.collector.methods(type), type, docReader);
        return document.toString();
    }

    private void appendFields(StringBuilder document, TypeElement type, DocReader docReader) {
        var fields = this.collector.fields(type);
        if (fields.isEmpty()) {
            return;
        }
        document.append("\n## Fields\n");
        for (var field : fields) {
            document.append('\n').append(MarkdownRenderer.formatField(toFieldDoc(field, docReader)));
        }
    }

    private void appendEnumConstants(StringBuilder document, TypeElement type, DocReader docReader) {
        var constants = this.collector.enumConstants(type);
        if (constants.isEmpty()) {
            return;
        }
        document.append("\n## Enum Constants\n");
        for (var constant : constants) {
            document.append('\n').append(MarkdownRenderer.formatField(toFieldDoc(constant, docReader)));
        }
    }

    private void appendRecordComponents(StringBuilder document, TypeElement type, DocReader docReader) {
        var components = this.collector.recordComponents(type);
        if (components.isEmpty()) {
            return;
        }
        // Record components are documented with @param tags on the record itself.
        var descriptions = namedTagDescriptions(docReader.blockTagsByName(type).get("param"));
        document.append("\n## Record Components\n");
        for (var component : components) {
            var name = component.getSimpleName().toString();
            var field = new FieldDoc(name, component.asType().toString(),
                    descriptions.getOrDefault(name, ""));
            document.append('\n').append(MarkdownRenderer.formatField(field));
        }
    }

    private void appendExecutables(StringBuilder document, String heading,
            List<ExecutableElement> executables, TypeElement type, DocReader docReader) {
        if (executables.isEmpty()) {
            return;
        }
        document.append('\n').append(heading).append('\n');
        for (var executable : executables) {
            document.append('\n').append(MarkdownRenderer.formatMethod(toMemberDoc(executable, type, docReader)));
        }
    }

    /**
     * Builds the {@link MemberDoc} for a constructor or method by combining its
     * signature (from the program model) with its documented parameters, return,
     * throws, and Agent Notes contract sections (from the JavaDoc comment).
     */
    private MemberDoc toMemberDoc(ExecutableElement executable, TypeElement type, DocReader docReader) {
        var isConstructor = executable.getKind() == ElementKind.CONSTRUCTOR;
        var name = isConstructor
                ? type.getSimpleName().toString()
                : executable.getSimpleName().toString();
        var returnType = isConstructor ? "" : executable.getReturnType().toString();

        var tags = docReader.blockTagsByName(executable);
        var paramDescriptions = namedTagDescriptions(tags.get("param"));
        var parameters = new ArrayList<ParamDoc>();
        for (var parameter : executable.getParameters()) {
            var parameterName = parameter.getSimpleName().toString();
            parameters.add(new ParamDoc(parameterName, parameter.asType().toString(),
                    paramDescriptions.getOrDefault(parameterName, "")));
        }

        var signature = MarkdownRenderer.formatSignature(modifiersOf(executable), returnType, name, parameters);
        var thrown = toThrowsDocs(tags);
        var deprecatedNote = firstTag(tags, "deprecated");
        return new MemberDoc(
                signature,
                docReader.fullBody(executable),
                parameters,
                firstTag(tags, "return"),
                thrown,
                tags.containsKey("deprecated"),
                deprecatedNote,
                agentNotesFrom(tags),
                tags);
    }

    private static FieldDoc toFieldDoc(VariableElement field, DocReader docReader) {
        return new FieldDoc(field.getSimpleName().toString(), field.asType().toString(),
                docReader.fullBody(field));
    }

    /**
     * Collects the {@code @throws}/{@code @exception} entries into ordered
     * {@link ThrowsDoc} values, splitting each entry into the leading exception
     * type and the remaining description.
     */
    private static List<ThrowsDoc> toThrowsDocs(Map<String, List<String>> tags) {
        var thrown = new ArrayList<ThrowsDoc>();
        for (var key : List.of("throws", "exception")) {
            for (var entry : tags.getOrDefault(key, List.of())) {
                var split = splitFirstToken(entry);
                thrown.add(new ThrowsDoc(split[0], split[1]));
            }
        }
        return thrown;
    }

    /**
     * Maps the nine custom contract tags to the fixed {@link AgentNotes} record,
     * leaving any section absent when its tag is not present.
     */
    private static AgentNotes agentNotesFrom(Map<String, List<String>> tags) {
        return new AgentNotes(
                firstTag(tags, "precondition"),
                firstTag(tags, "postcondition"),
                firstTag(tags, "sideeffect"),
                firstTag(tags, "idempotency"),
                firstTag(tags, "authorization"),
                firstTag(tags, "transactions"),
                firstTag(tags, "concurrency"),
                firstTag(tags, "threadsafety"),
                firstTag(tags, "errorhandling"));
    }

    /** Returns the declared modifiers in the canonical {@link Modifier} order. */
    private static List<String> modifiersOf(Element element) {
        var present = element.getModifiers();
        var ordered = new ArrayList<String>();
        for (var modifier : Modifier.values()) {
            if (present.contains(modifier)) {
                ordered.add(modifier.toString());
            }
        }
        return ordered;
    }

    /**
     * Builds a name &rarr; description map from {@code @param}-style entries,
     * each of which begins with the parameter/component name token.
     */
    private static Map<String, String> namedTagDescriptions(List<String> entries) {
        var descriptions = new java.util.LinkedHashMap<String, String>();
        if (entries != null) {
            for (var entry : entries) {
                var split = splitFirstToken(entry);
                descriptions.putIfAbsent(split[0], split[1]);
            }
        }
        return descriptions;
    }

    private static Optional<String> firstTag(Map<String, List<String>> tags, String name) {
        var values = tags.get(name);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        var first = values.getFirst();
        return first == null || first.isBlank() ? Optional.empty() : Optional.of(first.strip());
    }

    /** Splits an entry into its leading whitespace-delimited token and the rest. */
    private static String[] splitFirstToken(String entry) {
        var trimmed = entry == null ? "" : entry.strip();
        var space = -1;
        for (var i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                space = i;
                break;
            }
        }
        if (space < 0) {
            return new String[] { trimmed, "" };
        }
        return new String[] { trimmed.substring(0, space), trimmed.substring(space + 1).strip() };
    }

    private static String enclosingPackageName(TypeElement type) {
        var enclosing = type.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof PackageElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return enclosing instanceof PackageElement pkg ? pkg.getQualifiedName().toString() : "";
    }

    /** Maps a type's {@link ElementKind} to the lowercase keyword for the heading. */
    private static String kindOf(TypeElement type) {
        return switch (type.getKind()) {
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case RECORD -> "record";
            case ANNOTATION_TYPE -> "annotation";
            default -> "class";
        };
    }
}
