# mConfig Windows Registry JNI Module

This module provides high-performance, native access to the Windows Registry using JNI (Java Native Interface).
It is designed to be a faster and smaller alternative to the JNR-FFI based registry module.

## Features
- Direct native access to Windows Registry APIs (`advapi32.dll`).
- Full Unicode (UTF-16) support for keys and values.
- Automatic module discovery via `ServiceLoader` and JPMS.
- Secure, versioned DLL extraction at runtime.

## Build Requirements

### Platform
The native DLL is cross-compiled on **Linux** (Debian-based systems preferred) 
to ensure a consistent and reproducible build environment.

### Toolchain
- **MinGW-w64**: Required for cross-compiling the Windows DLL on Linux.
  - Debian/Ubuntu installation: `sudo apt install gcc-mingw-w64-x86-64`
- **JDK 11+**: Required for the Java compilation and JNI header generation.
- **Maven**: Standard build tool used for the project.

## Native Compilation

The compilation of the native C code into a Windows DLL is managed by a Maven profile.

### Profile: `build-native-dll`
This profile is automatically activated when:
1. The build is running on **Linux**.
2. The cross-compiler `/usr/bin/x86_64-w64-mingw32-gcc` is detected.

To explicitly run the native build (e.g., in CI):
```bash
mvn clean compile -pl mConfigWinRegistryJNI -Pbuild-native-dll
```

The resulting `mConfigWinRegistryJNI.dll` is bundled into the JAR at `/META-INF/natives/windows-x64/`.

## Runtime DLL Handling

To ensure seamless operation and allow for user overrides (e.g., providing a signed DLL):
1. **Detection**: At runtime, the library detects if it's running on Windows.
2. **Preference for System Path**: The library first attempts to load `mConfigWinRegistryJNI` via `System.loadLibrary()`. This allows users to provide their own DLL by placing it on the standard library path (e.g., using `-Djava.library.path`).
3. **Preference for Pre-extracted DLL**: If not found on the system path, it checks if the DLL already exists in the versioned temporary directory:
   - Path: `java.io.tmpdir/mConfig/<version>/mConfigWinRegistryJNI.dll`
   - If the file exists, the library attempts to load it directly, **forgoing extraction**. This allows users to manually place their own signed DLL in this location.
4. **Extraction**: If neither of the above succeeds, the bundled DLL is extracted from the JAR to the temporary directory.
5. **Loading**: The resulting DLL is then loaded into the JVM.

## Security and Signing Responsibility

### Integrity
The native code is minimal and focuses strictly on Registry read operations to minimize the attack surface.

### Code Signing
**The bundled DLL is provided UNSIGNED.**

In accordance with standard library distribution practices:
- **Our Role**: We provide the source code and a clean, deterministic build of the native component.
- **User's Role**: The application developer incorporating `mConfig` is responsible for applying their own **Digital Signature** to the final distribution.

Windows security features (like SmartScreen and Anti-Virus) evaluate the reputation of the entire application package.
For production deployments, it is highly recommended that you include the extracted or bundled DLL in your code-signing pipeline to establish a proper "Chain of Trust" for your users.
