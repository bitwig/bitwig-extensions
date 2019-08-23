#! /bin/bash

set -e

ROOT=$PWD

cd doc-source

find . -name '*.md' -type f | sed -e "s#\(.*\).md#$ROOT/tools/gen-docs/gen.sh \"\1.md\" \"$ROOT/src/main/resources/Documentation/Controllers/\1.html\"#" | sh
