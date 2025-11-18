#!/bin/bash
# Run server example on Linux/macOS

export LD_LIBRARY_PATH=native:$LD_LIBRARY_PATH  # Linux
export DYLD_LIBRARY_PATH=native:$DYLD_LIBRARY_PATH  # macOS

mvn exec:java -Dexec.mainClass="com.enet.example.ServerExample"

