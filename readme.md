# Java ENet Library

A Java wrapper for the ENet reliable UDP networking library using JNI bindings.

**ENet Version:** 2.6.5 (from [zpl-c/enet](https://github.com/zpl-c/enet))

## Running

### Server
```bash
java -cp target/classes com.enet.example.ServerExample
# or
run-server.bat
```

### Client
```bash
java -cp target/classes com.enet.example.ClientExample localhost 7777
# or
run-client.bat
```

### JNI Build
```bash
build-native.bat
```

## Building

See [BUILD.md](BUILD.md) for detailed build instructions.