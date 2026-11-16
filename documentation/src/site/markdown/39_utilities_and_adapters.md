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

### 3.9.1.4 What the adapters are, and what they are not

The adapters help with transition and adoption. They are views for reading, not a second facade;
for anything beyond that, use the mConfig `Configuration` itself.

- **Read-only.** Every `put(...)` throws `ConfigException` with reason `NOT_WRITEABLE`, with or
  without a scope; `isWriteable()` is `false`. (Up to 0.8.16, `RemappedConfiguration` wrote through
  for some `put` variants.)
- **No cursor**, and the schema cannot be set on a view: `getConfigCursor()` and `setConfigSchema()`
  throw `UnsupportedOperationException`.
- **`RemappedConfiguration` and `OverridingConfiguration` show their source as it is.** Values,
  entries, the schema, `getSourceLocations()`, `isClosed()` and update subscriptions are those of
  the configuration behind the view. A secret is handed on as a `SecretValue`. An entry reports
  the key, scope and location it has in the source; the schema of a remapped view knows the keys
  under the old prefix.
- **A remapped view hides what the source has under the new prefix.** Every key starting with
  `newPrefix` is read from `oldPrefix`, so a key the source itself has under `newPrefix` cannot be
  read through the view; a listing still names it once, since the view shows both under the same name.
  Choose a `newPrefix` the source does not use.
- **Overrides are checked when read.** An override that cannot be converted to the requested type
  throws `ConfigException` (`CONVERSION_FAILURE`); one that does not match the schema of the parent
  throws `ConfigException` (`ARGUMENT_INVALID`). The value is not put into the message. An entry
  from the overrides has no scope and no location (`null`): it was not read from anywhere.
- **`PropertiesConfiguration` behaves like the `Properties` behind it.** A key that is not there
  gives `null`, no exception. A value that cannot be converted to the requested type throws
  `ConfigException` (`CONVERSION_FAILURE`), as parsing the string in plain Java would (up to 0.8.16:
  `null`). `getSecret()` returns the property as a `SecretValue` (up to 0.8.16: always `null`);
  in the `Properties` it stays a plain, unprotected String, so the first such access is logged at
  INFO level, once per JVM, through the JDK's `System.Logger` named `mConfig`. Read secrets through
  mConfig itself. No schema, no locations, no update notifications.

## 3.9.2 Scope-Restricted Reads

All three adapter classes implement the full `Configuration` interface, including the
scope-aware read methods. Scope restriction is **per-call** — there is no per-instance
scope state — so the same adapter instance can safely serve call sites that need
different scope views.

The adapters do not treat the scopes alike:

| Adapter | Scope-restricted read |
|:--|:--|
| `RemappedConfiguration` | passed on to the source: only the given scopes are read |
| `OverridingConfiguration` | an override applies whatever the scopes; keys that are not overridden are read from the parent in the given scopes |
| `PropertiesConfiguration` | the scopes are ignored: `Properties` have none |

To read a value only from specific scopes, use `getConfigEntryFromFullKey(key, scopes)`:

```java
EnumSet<ConfigScope> localScopes = EnumSet.of(ConfigScope.PRODUCT, ConfigScope.HOST);

Configuration remapped = ConfigUtil.remapped(base, "old.prefix", "new.prefix");

ConfigEntry entry = remapped.getConfigEntryFromFullKey("my.key", localScopes);
String value = (entry != null) ? entry.getValueAsString() : null;
```

To enumerate all keys visible in specific scopes:

```java
Set<String> keys = config.getAllConfigurationKeysFlattened(localScopes);
```

## 3.9.3 `ConfigUtil` Entry Point

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

## 3.9.4 Usage

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
