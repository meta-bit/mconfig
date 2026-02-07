# 2.3 Configuration Schemes

Configuration schemes define the contract for a set of configuration entries. 
They specify what keys are expected, their data types, default values, validation rules, and other metadata.

## 2.3.1 Overview

A scheme ensures that the configuration data used by the application is valid and consistent. 
When a configuration is loaded (e.g., via `getConfig(name)`), mConfig attempts to bind it to a matching scheme.

### 2.3.1.1 Benefits of using Schemes
- **Validation**: Automatically check if values match expected types and patterns.
- **Defaults**: Provide fallback values if none are found in any configuration layer.
- **Documentation**: Automatically generate documentation for configuration entries.
- **Type Safety**: Preserve data types (e.g., Integer, Boolean) throughout the configuration stack.
- **Security**: Identify which entries contain sensitive data (secrets).

## 2.3.2 Example

Minimal example (single scheme entry with a default):
```json
[
  { "KEY": "server/port", "TYPE": "NUMBER", "DEFAULT": 8080, "DESCRIPTION": "Listening port" }
]
```

Named scheme example (multiple entries):
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

## 2.3.3 Scheme Structure

Schemes are typically defined in JSON format. A scheme file can contain a single scheme or multiple named schemes.

### 2.3.3.1 Scheme Entry Properties

Each entry in a scheme (within the `ENTRIES` list) can have the following properties:

| Property         | Type    | Description                                                                                                                                   |
|:-----------------|:--------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| `KEY`            | String  | The hierarchical path of the entry (e.g., `database/host`).                                                                                   |
| `TYPE`           | String  | The data type: `STRING`, `NUMBER`, `BOOLEAN`, `BYTES`, `MULTIPLE_STRINGS`, `ENUM`, `ENUM_SET`, `URI`, `FILEPATH`, `DATE`, `TIME`, `DATETIME`. |
| `DESCRIPTION`    | String  | Human-readable description or Resource Bundle key for localized description. (Optional)                                                                  |
| `DEFAULT`        | (any)   | The default value used if no other value is provided. (Optional)                                                                                         |
| `PATTERN`        | String  | A validation pattern (Regex, range specification, or enum list). (Optional)                                                                              |
| `SECRET`         | Boolean | If `true`, the entry is treated as sensitive data (redacted in logs). (Optional, defaults to `false`)                                                                         |
| `HIDDEN`         | Boolean | If `true`, the entry is hidden from automatic documentation. (Optional, defaults to `false`)                                                                                  |
| `ARITY`          | String  | The number of allowed occurrences (e.g., `1`, `0..1`, `1..*`). (Optional; see defaults below)                                                                                |
| `MANDATORY`      | Object  | (Optional) Block for future-proofing and strict requirements. See below.                                                                      |
| `AFTER`          | String  | (Optional) For temporal types, value must be after this (ISO format or `now`).                                                                |
| `BEFORE`         | String  | (Optional) For temporal types, value must be before this (ISO format or `now`).                                                               |
| `REQUIRE_OFFSET` | Boolean | (Optional) For `DATETIME`, requires an offset (e.g., `+02:00` or `Z`).                                                                        |

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
- mConfig looks for Resource Bundles named `messages` in the same locations as schemes (e.g., `.config/messages.properties`).
- If a matching key is found in the bundle for the current locale (configured via `ConfigFeature.PREFERRED_LANGUAGE`), the localized string is used.
- Otherwise, the raw `DESCRIPTION` string is used as a fallback.

## 2.3.4 ConfigSchemeFactory

For 1.0 and better multi-language support (JNI/Rust/Python), use `ConfigSchemeFactory` to create schemes and entries programmatically:

```java
ConfigSchemeFactory factory = ConfigSchemeFactory.create();
ConfigScheme scheme = factory.createScheme();
ConfigSchemeEntry entry = factory.createEntry("port", ConfigEntryType.NUMBER)
    .setValidationPattern("uint16")
    .setDefault("8080");
scheme.addSchemeEntry(entry);
```

## 2.3.5 Future-Proofing with the `MANDATORY` block

To ensure that future extensions to the scheme format do not break older versions of the library, or conversely, that newer schemes can demand features that older versions must respect, mConfig uses a `MANDATORY` sub-item.

If a scheme entry contains a `MANDATORY` block, the library MUST understand all keys within that block. If any key inside `MANDATORY` is unknown to the current version of mConfig, the scheme loading will fail with an `UNKNOWN_MANDATORY_FEATURE` error.

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
In this example, if a future version of mConfig introduces a `UNIT` feature and it's placed inside `MANDATORY`, an older version that doesn't know about `UNIT` will refuse to load the scheme, preventing potential misconfiguration (e.g., interpreting milliseconds as seconds).

Features outside the `MANDATORY` block that are unknown to the library are silently ignored.

## 2.3.6 Formats

mConfig supports several JSON structures for defining schemes:

### 2.3.6.1 Named Scheme Object
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

### 2.3.6.2 List of Entries (Anonymous Scheme)
If a JSON file contains only a list of entries, it is treated as a scheme for the configuration matching the filename (excluding `.scheme.json`).
```json
[
  { "KEY": "port", "TYPE": "NUMBER", "DEFAULT": 8080 }
]
```

## 2.3.7 Discovery

Schemes are automatically discovered from the classpath within `.config/` directories.
- Filenames should follow the pattern `name.scheme.json`.
- Schemes can also be provided manually via `ConfigFactoryBuilder.setSchemes()`.
- Discovered schemes are registered in the `ConfigSchemeRepository`.



## Developer Notes
For internals/parsing/validation details, see project `devdocs/config_schemes_internals.md` (not included in public docs).