# 3.6 Windows Registry Access

mConfig provides two alternative modules for accessing the Windows Registry. 
Both support reading configuration data from `HKEY_CURRENT_USER` (USER scope), `HKEY_LOCAL_MACHINE` (MACHINE scope), and Group Policy Objects (GPO) in both hives (POLICY scope).

## 3.6.1 Choosing the Right Module

| Feature           | `mConfigWinRegistry`                | `mConfigWinRegistryJNI`     |
|:------------------|:------------------------------------|:----------------------------|
| **Technology**    | JNR-FFI (Dynamic Native)            | JNI (Java Native Interface) |
| **Artifact Size** | ~13 KB                              | **~47 KB** (includes DLL)   |
| **Runtime Size**  | **~1.5 MB** (includes dependencies) | ~47 KB                      |
| **Dependencies**  | JNR-FFI, ASM, JFFI                  | None                        |
| **Complexity**    | Zero (no DLL to manage)             | Medium (DLL handling)       |
| **Build Req.**    | Standard Java/Maven                 | Linux + MinGW-w64 (for DLL) |
| **Performance**   | High                                | **Very High**               |

### `mConfigWinRegistry` (JNR-FFI based)
This module is the easiest to use as it requires no native toolchain and handles native access dynamically. 
- **Pros**: No native binaries to manage; works out-of-the-box.
- **Cons**: Significantly larger footprint (~1.5 MB) due to the JNR-FFI ecosystem.
- **Requirement**: Java 11+.

### `mConfigWinRegistryJNI` (JNI based)
This module provides the smallest footprint and highest performance by using a custom-built DLL.
- **Pros**: Extremely lightweight; no external dependencies.
- **Cons**: Requires building a DLL on Linux (cross-compiled for Windows); the DLL must be provided to the end-user (usually bundled in the JAR).
- **Requirement**: Windows DLL (`mConfigWinRegistryJNI.dll`).
- **Signing**: The bundled DLL is provided **unsigned**. For production deployments, application developers are responsible for signing the DLL as part of their distribution to establish a "Chain of Trust".

## 3.6.2 Priority and Discovery

If both modules are present on the classpath, mConfig defaults to using `mConfigWinRegistryJNI` (`registryjni`) over `mConfigWinRegistry` (`registry`) for better performance.

To use a specific one, you can configure `STORAGE_TYPE_PRIORITIES`:
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("company", "app")
    .setFeature(ConfigFeature.STORAGE_TYPE_PRIORITIES, List.of("registryjni")) // Force JNI
    .build())
    {
    // ...
    }
```

## 3.6.3 Base Path Configuration

By default, the registry source looks under `Software\\[<CompanyName>\\\\]<ApplicationName>` (company segment omitted if `COMPANY_NAME` blank/null/whitespace). You can override this using the `REGISTRY_BASE_PATH` feature:

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("company", "app")
    .setFeature(ConfigFeature.REGISTRY_BASE_PATH, "Software\\LegacyPath\\App")
    .build())
    {
    // ...
    }
```
