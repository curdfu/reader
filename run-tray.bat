@echo off
cd /d "%~dp0"
call .\gradlew.bat buildServerJar jarTray --quiet
start "" javaw -jar "build\libs\reader-tray-2.5.4.jar"
