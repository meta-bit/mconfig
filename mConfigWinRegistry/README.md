# mConfig Windows Registry Module (JNR-FFI)

This module provides native access to the Windows Registry using JNR-FFI (Java Native Runtime - Foreign Function Interface).
It allows mConfig to read and discover configurations stored in the Windows Registry without requiring custom native binaries (DLLs), as JNR-FFI handles the native mapping dynamically.

## Features
- Native access to Windows Registry APIs (`advapi32.dll`) via JNR-FFI.
- No custom native build required; uses standard Windows system libraries.
- Automatic module discovery via `ServiceLoader` and JPMS.
- Support for common Registry types: `REG_SZ`, `REG_DWORD`, `REG_MULTI_SZ`, etc.
- Recursive reading of Registry keys into hierarchical mConfig structures.

## Dependencies

This module relies on the **JNR-FFI** ecosystem:
- `jnr-ffi`: The core abstraction for native calls.
- `jffi`: The low-level native interface.
- `asm`: Used by JNR for dynamic bytecode generation of native stubs.

While these dependencies increase the runtime classpath size (approximately 1.5 MB),
they eliminate the need for maintaining a separate C build pipeline for this module.

## Build Requirements

- **JDK 11+**: Standard Java development kit.
- **Maven**: Standard build tool used for the project.

No special toolchains (like MinGW) are required for this module, 
as it does not contain custom C code.

## Runtime Behavior

### Platform Detection
At runtime, the module utilizes `PlatformDetector` to check if the operating system is Windows. 
- **On Windows**: The module initializes and registers itself as a `ConfigStorage` provider for `USER` (HKCU) and `MACHINE` (HKLM) scopes.
- **On Non-Windows**: The module gracefully and silently disables itself during the `init()` phase. No native libraries are loaded, and no registry calls are attempted, ensuring stability on Linux, macOS, and other systems.

### Configuration Discovery
The module automatically discovers configurations by enumerating subkeys under the defined base path (default: `Software\<CompanyName>\<ApplicationName>\`).

## Security and Stability

### Permissions
The module operates with the permissions of the JVM process. It primarily focuses on read operations to provide configuration data to the application.

### Comparison with JNI Module
Compared to the `mConfigWinRegistryJNI` module:
- **Pros**: Easier to build and distribute (no DLLs to manage/sign), higher portability across Windows architectures (x86, x64, ARM64 handled by JNR).
- **Cons**: Larger dependency footprint and slightly higher memory usage due to the JNR runtime.

For environments where binary size is critical or external dependencies are restricted, the JNI-based module (`mConfigWinRegistryJNI`) is recommended as a lightweight alternative.
Caveats regarding DLL signing apply to the JNI module. (This one has no DLLs, so no signing concerns)
