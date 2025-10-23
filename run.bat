@echo off
echo Starting Warsaw Transport Map Application...
cd /d %~dp0
set PATH=%PATH%;%CD%\apache-maven-3.9.4\bin
mvn.cmd javafx:run

