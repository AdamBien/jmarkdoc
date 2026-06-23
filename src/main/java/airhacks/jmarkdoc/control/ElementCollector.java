package airhacks.jmarkdoc.control;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import jdk.javadoc.doclet.DocletEnvironment;

/**
 * Discovers and orders the program elements to document.
 *
 * <p>Given a {@link DocletEnvironment}, this collector selects the included
 * {@link TypeElement}s and {@link PackageElement}s and extracts the members of a
 * type ({@link ExecutableElement constructors and methods}, {@link VariableElement
 * fields and enum constants}, and {@link RecordComponentElement record
 * components}) using {@link ElementKind} and {@link ElementFilter}.
 *
 * <p>Every returned list is sorted by a stable key so the documentation set is
 * <em>deterministic</em>: types and packages are ordered by their fully
 * qualified name, members by a stable signature/name string. This guarantees
 * byte-identical output across runs regardless of the iteration order of the
 * underlying model (Requirement 10.2, 10.3).
 *
 * <p>This class performs no I/O and holds no mutable state.
 */
public final class ElementCollector {

    /** Orders types by their fully qualified name. */
    private static final Comparator<TypeElement> BY_QUALIFIED_NAME =
            Comparator.comparing(type -> type.getQualifiedName().toString());

    /** Orders packages by their fully qualified name. */
    private static final Comparator<PackageElement> BY_PACKAGE_NAME =
            Comparator.comparing(pkg -> pkg.getQualifiedName().toString());

    /** Orders executable members by a stable signature string. */
    private static final Comparator<ExecutableElement> BY_SIGNATURE =
            Comparator.comparing(ElementCollector::signatureOf);

    /** Orders variable members (fields, enum constants) by their simple name. */
    private static final Comparator<VariableElement> BY_VARIABLE_NAME =
            Comparator.comparing(variable -> variable.getSimpleName().toString());

    /** Orders record components by their simple name. */
    private static final Comparator<RecordComponentElement> BY_COMPONENT_NAME =
            Comparator.comparing(component -> component.getSimpleName().toString());

    /**
     * Returns the included types (top-level and nested) in deterministic order,
     * sorted by fully qualified name.
     *
     * @param env the doclet environment; must not be {@code null}
     * @return the included {@link TypeElement}s, sorted by qualified name
     */
    public List<TypeElement> includedTypes(DocletEnvironment env) {
        return ElementFilter.typesIn(env.getIncludedElements()).stream()
                .sorted(BY_QUALIFIED_NAME)
                .collect(Collectors.toList());
    }

    /**
     * Returns the included packages in deterministic order, sorted by fully
     * qualified name.
     *
     * @param env the doclet environment; must not be {@code null}
     * @return the included {@link PackageElement}s, sorted by qualified name
     */
    public List<PackageElement> includedPackages(DocletEnvironment env) {
        return ElementFilter.packagesIn(env.getIncludedElements()).stream()
                .sorted(BY_PACKAGE_NAME)
                .collect(Collectors.toList());
    }

    /**
     * Returns the constructors declared by the given type in deterministic
     * order, sorted by a stable signature string.
     *
     * @param type the enclosing type; must not be {@code null}
     * @return the constructors of {@code type}, sorted by signature
     */
    public List<ExecutableElement> constructors(TypeElement type) {
        return ElementFilter.constructorsIn(type.getEnclosedElements()).stream()
                .sorted(BY_SIGNATURE)
                .collect(Collectors.toList());
    }

    /**
     * Returns the methods declared by the given type in deterministic order,
     * sorted by a stable signature string.
     *
     * @param type the enclosing type; must not be {@code null}
     * @return the methods of {@code type}, sorted by signature
     */
    public List<ExecutableElement> methods(TypeElement type) {
        return ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                .sorted(BY_SIGNATURE)
                .collect(Collectors.toList());
    }

    /**
     * Returns the fields declared by the given type, <em>excluding</em> enum
     * constants, in deterministic order sorted by simple name.
     *
     * @param type the enclosing type; must not be {@code null}
     * @return the non-enum-constant fields of {@code type}, sorted by name
     */
    public List<VariableElement> fields(TypeElement type) {
        return ElementFilter.fieldsIn(type.getEnclosedElements()).stream()
                .filter(field -> field.getKind() != ElementKind.ENUM_CONSTANT)
                .sorted(BY_VARIABLE_NAME)
                .collect(Collectors.toList());
    }

    /**
     * Returns the enum constants declared by the given type in deterministic
     * order, sorted by simple name. The result is empty for non-enum types.
     *
     * @param type the enclosing type; must not be {@code null}
     * @return the enum constants of {@code type}, sorted by name
     */
    public List<VariableElement> enumConstants(TypeElement type) {
        return ElementFilter.fieldsIn(type.getEnclosedElements()).stream()
                .filter(field -> field.getKind() == ElementKind.ENUM_CONSTANT)
                .sorted(BY_VARIABLE_NAME)
                .collect(Collectors.toList());
    }

    /**
     * Returns the record components of the given type in deterministic order,
     * sorted by simple name. The result is empty for non-record types.
     *
     * @param type the enclosing type; must not be {@code null}
     * @return the record components of {@code type}, sorted by name
     */
    public List<RecordComponentElement> recordComponents(TypeElement type) {
        return type.getRecordComponents().stream()
                .sorted(BY_COMPONENT_NAME)
                .collect(Collectors.toList());
    }

    /**
     * Builds a stable signature string for an executable member: the simple
     * name followed by the parenthesized, comma-separated list of erased
     * parameter type strings. This key is independent of model iteration order,
     * so sorting by it yields deterministic member ordering even when two
     * members share a name (overloads).
     *
     * @param executable the constructor or method
     * @return a deterministic signature key
     */
    private static String signatureOf(ExecutableElement executable) {
        var parameters = executable.getParameters().stream()
                .map(parameter -> parameter.asType().toString())
                .collect(Collectors.joining(","));
        return executable.getSimpleName().toString() + "(" + parameters + ")";
    }
}
