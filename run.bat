@echo off
set JAVA_HOME=%USERPROFILE%\scoop\apps\openjdk17\current
set PATH=%JAVA_HOME%\bin;%USERPROFILE%\scoop\apps\maven\current\bin;%PATH%
cd /d %~dp0
java -Donnxruntime.native.path=%USERPROFILE%\.yolosight\native -jar target\yolo-sight-1.0.0.jar
