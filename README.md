# jMarkDoc

A custom JavaDoc doclet that generates clean Markdown API docs from Java source and comments. Built for agent-ready context, RAG pipelines, and developer portals, it preserves signatures, params, returns, throws, packages, and class structure.

## Requirements

- JDK 25+ (the doclet uses the `jdk.javadoc` and `jdk.compiler` modules)
- No Maven, Gradle, Ant, or third-party libraries — plain `javac`, `jar`, and `javadoc`

## Build and Generate

The supported entry point is `buildAndRun.sh`. It cleans `build/` and `target/`, compiles the doclet, packages `build/jmarkdoc.jar`, then runs `javadoc` with the doclet against the example sources and writes Markdown into `target/api-md`:

```bash
./buildAndRun.sh
```

On success it prints the paths of the generated `*.md` files.

## Executable JAR

A doclet is not directly runnable — it is a callback the `javadoc` tool loads and invokes. `airhacks.jmarkdoc.Main` wraps it in a launcher that runs the documentation tool in-process with the doclet baked in, so no doclet arguments are needed. Build the executable JAR with [zb](https://github.com/AdamBien/zb):

```bash
zb.sh
```

This produces `zbo/jmarkdoc.jar`. Run it with no arguments to document `src/main/java` into `target/site/apidocs` (the default Maven JavaDoc output directory):

```bash
java -jar zbo/jmarkdoc.jar
```

The source and output directories are optional positional arguments:

```bash
java -jar zbo/jmarkdoc.jar src/example/java target/example-md
```

## Run Against Your Own Sources

Point the `javadoc` tool at the packaged doclet and your sources:

```bash
javadoc \
  -doclet airhacks.jmarkdoc.MarkdownDoclet \
  -docletpath build/jmarkdoc.jar \
  --output target/api-md \
  $(find src/main/java -name '*.java')
```

### Options

| Option | Argument | Description |
| --- | --- | --- |
| `--output` | `<directory>` | Directory for the generated Markdown. Defaults to the current directory (`.`). |

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
