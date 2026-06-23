#!/usr/bin/env bash
#
# buildAndRun.sh - Zero-dependency build for jMarkDoc.
#
# Compiles the doclet into target/classes, packages it into build/jmarkdoc.jar,
# and runs the javadoc tool with the custom MarkdownDoclet against the example
# sources, writing generated Markdown into target/api-md.
#
# Requires a clean JDK 25 installation. No Maven, Gradle, Ant, or third-party
# libraries are used.

set -euo pipefail

# Work from the project root so the script runs from any directory.
cd "$(dirname "${BASH_SOURCE[0]}")"

CLASSES_DIR="target/classes"
JAR_FILE="build/jmarkdoc.jar"
OUTPUT_DIR="target/api-md"
DOCLET_CLASS="airhacks.jmarkdoc.MarkdownDoclet"

echo "==> Cleaning build directory"
rm -rf build target
mkdir -p "$CLASSES_DIR" "$OUTPUT_DIR" "$(dirname "$JAR_FILE")"

echo "==> Compiling sources"
javac --release 25 --add-modules jdk.javadoc -d "$CLASSES_DIR" $(find src/main/java -name '*.java')

echo "==> Packaging $JAR_FILE"
jar --create --file "$JAR_FILE" -C "$CLASSES_DIR" .

echo "==> Generating Markdown into $OUTPUT_DIR"
javadoc \
    -doclet "$DOCLET_CLASS" \
    -docletpath "$JAR_FILE" \
    --output "$OUTPUT_DIR" \
    $(find src/example/java -name '*.java')

echo "==> Generated Markdown files:"
find "$OUTPUT_DIR" -name '*.md' -print

echo "==> Build complete"
