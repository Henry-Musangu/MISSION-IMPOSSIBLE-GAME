@echo off
rem Compile et lance l'Agenda (version PC).
cd /d "%~dp0"
if not exist out\agenda mkdir out\agenda
javac -encoding UTF-8 -d out\agenda agenda\core\src\main\java\agenda\*.java agenda\desktop\src\main\java\agenda\desktop\*.java
if errorlevel 1 (
  echo Erreur de compilation. Verifie que le JDK Java est installe.
  pause
  exit /b 1
)
start "" javaw -cp out\agenda agenda.desktop.Main
