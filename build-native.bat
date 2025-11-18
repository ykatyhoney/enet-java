@echo off
REM Build script for Windows (MinGW-w64)

if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set
    exit /b 1
)

cd native

REM Check if enet folder exists
if exist "enet\include\enet.h" (
    echo Using local ENet from enet folder...
    set ENET_INCLUDE=-Ienet\include
) else (
    echo Using system ENet installation...
    set ENET_INCLUDE=
)

echo Building enet_jni.dll...
gcc -shared -fPIC -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" %ENET_INCLUDE% ^
    -o enet_jni.dll enet_jni.c -lws2_32 -lwinmm

if %ERRORLEVEL% EQU 0 (
    echo Build successful! enet_jni.dll created in native folder
) else (
    echo Build failed! 
    echo.
    echo Make sure the enet folder exists in the native directory:
    echo   native/enet/include/enet.h
    echo.
    echo Or install ENet system-wide and ensure it's in your include path
    echo.
    echo Also make sure:
    echo 1. MinGW-w64 is installed and in PATH
    echo 2. JAVA_HOME points to your JDK installation
)

cd ..

