#!/bin/bash
# Build script for Linux/macOS

if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME is not set"
    exit 1
fi

cd native

# Check if enet folder exists
if [ -f "enet/include/enet.h" ]; then
    echo "Using local ENet from enet folder..."
    ENET_INCLUDE="-Ienet/include"
else
    echo "Using system ENet installation..."
    ENET_INCLUDE=""
fi

OS="$(uname -s)"
case "${OS}" in
    Linux*)
        echo "Building libenet_jni.so for Linux..."
        gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
            $ENET_INCLUDE -o libenet_jni.so enet_jni.c
        LIB_NAME="libenet_jni.so"
        ;;
    Darwin*)
        echo "Building libenet_jni.dylib for macOS..."
        gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
            $ENET_INCLUDE -o libenet_jni.dylib enet_jni.c
        LIB_NAME="libenet_jni.dylib"
        ;;
    *)
        echo "Unsupported OS: $OS"
        exit 1
        ;;
esac

if [ $? -eq 0 ]; then
    echo "Build successful! $LIB_NAME created in native folder"
else
    echo "Build failed!"
    echo ""
    echo "Make sure the enet folder exists in the native directory:"
    echo "  native/enet/include/enet.h"
    echo ""
    echo "Or install ENet system-wide and ensure it's in your include path"
    echo ""
    echo "Also make sure:"
    echo "1. GCC/Clang is installed"
    echo "2. JAVA_HOME points to your JDK installation"
    exit 1
fi

cd ..

