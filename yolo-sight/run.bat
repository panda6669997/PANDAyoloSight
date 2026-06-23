@echo off
set JAVA_HOME=%USERPROFILE%\scoop\apps\openjdk17\current
set PATH=%JAVA_HOME%\bin;%USERPROFILE%\scoop\apps\maven\current\bin;%PATH%
cd /d %~dp0
mvn javafx:run
