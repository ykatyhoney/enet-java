@echo off
REM Run client example on Windows

set PATH=%PATH%;native
if "%1"=="" (
    mvn exec:java -Dexec.mainClass="com.enet.example.ClientExample" -Dexec.args="localhost 7777"
) else (
    mvn exec:java -Dexec.mainClass="com.enet.example.ClientExample" -Dexec.args="%1 %2"
)

