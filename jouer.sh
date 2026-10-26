#!/bin/sh
# Compile et lance Henry le Hérisson (version PC).
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out core/src/main/java/henry/*.java desktop/src/main/java/henry/desktop/*.java \
  && java -cp out henry.desktop.Main
