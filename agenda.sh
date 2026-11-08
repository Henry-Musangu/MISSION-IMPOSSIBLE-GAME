#!/bin/sh
# Compile et lance l'Agenda (version PC).
cd "$(dirname "$0")" || exit 1
mkdir -p out/agenda
javac -encoding UTF-8 -d out/agenda agenda/core/src/main/java/agenda/*.java agenda/desktop/src/main/java/agenda/desktop/*.java \
  && java -cp out/agenda agenda.desktop.Main
