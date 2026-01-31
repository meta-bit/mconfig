# Immutable Configuration Views

These utility methods in `ConfigUtil` create lightweight, immutable `Configuration` views for overrides, prefix remapping,
and `Properties` adaptation. They support chaining and inherit scheme validation where applicable.

## withOverrides

Overlay in-memory overrides on a parent `Configuration`:

```java
Configuration parent = factory.getConfiguration("app");
Map<String, Object> overrides = Map.of(
    "db.host", "localhost",
    "db.port", 5432
    );
Configuration cfg = ConfigUtil.withOverrides(parent, overrides);

String host = cfg.getString("db.host"); // "localhost"
Integer port = cfg.getInteger("db.port"); // 5432 (auto-convert)
```

- Overrides take precedence.
- Lazy type conversion (null on parse fail).
- If `parent` has `ConfigScheme`, validates overrides (throws `IllegalArgumentException` on mismatch).
- `getAllConfigurationKeysFlattened()`: union of keys.

## remapped

Remap prefixed keys for sub-config views (e.g., DB modules):

```java
Configuration cfg = ConfigUtil.remapped(parent, "super_db.", "db.");
String host = cfg.getString("db.host"); // → parent.getString("super_db.host")
String other = cfg.getString("other.key"); // passthru
```

- `get("db.xxx")` → `parent.get("super_db.xxx")`.
- Non-matching keys delegate directly.
- Trims prefixes/keys aggressively.
- Uniform for all typed getters; projects `getAll*`.
- Inherits scheme.

## fromProperties

Adapt legacy `java.util.Properties`:

```java
Properties props = new Properties();
props.setProperty("key1", "value1");
props.setProperty("key2", "42");
Configuration cfg = ConfigUtil.fromProperties(props);

String v1 = cfg.getString("key1"); // "value1"
Integer v2 = cfg.getInteger("key2"); // 42 (null on fail)
```

- Lazy parsing, null on errors.
- No scheme.
- `getAllConfigurationKeysFlattened()`: props keys.

## Chaining

Combine freely:

```java
Configuration dbView = ConfigUtil.withOverrides(
    ConfigUtil.remapped(parent, "legacy_db.", "db."), 
    Map.of("db.timeout", 30)
    );
```

Views are read-only (`put*` throws `UnsupportedOperationException`)."

