@echo off
REM Run server example on Windows

set PATH=%PATH%;native
mvn exec:java -Dexec.mainClass="com.enet.example.ServerExample"

