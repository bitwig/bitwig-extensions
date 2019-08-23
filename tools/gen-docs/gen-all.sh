#! /bin/bash

set -e

ROOT=$PWD

cd resources/doc-source

find . -name '*.md' -type f | sed -e "s#\(.*\).md#$ROOT/tools/gen-docs/gen.sh \"\1.md\" \"../doc/Controllers/\1.html\"#" | sh
