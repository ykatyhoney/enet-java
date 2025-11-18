# Building and Running ENet Java Library

## Prerequisites

1. **Java 11+** - Required for compilation
2. **Maven** - For building Java code
3. **C Compiler** - For building native library:
   - Windows: MinGW-w64 or Visual Studio
   - Linux: GCC
   - macOS: Clang (Xcode Command Line Tools)
4. **ENet Library** - Download from https://github.com/lsalzman/enet

## Step 1: Build Native Library (JNI)

### Windows (MinGW-w64)

```bash
cd native
gcc -shared -fPIC -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" \
    -o enet_jni.dll enet_jni.c -lenet
```

### Windows (Visual Studio)

```bash
cd native
cl /LD /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" \
   enet_jni.c /link enet.lib /OUT:enet_jni.dll
```

### Linux

```bash
cd native
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    -o libenet_jni.so enet_jni.c -lenet
```

### macOS

```bash
cd native
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
    -o libenet_jni.dylib enet_jni.c -lenet
```

**Note:** Make sure the ENet library is installed and linked properly.

## Step 2: Build Java Code

```bash
mvn clean compile
```

## Step 3: Run Examples

### Option A: Using Maven Exec Plugin

Add to `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <configuration>
        <mainClass>com.enet.example.ServerExample</mainClass>
    </configuration>
</plugin>
```

Then run:
```bash
# Terminal 1 - Server
mvn exec:java -Dexec.mainClass="com.enet.example.ServerExample"

# Terminal 2 - Client
mvn exec:java -Dexec.mainClass="com.enet.example.ClientExample" -Dexec.args="localhost 7777"
```

### Option B: Manual Java Command

```bash
# Compile
mvn compile

# Set library path (adjust path to your native library location)
# Windows:
set PATH=%PATH%;native
# Linux/macOS:
export LD_LIBRARY_PATH=native:$LD_LIBRARY_PATH  # Linux
export DYLD_LIBRARY_PATH=native:$DYLD_LIBRARY_PATH  # macOS

# Run Server
java -cp target/classes com.enet.example.ServerExample

# Run Client (in another terminal)
java -cp target/classes com.enet.example.ClientExample localhost 7777
```

## Step 4: Package as JAR (Optional)

```bash
mvn package
```

This creates `target/enet-server-1.0.0.jar`

