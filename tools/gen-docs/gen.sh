#! /bin/bash

set -e

ROOT=$(dirname "$0")

IN_MD="$1"
OUT_HTML="$2"

mkdir -p $(dirname "$2")

echo Converting "$IN_MD" to "$OUT_HTML"

{
   cat $ROOT/header.html
   pandoc --verbose --from=gfm --to=html "$IN_MD"
   cat $ROOT/footer.html
} >"$OUT_HTML"
