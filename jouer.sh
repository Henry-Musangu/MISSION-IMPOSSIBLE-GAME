#!/bin/sh
# Compile et lance Henry le Hérisson.
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out src/henry/*.java && java -cp out henry.Main
