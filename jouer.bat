@echo off
rem Compile et lance Henry le Herisson.
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\henry\*.java
if errorlevel 1 (
  echo Erreur de compilation. Verifie que le JDK Java est installe.
  pause
  exit /b 1
)
java -cp out henry.Main
