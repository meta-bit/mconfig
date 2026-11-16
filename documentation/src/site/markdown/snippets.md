# Snippets Reference

A lookup page: the exact names, fields, settings and scopes to use when writing code against
mConfig. It holds facts, not templates.

- **Code to copy** is in
  [examples/snippets.md](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md)
  (in a source checkout: `examples/snippets.md`). Its Java templates are compiled against the
  current API on every build.
- **What to replace in existing code** is in
  [Better practice: replace this with that](38_code_improvements.md).

Audience: developers and AI coding assistants. Where this page and the source code disagree, the
code is right and this page needs fixing.

## 1. API contract: the exact names

Older documents and generated code often contain the forms in the right-hand column.
**Do not use those.**

| Correct                                                         | Wrong or stale                                       |
|-----------------------------------------------------------------|------------------------------------------------------|
| `Configuration cfg = factory.getConfig("net");`                 | `Config cfg = ...` (no such type)                    |
| `factory.getConfig(name)`                                       | `factory.getConfiguration(name)` (does not exist)    |
| `cfg.put(key, value)` / `cfg.put(key, value, scope)`            | `cfg.set(key, value)` (does not exist)               |
| `ConfigFactoryBuilder.create(org, app)`                         | `new ConfigFactoryBuilder(...)` (it is an interface) |
| `builder.setTestMode(true);` returns **`void`**                 | `.setTestMode(true).build()` (will not compile)      |
| `builder.setTestConfigPaths(scope, list);` returns **`void`**   | chaining off it (will not compile)                   |
| `builder.setFeature(...)` returns **the builder**, chainable    |                                                      |
| `cfg.getSecret(key)` returns a `SecretValue`                    | casting `getEntry(...)` to `SecretValue`             |
| `secret.getValue()` (`byte[]`) / `getChars()`, then `erase()`   | `secret.clearValue()` (does not exist)               |
| `ConfigSchema`, `SCHEMA_*`, `*.mconfig-schema.json`             | `ConfigScheme`, `SCHEME_*`                           |
| `"SECRET": true` in the schema entry                            | a "Secret" entry type; `"FLAGS": ["SECRET"]`         |

Other hard facts:

- **The key separator is `/`** for hierarchical formats: `cfg.getString("server/host")`.
  Flat formats (`.properties`) keep dotted keys literal: `cfg.getString("db.password")`.
- **Typed getters return boxed types** (`Integer`, `Boolean`, `Double`), so they can be `null`
  when `EXCEPTION_ON_MISSING_ENTRY` is turned off.
- **`ConfigException` is unchecked; `ConfigCheckedException` is checked.**
  `getConfig(String)` throws the unchecked one. `getConfig(String, ConfigSchema)`,
  `flush()`, `reload()` and `setTestConfigPaths()` throw the checked one.
- **`ConfigFactory.close()` is declared without `throws`**, so try-with-resources is clean.
  `Configuration` inherits `AutoCloseable.close() throws Exception`; let the factory own the
  lifecycle instead of wrapping a `Configuration` in try-with-resources.
- **One `ConfigFactory` per application.** It owns the file watchers, caches and background
  threads; `close()` releases them.
- **Never provide defaults at the call site.** They belong in a `*.mconfig-schema.json` or a
  `PRODUCT`-scope resource file.
- **Dependency versions:** read the version from the project's own build file or the local
  repository. Do not copy a version or a `${mconfig.version}` placeholder from documentation.

## 2. Schema fields and gotchas

A schema is a JSON file named `<configName>.mconfig-schema.json`, placed under
`.config/<company>/<app>/` on the classpath, or in the OS schema directories
(`/etc/mconfig/schemas`, `~/.config/mconfig/schemas`, `%PROGRAMDATA%\mconfig\schemas`, …).
It is found automatically. A complete example is in the
[templates](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md#3-defaults-schema-and-resource-file);
the full description is in [Configuration Schemas](23_configuration_schemas.md).

| Field         | Meaning                                                                                                                                   |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `KEY`         | the key, with `/` as separator                                                                                                            |
| `TYPE`        | `STRING`, `NUMBER`, `BOOLEAN`, `BYTES`, `MULTIPLE_STRINGS`, `ENUM`, `ENUM_SET`, `URI`, `FILEPATH`, `DATE`, `TIME`, `DATETIME`, `DURATION` |
| `DEFAULT`     | the default value; any JSON type                                                                                                          |
| `DESCRIPTION` | text for documentation and tools                                                                                                          |
| `PATTERN`     | a range, an enumeration, a named validator (e.g. `uint16`) or a regular expression                                                        |
| `ARITY`       | how many values the entry may hold                                                                                                        |
| `SECRET`      | `true` marks the entry as a secret                                                                                                        |
| `MANDATORY`   | the entry must have a value                                                                                                               |
| `FLAGS`       | list of flags; only `MANDATORY` is acted upon today                                                                                       |
| `WRITE_SCOPE` | the scope a `put()` without scope argument writes to                                                                                      |

Gotchas that cost real time:

- **The file is an array of entry objects**, each with its own `KEY`. An object keyed by
  configuration key parses to an *empty* schema, without an error.
- Field names are case-insensitive, by convention UPPER CASE.
- `DEFAULT` accepts any JSON type: `"DEFAULT": 8080`, `"DEFAULT": false` and `"DEFAULT": "8080"`
  all work; numbers and booleans are read as their text form. A JSON array is joined into a
  comma-separated value.
- Mark secrets with **`"SECRET": true`**, and only that. `"FLAGS": ["SECRET"]` parses (since
  0.8.15 with a WARNING at schema load) but does **not** mark the entry secret: no redaction,
  no `SecretValue`, no wiping.
- The flags `READ_ONLY`, `WRITE_ONLY` and `SENSITIVE` are parsed and kept, but not enforced yet;
  loading a schema that uses them logs a WARNING.
- `ALLOW_LOCAL_SCHEMA_OVERRIDE=false` locks an application to its bundled schema. The feature is
  deprecated since 0.8.17 and replaced in 0.9.0 by an allowlist of schema sources; it keeps
  working until then.
- Schemas stay in force in test mode.

## 3. Strictness: choosing the failure mode

Set with `ConfigFactoryBuilder.setFeature(...)`; all settings are described in
[Config Features](25_config_features.md).

| Feature                                  | Default | Effect                                                                                   |
|------------------------------------------|---------|------------------------------------------------------------------------------------------|
| `EXCEPTION_ON_MISSING_ENTRY`             | `true`  | a key with neither value nor default throws `ConfigException`; `false` returns `null`    |
| `EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND` | `false` | `true`: a missing configuration as a whole throws                                        |
| `SCHEMA_STRICT_MODE`                     | `false` | `true`: keys the schema does not declare are rejected                                    |
| `FALLBACKS_ACROSS_SCOPES`                | `true`  | a lookup continues into more general scopes; `false` restricts the fallback to `PRODUCT` |
| `WRITE_FALLBACK_ACROSS_SCOPES`           | `false` | `true`: a write may fall back to a more specific scope                                   |

- Schema `DEFAULT` values apply in either mode; `null` only comes back for a key with no value
  and no default.
- `DEFAULT_ON_MISSING_ENTRY` is deprecated since 0.8.17: it was never evaluated, and setting it
  has no effect. Do not generate it.
- Do not make up for a lenient setting with `if (v == null) v = FALLBACK;` at the call site.
  Declare the default in the schema.

## 4. Library self-configuration

Library settings can be shipped as a classpath resource of your component instead of code.
The file applies only to factories created for that company and application; values set in code win.

`src/main/resources/.config/<company>/<app>/mconfig.properties`

```properties
EVENTS_DETAIL_LEVEL=FAILURES_ONLY
UPDATE_CHECK_FREQUENCY_MS=2000
```

- Features marked `CODE_ONLY` (`ALLOW_MCONFIG_RUNTIME_SETTINGS`, `ENABLE_SELF_CONFIGURATION`,
  `ALLOW_APPLICATION_POLICY_LOCATIONS`, …) are ignored in this file, with a warning; set them in
  code. See [who may set a feature](25_config_features.md#who-may-set-a-feature).
- Do not use the global `.config/metabit/mConfig/mconfig.properties`: it is deprecated and will be
  removed before 0.9.0.
- Per-module settings go into their own file in the same directory (`vault.properties`,
  `aws.properties`, `kubernetes.properties`), which keeps the modules apart.
- `ENABLE_SELF_CONFIGURATION=false`, set in code, switches classpath self-configuration off
  where the classpath is not fully controlled.

## 5. Scope reference

Resolution runs from the most specific scope down; the first hit wins.
Details: [Scopes](20_scopes.md), [Priorities and Hierarchies](22_priorities_and_hierarchies.md).

| Priority | Scope          | Typical source                                      |
|----------|----------------|-----------------------------------------------------|
| 1 (top)  | `POLICY`       | Group Policy / enforced admin settings              |
| 2        | `RUNTIME`      | In-memory, set by your own code                     |
| 3        | `SESSION`      | Environment variables, CLI arguments                |
| 4        | `USER`         | `~/.config/`, `%APPDATA%`                           |
| 5        | `APPLICATION`  | Installation directory, portable root               |
| 6        | `HOST`         | `/etc/`, `%PROGRAMDATA%`                            |
| 7        | `CLUSTER`      | ZooKeeper, Kubernetes                               |
| 8        | `CLOUD`        | Cross-cluster shared configuration                  |
| 9        | `ORGANIZATION` | Licensee / tenant settings                          |
| 10       | `PRODUCT`      | Schema defaults, JAR resources                      |

- Do not register `POLICY`-scope locations from application code; outside test mode they fail
  unless `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code.
- Environment variables are read-only: a `put(..., ConfigScope.SESSION)` against them is refused.

## 6. Module selection

Artifact IDs are lower case; the `groupId` is `org.metabit.platform.support.config`.

| Bundle            | Use when                                                                                             |
|-------------------|------------------------------------------------------------------------------------------------------|
| `mconfigbasic`    | lightweight, purely file-based Java: core, schema, secrets, filesystem, JAR, `.properties`, util     |
| `mconfigstandard` | cloud-native: adds environment variables, JSON/YAML/TOML (Jackson), INI, slf4j, the Windows registry |

Vault, AWS Secrets Manager and Kubernetes are opt-in storage modules, in neither bundle. They are
not secrets providers; mConfig ships no secrets provider.

Packing everything into a single JAR must merge the `META-INF/services` files; never use the
assembly descriptor `jar-with-dependencies`. See
[Packaging as a single JAR](13_getting_started.md#138-packaging-as-a-single-jar).

---

Related: [Getting Started](13_getting_started.md) ·
[Examples](examples/index.md) ·
[AI Guidance](52_ai_guidance.md)
