# 2.3 Configuration Schemas

Configuration schemas define the contract for a set of configuration entries. 
They specify what keys are expected, their data types, default values, validation rules, and other metadata.

## 2.3.1 Overview

A schema ensures that the configuration data used by the application is valid and consistent. 
When a configuration is loaded (e.g., via `getConfig(name)`), mConfig attempts to bind it to a matching schema.

### 2.3.1.1 Benefits of using Schemas
- **Validation**: Automatically check if values match expected types and patterns.
- **Defaults**: Provide fallback values if none are found in any configuration layer.
- **Documentation**: Automatically generate documentation for configuration entries.
- **Type Safety**: Preserve data types (e.g., Integer, Boolean) throughout the configuration stack.
- **Security**: Identify which entries contain sensitive data (secrets).

## 2.3.2 Example

Minimal example (single schema entry with a default):
```json
[
  { "KEY": "server/port", "TYPE": "NUMBER", "DEFAULT": 8080, "DESCRIPTION": "Listening port" }
]
```

Named schema example (multiple entries):
```json
{
  "my-app-config": {
    "NAME": "my-app-config",
    "ENTRIES": [
      { "KEY": "server/host", "TYPE": "STRING", "DEFAULT": "127.0.0.1" },
      { "KEY": "server/port", "TYPE": "NUMBER", "DEFAULT": 8080 }
    ]
  }
}
```

## 2.3.3 Schema Structure

Schemas are typically defined in JSON format. A schema file can contain a single schema or multiple named schemas.

### 2.3.3.1 Schema Entry Properties

Each entry in a schema (within the `ENTRIES` list) can have the following properties:

| Property         | Type    | Description                                                                                                                                   |
|:-----------------|:--------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| `KEY`            | String  | The hierarchical path of the entry (e.g., `database/host`).                                                                                   |
| `TYPE`           | String  | The data type: `STRING`, `NUMBER`, `BOOLEAN`, `BYTES`, `MULTIPLE_STRINGS`, `ENUM`, `ENUM_SET`, `URI`, `FILEPATH`, `DATE`, `TIME`, `DATETIME`. |
| `DESCRIPTION`    | String  | Human-readable description or Resource Bundle key for localized description. (Optional)                                                       |
| `DEFAULT`        | (any)   | The default value used if no other value is provided. (Optional)                                                                              |
| `PATTERN`        | String  | A validation pattern (Regex, range specification, or enum list). (Optional)                                                                   |
| `SECRET`         | Boolean | If `true`, the entry holds a cryptographic secret or credential: it is redacted in logs, kept out of the JVM String pool, and retrieved via `getSecret()`. (Optional, defaults to `false`) |
| `SENSITIVE`      | Boolean | If `true`, the entry holds information that must not be disclosed, typically personal data under GDPR. **Defined but not yet enforced** — see below. (Optional, defaults to `false`) |
| `HIDDEN`         | Boolean | If `true`, the entry is hidden from automatic documentation. (Optional, defaults to `false`)                                                  |
| `FLAGS`          | Array   | Declarative markers for the entry: `MANDATORY`, `READ_ONLY`, `WRITE_ONLY`, `SECRET`, `SENSITIVE`. Not all are enforced yet — see below.        |
| `ARITY`          | String  | The number of allowed occurrences (e.g., `1`, `0..1`, `1..*`). (Optional; see defaults below)                                                 |
| `MANDATORY`      | Object  | (Optional) Block for future-proofing and strict requirements. Note this is the *block*, distinct from the `MANDATORY` entry in `FLAGS`. See below. |
| `AFTER`          | String  | (Optional) For temporal types, value must be after this (ISO format or `now`).                                                                |
| `BEFORE`         | String  | (Optional) For temporal types, value must be before this (ISO format or `now`).                                                               |
| `REQUIRE_OFFSET` | Boolean | (Optional) For `DATETIME`, requires an offset (e.g., `+02:00` or `Z`).                                                                        |

Property names are matched case-insensitively, so `"KEY"` and `"key"` are equivalent.

#### Boolean values

An entry of `TYPE` `BOOLEAN` accepts `true/false`, `yes/no`, `on/off` and `1/0`, in any case and with
surrounding blanks; a `DEFAULT` of `"on"` is valid. Any other value, an empty one included, does not match the
schema. Such a value is skipped when read: the next layer, or finally the `DEFAULT`, applies, so a typing error in
one file does not switch a feature off. Writing such a value through `put()` is rejected. (Up to 0.8.16 the check
accepted `true` and `false` only.)

Without a schema, `getBoolean()` uses the first value found and throws `ConfigException` (`CONVERSION_FAILURE`) if
it is not one of these literals. See [`LENIENT_BOOLEAN_CONVERSION`](25_config_features.md) for the plain Java
behaviour.

#### Confidentiality markers: `SECRET` and `SENSITIVE`

These mark two different kinds of confidentiality, and most entries that are `SECRET` are also
`SENSITIVE` — though rarely the reverse. In short: `SECRET` is for cryptographic secrets and
passwords, `SENSITIVE` for information that must not leak into view, such as personal data.
See [Secret and Sensitive](26_secrets_concept.md#secret-and-sensitive) for the full model,
including which storage places will accept which kind of data.

Both markers suppress disclosure of the *value* only. The storage location of a marked entry
stays traceable through `getScope()`, `getLocation()` and `getURI()`, so protected data remains
findable, correctable and erasable.

#### Which markers are enforced today

The `FLAGS` array is accepted and parsed, but the library does not yet act on every marker in it:

| Marker       | Enforced? | Notes                                                                        |
|:-------------|:----------|:------------------------------------------------------------------------------|
| `MANDATORY`  | yes       | via `FLAGS`; drives default arity and presence validation                     |
| `SECRET`     | yes       | **only via the `SECRET` property**, not via `FLAGS` (see the warning below)   |
| `SENSITIVE`  | no        | recognised and stored; no redaction or export suppression yet                 |
| `READ_ONLY`  | no        | recognised and stored; writes are not blocked yet                             |
| `WRITE_ONLY` | no        | recognised and stored; reads are not blocked yet                              |

> **Warning:** `"FLAGS": ["SECRET"]` does **not** currently mark an entry as secret. The flag is
> stored but is not consulted by the secrets handling, which reads only the separate `SECRET`
> property. Always write `"SECRET": true`. Using the flag alone means the value is *not*
> protected — no redaction, no `SecretValue`, no wiping.

Since 0.8.15, loading a schema logs a WARNING for each entry that uses a marker without effect:
`SECRET` in `FLAGS` without `"SECRET": true` (the value is not protected), and `SENSITIVE`,
`READ_ONLY` or `WRITE_ONLY` (not enforced yet). `SECRET` in `FLAGS` next to `"SECRET": true` is
redundant and not reported. 0.9.0 will be stricter.

### 2.3.3.2 Arity (`ARITY`)

The `ARITY` property defines the constraints on the number of elements for an entry. This is especially useful for `MULTIPLE_STRINGS` and `ENUM_SET` types.

- **Exact number**: `"ARITY": "3"` (exactly 3 items required).
- **Range**: `"ARITY": "1..5"` (minimum 1, maximum 5 items).
- **Open-ended**: `"ARITY": "1..*"` or `"ARITY": "1..n"` (at least 1 item, no upper limit).
- **Defaults**:
    - Mandatory entries: `1`
    - Optional entries: `0..1`
    - List types (`MULTIPLE_STRINGS`, `ENUM_SET`): `0..*` (optional) or `1..*` (mandatory).

### 2.3.3.3 Validation Patterns (`PATTERN`)

Validation patterns depend on the `TYPE` of the entry:

- **`STRING`**: A Java Regular Expression. Compiled with `Pattern.UNICODE_CHARACTER_CLASS` by default, supporting Unicode property escapes like `\p{L}`.
- **`NUMBER`**:
    - **Mathematical Interval Notation**: Supports `[min, max]`, `(min, max)`, `[min, max)`, and `(min, max]`. Example: `[1024, 65535]`.
    - **Predefined Aliases**:
        - Unsigned: `uint8` (0-255), `uint16`, `uint32`, `uint64`.
        - Signed: `int8` (-128 to 127), `int16`, `int32`, `int64`, and custom bit-widths like `int7`, `int15`, `int31`, `int63`.
- **`ENUM` / `ENUM_SET`**: A pipe-separated list of valid options. Example: `DEBUG|INFO|WARN|ERROR`.
- **`URI`**: A valid URI string (RFC 3986).
- **`FILEPATH`**: A valid path on the filesystem.
    - **Path Validation Flags**: Can be placed inside the `MANDATORY` block for semantic checks:
        - `EXISTS`: `true` (path must exist)
        - `IS_DIRECTORY`: `true` (path must be a directory)
        - `IS_FILE`: `true` (path must be a regular file)
        - `CAN_WRITE`: `true` (path must be writable)
- **`DATE` / `TIME` / `DATETIME`**: Temporal types in ISO-8601 format.
    - **Temporal Validation Flags**: Can be used directly or inside `MANDATORY`:
        - `AFTER`: Value must be chronologically after this (e.g., `"2020-01-01"`, `"now"`).
        - `BEFORE`: Value must be chronologically before this.
        - `REQUIRE_OFFSET`: (For `DATETIME` only) If `true`, requires the presence of a time zone offset (e.g., `+05:00` or `Z`).

### 2.3.3.4 Internationalization (I18N)

The `DESCRIPTION` property can be a plain string or a key in a Resource Bundle.
- mConfig looks for Resource Bundles named `messages` in the same locations as schemas (e.g., `.config/messages.properties`).
- If a matching key is found in the bundle for the current locale (configured via `ConfigFeature.PREFERRED_LANGUAGE`), the localized string is used.
- Otherwise, the raw `DESCRIPTION` string is used as a fallback.

## 2.3.4 ConfigSchemaFactory

For 1.0 and better multi-language support (JNI/Rust/Python), use `ConfigSchemaFactory` to create schemas and entries programmatically:

```java
ConfigSchemaFactory factory = ConfigSchemaFactory.create();
ConfigSchema schema = factory.createSchema();
ConfigSchemaEntry entry = factory.createEntry("port", ConfigEntryType.NUMBER)
    .setValidationPattern("uint16")
    .setDefault("8080");
schema.addSchemaEntry(entry);
```

## 2.3.5 Future-Proofing with the `MANDATORY` block

To ensure that future extensions to the schema format do not break older versions of the library, or conversely, that newer schemas can demand features that older versions must respect, mConfig uses a `MANDATORY` sub-item.

If a schema entry contains a `MANDATORY` block, the library MUST understand all keys within that block. If any key inside `MANDATORY` is unknown to the current version of mConfig, the schema loading will fail with an `UNKNOWN_MANDATORY_FEATURE` error.

**Example:**
```json
{
  "KEY": "network/timeout",
  "TYPE": "NUMBER",
  "MANDATORY": {
    "UNIT": "milliseconds"
  }
}
```
In this example, if a future version of mConfig introduces a `UNIT` feature and it's placed inside `MANDATORY`, an older version that doesn't know about `UNIT` will refuse to load the schema, preventing potential misconfiguration (e.g., interpreting milliseconds as seconds).

Features outside the `MANDATORY` block that are unknown to the library are silently ignored.

## 2.3.6 Formats

mConfig supports several JSON structures for defining schemas:

### 2.3.6.1 Named Schema Object
```json
{
  "my-app-config": {
    "NAME": "my-app-config",
    "ENTRIES": [
      { "KEY": "port", "TYPE": "NUMBER", "DEFAULT": 8080 }
    ]
  }
}
```

### 2.3.6.2 List of Entries (Anonymous Schema)
If a JSON file contains only a list of entries, it is treated as a schema for the configuration matching the filename (excluding `.mconfig-schema.json`).
```json
[
  { "KEY": "port", "TYPE": "NUMBER", "DEFAULT": 8080 }
]
```

## 2.3.7 Discovery

Schemas are automatically discovered from multiple locations:

1.  **Classpath**: Scanned within `.config/` directories. Filenames should follow the pattern `name.mconfig-schema.json`.
2.  **Filesystem (Local Repository)**: Scanned in prioritized OS-specific locations to allow system-wide sharing and hardening. Schemas are stored under an app-scoped subdirectory:
    *   `[<company>/]<application>/<configName>.mconfig-schema.json`
    *   If company is blank/null/whitespace, the company segment is omitted (app-only paths).
    *   **Windows**: `%APPDATA%\mconfig\schemas`, `%PROGRAMDATA%\mconfig\schemas`.
    *   **Unix/Linux/Mac**: `~/.config/mconfig/schemas`, `/etc/mconfig/schemas`, `/usr/local/share/mconfig/schemas`.
3.  **Manual**: Provided via `ConfigFactoryBuilder.setSchemas()`.

### 2.3.7.1 Precedence and Control
By default, schemas found in the filesystem take precedence over those found on the classpath. This allows administrators to override or harden configuration contracts without modifying the application binaries.

This behavior can be controlled via `ConfigFeature`:
*   `ALLOW_LOCAL_SCHEMA_OVERRIDE` (default `true`): Set to `false` to ignore filesystem schemas and lock the application to bundled contracts. Deprecated since 0.8.17: 0.9.0 replaces it with an allowlist of schema sources. It keeps working in 0.8.x; keep using it until then.
*   `LOCAL_SCHEMA_DIRECTORY`: Can be used to specify a custom directory for schema discovery instead of the OS defaults.



## Developer Notes
For internals/parsing/validation details, see project `devdocs/config_schemas_internals.md` (not included in public docs).
