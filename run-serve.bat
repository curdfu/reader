@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%PATH%
"C:\Program Files\Git\bin\bash.exe" --login -c "cd /d/Code/agent/codex/github.com/curdfu/reader && bash build.sh serve"
pause
