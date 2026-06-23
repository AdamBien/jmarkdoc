# jMarkDoc

A custom JavaDoc doclet that generates clean Markdown API docs from Java source and comments. Built for agent-ready context, RAG pipelines, and developer portals, it preserves signatures, params, returns, throws, packages, and class structure.

## Requirements

- JDK 25+ (the doclet uses the `jdk.javadoc` and `jdk.compiler` modules)
- [zb](https://github.com/AdamBien/zb) on the `PATH` to build the executable JAR — no Maven, Gradle, Ant, or third-party runtime libraries

## Build and Generate

Build the executable JAR with [zb](https://github.com/AdamBien/zb):

```bash
zb.sh
```

This compiles the sources into `zbo/jmarkdoc.jar`. A doclet is not directly runnable — it is a callback the `javadoc` tool loads and invokes — so `airhacks.jmarkdoc.boundary.Main` wraps it in a launcher that runs the documentation tool in-process with the doclet baked in. Run the JAR with no arguments to document `src/main/java` into `target/site/apidocs` (the default Maven JavaDoc output directory):

```bash
java -jar zbo/jmarkdoc.jar
```

The source and output directories are optional positional arguments:

```bash
java -jar zbo/jmarkdoc.jar src/example/java target/example-md
```

The `buildAndRun.sh` convenience script chains both steps — it builds with `zb`, runs the JAR against the bundled example sources, and prints the paths of the generated `*.md` files:

```bash
./buildAndRun.sh
```

For day-to-day use from the project root, the single-file `jmarkdoc` script (Java 25 source-file mode) wraps the built JAR and takes the same optional arguments:

```bash
./jmarkdoc                                # src/main/java -> target/site/apidocs
./jmarkdoc src/example/java target/example-md
./jmarkdoc -help
```

It resolves the doclet from `zbo/jmarkdoc.jar` on its shebang classpath, so run `zb.sh` first and invoke it from the project root.

## Run Against Your Own Sources

The same `zbo/jmarkdoc.jar` doubles as a doclet JAR. Point the `javadoc` tool at it and your sources — for example to plug jMarkDoc into an existing `javadoc` or Maven build:

```bash
javadoc \
  -doclet airhacks.jmarkdoc.boundary.MarkdownDoclet \
  -docletpath zbo/jmarkdoc.jar \
  --output target/site/apidocs \
  $(find src/main/java -name '*.java')
```

### Options

| Option | Argument | Description |
| --- | --- | --- |
| `--output` | `<directory>` | Directory for the generated Markdown. Defaults to the current directory (`.`). |

### Maven

Plug the same JAR into the `maven-javadoc-plugin` as a custom doclet. Point `docletPath` at `zbo/jmarkdoc.jar`, disable the standard doclet options (they do not apply to this doclet), and pass `--output` as an additional option:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <configuration>
    <doclet>airhacks.jmarkdoc.boundary.MarkdownDoclet</doclet>
    <docletPath>${project.basedir}/zbo/jmarkdoc.jar</docletPath>
    <useStandardDocletOptions>false</useStandardDocletOptions>
    <additionalOptions>
      <additionalOption>--output</additionalOption>
      <additionalOption>${project.build.directory}/site/apidocs</additionalOption>
    </additionalOptions>
  </configuration>
</plugin>
```

Then generate the Markdown with:

```bash
mvn javadoc:javadoc
```

## Documenting Your Sources

Write doc comments in the modern Markdown JavaDoc syntax (`///`, JDK 23+ / JEP 467). Standard block tags and the custom contract tags work the same as in classic `/** */` comments:

```java
/// Creates a new user account.
///
/// Backed by an in-memory store in this example, but the contract is written
/// so a database-backed implementation could replace it unchanged.
///
/// @param email the account email; must be unique and non-blank
/// @return the created [User]
/// @throws IllegalArgumentException if `email` is blank
/// @precondition  `email` is non-null and not already registered
/// @postcondition a new [User] is persisted and returned
/// @idempotency   not idempotent — repeated calls create distinct accounts
/// @threadsafety  safe for concurrent callers; the store is synchronized
public User create(String email) {
    ...
}
```

## Agent Notes

Beyond standard JavaDoc (`@param`, `@return`, `@throws`, `@see`, ...), jMarkDoc renders an **Agent Notes** block from nine custom contract tags. Each section appears only when the corresponding tag is present in the source — the renderer never invents content:

| Tag | Section |
| --- | --- |
| `@precondition` | Preconditions |
| `@postcondition` | Postconditions |
| `@sideeffect` | Side effects |
| `@idempotency` | Idempotency |
| `@authorization` | Authorization |
| `@transactions` | Transactions |
| `@concurrency` | Concurrency |
| `@threadsafety` | Thread-safety |
| `@errorhandling` | Error handling |

See `src/example/java/UserService.java` for a source file that exercises the full range of supported tags.

## Tests

The doclet itself stays JDK-only. Tests are test-scope only (jqwik + JUnit Platform, vendored under `lib/`) and never bundled into `jmarkdoc.jar`:

```bash
./run-tests.sh
```
