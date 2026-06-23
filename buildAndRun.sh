#!/usr/bin/env bash
#
# buildAndRun.sh - Build the jMarkDoc executable and document the example.
#
# Builds zbo/jmarkdoc.jar with zb (Zero Dependencies Builder), then runs that
# jar against the bundled example sources, writing Markdown into
# target/site/apidocs.
#
# Requires a clean JDK 25 installation and zb (https://github.com/AdamBien/zb)
# on the PATH.

set -euo pipefail

# Work from the project root so the script runs from any directory.
cd "$(dirname "${BASH_SOURCE[0]}")"

EXAMPLE_DIR="src/example/java"
OUTPUT_DIR="target/site/apidocs"

echo "==> Building zbo/jmarkdoc.jar with zb"
rm -rf target
zb.sh

echo "==> Documenting $EXAMPLE_DIR into $OUTPUT_DIR"
java -jar zbo/jmarkdoc.jar "$EXAMPLE_DIR" "$OUTPUT_DIR"

echo "==> Generated Markdown files:"
find "$OUTPUT_DIR" -name '*.md' -print
