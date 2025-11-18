#!/bin/bash
# Run client example on Linux/macOS

export LD_LIBRARY_PATH=native:$LD_LIBRARY_PATH  # Linux
export DYLD_LIBRARY_PATH=native:$DYLD_LIBRARY_PATH  # macOS

HOST=${1:-localhost}
PORT=${2:-7777}

mvn exec:java -Dexec.mainClass="com.enet.example.ClientExample" -Dexec.args="$HOST $PORT"

