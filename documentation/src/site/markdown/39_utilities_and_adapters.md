# 3.9 Utilities and Adapters

The `mConfigUtil` module provides specialized `Configuration` implementations (adapters) and static utility methods to bridge existing Java structures with the `mConfig` ecosystem.

## 3.9.1 Core Adapters

These classes allow existing configuration sources to be viewed through the `Configuration` interface.

### 3.9.1.1 `PropertiesConfiguration`
Used for adapting legacy code step-by-step. It allows programmatically generated `java.util.Properties` to be integrated into the unified `mConfig` structure.
- **Note**: This adapter provides almost none of the advanced features `mConfig` supplies (like multi-tier resolution or automatic schema discovery).
- **Recommendation**: Consider loading properties via `mConfig` itself by adding paths to `ConfigFeature.ADDITIONAL_USER_DIRECTORIES`.

### 3.9.1.2 `OverridingConfiguration`
Provides an immutable view that overlays a `Map<String, Object>` on top of an existing `Configuration`. Overrides take precedence.
- **Note**: This is intended for temporary, in-memory overlays. 
- **Recommendation**: For most use cases, prefer using `Configuration.put*(key, value, ConfigScope.RUNTIME)`. The `RUNTIME` scope is specifically designed for temporary overrides that are not persisted.

### 3.9.1.3 `RemappedConfiguration`
Remaps keys matching a `newPrefix` to an `oldPrefix` for lookup in an underlying `source` configuration. This is useful for maintaining backward compatibility or redirecting configuration branches.

## 3.9.2 `ConfigUtil` Entry Point

The `ConfigUtil` class provides convenient factory methods for the adapters above, along with other helper functions:

- **Factory Methods**:
    - `ConfigUtil.fromProperties(Properties props)`
    - `ConfigUtil.withOverrides(Configuration parent, Map<String, Object> overrides)`
    - `ConfigUtil.remapped(Configuration source, String oldPrefix, String newPrefix)`
- **Path Discovery**:
    - `ConfigUtil.whereToPutMyFiles(...)`: Helps find writable configuration paths for a given scope.
    - `ConfigUtil.printConfigPaths(...)`: Prints the discovered configuration search paths for debugging.
- **Property Extraction**:
    - `ConfigUtil.copyPrefixedEntriesToJavaProperties(...)`: Exports a subset of a configuration to a `Properties` object.
    - `ConfigUtil.copySchemeDefinedEntriesToJavaProperties(...)`: Exports all entries defined by the configuration schema.
- **Quick Configuration**:
    - `ConfigUtil.quickConfig(...)`: A one-liner for bootstrapping a standard configuration for simple applications.

## 3.9.3 Usage

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigUtil</artifactId>
    <version>${mconfig.version}</version>
</dependency>
```

JPMS module name:
```java
requires metabit.mconfig.util;
```
