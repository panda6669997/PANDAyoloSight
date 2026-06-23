#!/bin/bash
export JAVA_HOME="$HOME/scoop/apps/openjdk17/current"
export PATH="$JAVA_HOME/bin:$HOME/scoop/apps/maven/current/bin:/c/Windows/System32/WindowsPowerShell/v1.0:$PATH"
cd "$(dirname "$0")"
mvn javafx:run
