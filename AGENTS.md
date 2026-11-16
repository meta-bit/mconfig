# AGENTS.md - mConfig AI Agent Guide

**Read this first.**

- **Authority**: the source code is authoritative. Where this file and the documentation under
  `documentation/` disagree, this file wins; where this file and the code disagree, the code wins
  and this file needs fixing.
- **Do not guess the API.** Use the routing map below to read the relevant file before writing code.
- **Before changing mConfig itself**, read the working protocol in Section 7.

### Intent Routing Map

| If the task concerns                                        | Read                                                               |
|:------------------------------------------------------------|:-------------------------------------------------------------------|
| Core architecture, thread-safety, late binding              | `documentation/src/site/markdown/44_design_consolidated.md`        |
| Scope precedence and resolution order                       | `documentation/src/site/markdown/22_priorities_and_hierarchies.md` |
| Schemas: fields, validation, defaults                       | `documentation/src/site/markdown/23_configuration_schemas.md`      |
| Secrets and confidentiality model                           | `documentation/src/site/markdown/26_secrets_concept.md`            |
| Code templates to copy (env vars, hot-reload, secrets)      | `examples/snippets.md`                                             |
| Exact API names, schema fields, settings, scopes            | `documentation/src/site/markdown/snippets.md`                      |
| Migrating code to mConfig; reviewing code that uses it      | `documentation/src/site/markdown/38_code_improvements.md`          |
| Zero-config boilerplates (Maven/Gradle/JPMS)                | `examples/starters/README.md`                                      |
| CLI commands and diagnostics (live-tail, auditing)          | `mConfigTools/README.md`                                           |
| Test isolation                                              | `documentation/src/site/markdown/15_test_mode.md`                  |
| Known code-generator pitfalls                               | `documentation/src/site/markdown/52_ai_guidance.md`                |

### Quick Answers

Short pointers for common requests. "Templates" is `examples/snippets.md`; its Java blocks are
compiled against the current API on every build.

- **Which dependency?** `mconfigbasic` for lightweight, file-based Java projects; `mconfigstandard`
  for environment variables, JSON/YAML/TOML, INI, slf4j or the Windows registry (Section 2.3).
- **Minimal working code:** Templates, section 2 (bootstrap); complete projects in `examples/starters/`.
- **Defaults and validation:** a `<config>.mconfig-schema.json` next to the defaults file; never at
  the call site (Templates, section 3).
- **Override a value per deployment or container:** environment variable
  `<APP>_<CONFIG>_<KEY>` with `mconfigsourceenvvar`; `/` becomes `_`, a literal `_` becomes `__`
  (Templates, section 4).
- **Hot reload, also for Kubernetes ConfigMaps:** read values on use; `subscribeToUpdates()` only to
  react (Templates, sections 5 and 6).
- **Passwords and tokens:** `"SECRET": true` in the schema plus `getSecret()`; there is no Secret
  entry type (Section 4.3; Templates, section 7).
- **Tests that leave the machine's configuration alone:** `builder.setTestMode(true)`, not chained
  (Templates, section 9).
- **"Where does this value come from?"** `mconfig <company>:<app>:<config> get <key> -v`, or
  Templates, section 10.
- **Replace hand-written loading, parsing, path search or file watching:**
  `documentation/src/site/markdown/38_code_improvements.md`, part 1.
- **Single JAR:** merge `META-INF/services`; never `jar-with-dependencies` (Section 2.3).

## 1. Project Overview
**mConfig** is a modular, 10-tier hierarchical configuration library for Java. It is designed to be cross-platform (Linux, Windows, Android, embedded) and agnostic of specific storage backends or formats.

### Core Philosophy
- **Layered Stack**: Configurations are resolved through a stack of layers: `Cache` -> `Sources` (ordered by scope) -> `Defaults`.
- **Scope-Based Priority**: Scopes define precedence (e.g., `POLICY` > `USER` > `PRODUCT`).
- **Separation of Concerns**: Decouples `ConfigStorage` (where it is) from `ConfigFormat` (how it is parsed).
- **Extension Features**: Supports module-specific settings (e.g., `VaultFeatures`) that are decoupled from the core library but still type-safe.
- **Immutability & Safety**: Prefers immutable configuration structures and safe, typed access.
- **Embedded-Friendly**: `mConfigCore` is designed to work even on systems without a traditional filesystem.

---

## 2. Quick Start for Application Developers

### 2.1 Bootstrapping mConfig
The entry point is `ConfigFactoryBuilder`. Use it to configure library features and build a `ConfigFactory`.

`ConfigFactory` is `AutoCloseable` - always use try-with-resources, or call `close()` explicitly.

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "demoApp").build())
    {
    // Get a configuration handle
    Configuration config = factory.getConfig("network");

    // Read typed values
    int    port = config.getInteger("server/port");
    String host = config.getString("server/host");
    }
```

**Key separator**: use `/` to address hierarchical structure (`server/port`). A dot is *not* a
separator - in flat formats such as `.properties` a dot is a literal part of the key.

Most builder methods chain: all `setFeature(...)` variants, `setSchemas()`, `addSource()`,
`addSecretsProvider()` etc. return the builder. **Not** chainable (they return `void`): `setTestMode()`,
`setTestConfigPaths()`, `setTestParameters()`, and the static `forbidTestMode()` / `permitTestMode()`.

### 2.2 Recommended Resource Layout
Place your default configurations and schemas in your Maven/Gradle resource folder:
- `src/main/resources/.config/<company>/<app>/<config>.<ext>`
- `src/main/resources/.config/<company>/<app>/<config>.mconfig-schema.json`

e.g., `src/main/resources/.config/metabit/demoApp/network.properties` and `src/main/resources/.config/metabit/demoApp/network.mconfig-schema.json`.

### 2.3 Module Selection
- `mconfigbasic`: lightweight, purely file-based Java projects. Core, schema, secrets, filesystem, JAR, java-properties, util.
- `mconfigstandard`: cloud-native environments needing environment variables, JSON/YAML/TOML (Jackson), INI, slf4j, or the Windows registry.

Artifact IDs are lowercase; `groupId` is `org.metabit.platform.support.config`.

**Single ("fat") JAR packaging** must merge `META-INF/services` files. Never generate the assembly
descriptor `jar-with-dependencies`; use the merging descriptor, shade's `ServicesResourceTransformer`, or
Shadow's `mergeServiceFiles()` (`examples/snippets.md`, section 1). Unmerged packaging fails at `build()` with
`JAVA_SERVICE_CONFIGURATION_ERROR`, or loses sources silently.

### 2.4 Dependency Versions
Documentation and examples may carry placeholders such as `${mconfig.version}`, or a static
version that has since aged. Do **not** copy either literally into a user's project.

1. Read the actual version from the repository's own `pom.xml`, the user's build file, or the `.m2` cache.
2. Use that version in generated build snippets.
3. If none can be resolved, state so and ask - do not invent a version, and do not carry a
   hardcoded baseline forward from documentation.

---

## 3. Core Concepts

### 3.1 The 10-Tier Scope Hierarchy
Priority from Highest (1) to Lowest (10):
1.  **`POLICY`**: Enforced settings (e.g., GPO).
2.  **`RUNTIME`**: Volatile, in-memory settings (highest normal override).
3.  **`SESSION`**: Environment variables or CLI arguments.
4.  **`USER`**: Personal user settings (e.g., `~/.config/`).
5.  **`APPLICATION`**: App-specific portable root settings.
6.  **`HOST`**: System-wide settings (e.g., `/etc/`).
7.  **`CLUSTER`**: Distributed configuration (e.g., ZooKeeper).
8.  **`CLOUD`**: Shared across multiple clusters.
9.  **`ORGANIZATION`**: Settings shared across an organization.
10. **`PRODUCT`**: Hardcoded defaults (from JAR resources).

### 3.2 mConfig Schema
The Schema is a core feature (like the 10-tier hierarchy) that provides type safety, validation, and documentation for configuration entries.

- **Role**: Defines expected keys, their types, default values, and validation rules.
- **Discovery**: Automatically discovered from `.config/` directories in the classpath and prioritized OS-specific locations (e.g., `/etc/mconfig/schemas`, `~/.config/mconfig/schemas`).
- **Naming**: JSON files named `<config>.mconfig-schema.json`.
- **Shape**: a JSON **array of entry objects**, each carrying its own `KEY`. An object keyed by
  configuration key parses to an *empty* schema without raising an error.
- **Core Fields**: `KEY`, `TYPE`, `DEFAULT`, `DESCRIPTION`, `PATTERN`, `ARITY`, `SECRET`, `MANDATORY`, `FLAGS`.
- **Spelling**: it is `Schema` / `ConfigSchema` / `SCHEMA_*`, never `Scheme`.
- **Flag enforcement**: of the `FLAGS` values, only `MANDATORY` is acted upon today. `READ_ONLY`,
  `WRITE_ONLY` and `SENSITIVE` are parsed and retained but not yet enforced; since 0.8.15, loading a
  schema that uses them logs a WARNING.
- **Detailed Documentation**: [Configuration Schemas](documentation/src/site/markdown/23_configuration_schemas.md).

---

## 4. Application Development Guidelines

### 4.1 Value Access & Conversion
- **Use typed getters**: Prefer `getInteger()`, `getBoolean()`, `getEnum()`, etc. from the `Configuration` facade.
- **Use Features**: Set library-wide settings (core or extension) via `ConfigFactoryBuilder.setFeature()`. Prefer constants (`ConfigFeature.TEST_MODE`, `VaultFeatures.BOOTSTRAP_CONFIG_NAME`) for type safety.
- **Self-configuration is per component**: put library settings into
  `src/main/resources/.config/<company>/<app>/mconfig.properties`; it applies only to factories for that
  company/app. Code wins over the file. The global `.config/metabit/mConfig/mconfig.properties` is deprecated
  (removed before 0.9.0) - do not generate it.
- **Setting levels**: features marked `CODE_ONLY` (e.g. `ALLOW_MCONFIG_RUNTIME_SETTINGS`,
  `ALLOW_APPLICATION_POLICY_LOCATIONS`) can only be set in code; `COMPONENT` ones (e.g. `TEST_MODE`) also via
  runtime settings or the component file. See `ConfigFeature.SettingLevel` and
  [Config Features](documentation/src/site/markdown/25_config_features.md#who-may-set-a-feature).
- **POLICY locations**: do not register POLICY-scope locations from application code; outside test mode they fail
  unless `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code.
- **Do NOT parse or validate manually**: mConfig handles conversion, schema-based defaults, and mandatory field validation. 
- **Use mConfig Features**: Always prefer library features (Schemas, `MANDATORY` flags, `DEFAULT` values in JSON) over manual code checks or hardcoded fallbacks.
- **No defaults at the call site**: never generate `properties.get("key", "default")`,
  `getString("key", fallback)` or equivalent. Name, constraints and default value belong in a
  central `ConfigSchema`.
- **Avoid Redundant Fallbacks**: Before implementing patterns like `if (val == null) val = DEFAULT`, verify if mConfig already provides the default via the 10-tier hierarchy or `ConfigFeature` defaults.
- **Defaults**: Do not hardcode defaults in Java. Use Schemas or resource files (Section 2.2).

### 4.2 Error Handling
- **`ConfigException` (Unchecked)**: Thrown for severe issues or logic errors where recovery is impossible (e.g., invalid schema, missing mandatory entry).
- **`ConfigCheckedException`**: Used for operations that might fail due to external factors (e.g., writing to a read-only location).
- **Rule of Thumb**: If the library can fall back to a default (e.g. a schema `DEFAULT`), it returns that. Without one, a missing entry throws `ConfigException`: `EXCEPTION_ON_MISSING_ENTRY` defaults to `true`.

### 4.3 Handling Secrets
Security-relevant, and easy to get subtly wrong. There is no "Secret" entry type - do not invent one.

- **Mark**: set `"SECRET": true` on the entry in its schema.
    - **`"FLAGS": ["SECRET"]` does NOT work.** It parses (since 0.8.15 with a WARNING at schema
      load), but the flag is never consulted; all secret handling reads the separate `SECRET`
      property. Using the flag alone leaves the value unprotected - no redaction, no `SecretValue`,
      no wiping.
    - Secrecy may also originate from the source itself (Vault, AWS Secrets Manager, Kubernetes),
      with no schema involved.
- **Retrieve**: use `config.getSecret("key")`, which returns a `SecretValue`.
- **Usage**: access raw data via `getValue()` (bytes) or `getChars()`, and call `erase()` after
  use to wipe it from memory. Never map a credential as a plain `String`.
- **Logging**: secret values are automatically redacted (`[REDACTED]`) in logs and `toString()` outputs.
- **Scope of protection**: mConfig prevents *accidental disclosure*. It does not protect secrets in
  process memory (heap dumps, debuggers, same-process code); do not describe it as doing so.
  Wipe-on-close covers in-memory, env-var, Vault, AWS and Kubernetes layers, not file layers yet.
- **Sources**: Vault, AWS Secrets Manager and Kubernetes are opt-in *storage modules*, not secrets
  providers. `ConfigSecretsProviderInterface` / `addSecretsProvider()` is an extension point; mConfig
  ships no provider - do not generate code that assumes one (e.g. an ID "hashicorp-vault").
- **Where values may go**: a secret stays intact in its own storage (writing back to the same file
  keeps it), but never goes into another context: `ConfigMapper.saveTo()` leaves it out, CLI and
  tool output show the string marker `[REDACTED]` in every format, and plain text is given out only
  on an explicit single-value request (`mconfig get <key> --reveal`). Use
  `SecretRedaction.isConfidential(entry)` when writing new output paths. Details:
  [Where secret values may appear](documentation/src/site/markdown/26_secrets_concept.md#where-secret-values-may-appear).
- **`SECRET` vs `SENSITIVE`**: `SECRET` is for cryptographic secrets and credentials; `SENSITIVE`
  is for information that must not become visible where it does not belong, typically personal
  data. Most secrets are also sensitive, rarely the reverse. `SENSITIVE` is **defined but not yet
  enforced** - do not rely on it. Both suppress the *value* only: the storage location of a
  protected entry stays traceable via `getScope()`, `getLocation()` and `getURI()`, deliberately,
  so data remains findable, correctable and erasable.
- **Detailed Documentation**: [Secrets Concept](documentation/src/site/markdown/26_secrets_concept.md).

### 4.4 Testing (Test Mode)
- **Isolation**: Use `ConfigFactoryBuilder.setTestMode(true)` in tests to bypass actual OS paths and prevent interference with real user data.
- **Test Resources**: Place test-specific configs in `src/test/resources/.config/`.
- **Detailed Documentation**: [Test Mode](documentation/src/site/markdown/15_test_mode.md).

---

## 5. Design Decisions & Reference

This section applies to mConfig library development, not to the application developer.

Some references below point into the internal `devdocs/` tree. That tree is **not** part of the
public distribution, so it is cited by name rather than linked; if it is not present in your
checkout, the material is unavailable and should not be linked to from public documentation.

### 5.1 Code Style
mConfig uses **Moderate Whitesmiths style**. 
- **Tenet**: Opening and closing braces MUST be in the same column, OR in the same row.
- **Full Rules**: see `devdocs/formatting.md` (internal).
- **Architectural Rationales**: see `devdocs/architectural_rationales.md` (internal) for justifications of specific design patterns (e.g., inner classes) against static analysis tools.

### 5.2 Library Internals
- **Self-Configuration**: mConfig modules MUST use the library's own configuration hierarchy for their internal settings. 
    - **Avoid Direct Access**: Never use `System.getProperty()` or `System.getenv()` directly for module settings, including during bootstrap.
    - **Use mConfig Lookups**: retrieve typed settings through the context handed to `postInit()`.
      Mind which context you get: `ConfigStorageInterface.postInit()` receives a
      `BootstrapConfigContext`, which offers only `getRestrictedConfig(name, scopes)` and
      `getLogger()` - there is no `getFactory()` on it. `ConfigFactoryComponent`,
      `ConfigLoggingInterface` and `ConfigSecretsProviderInterface` still receive a `ConfigFactory`.
    - **Avoid Self-Reads**: a storage module must not read its own not-yet-initialized scope. The
      cloud storages (Vault, AWS, ZooKeeper) restrict themselves to `EnumSet.of(PRODUCT, HOST, USER, APPLICATION)`.
      **Exception: Kubernetes** also reads SESSION and RUNTIME, deliberately. In a pod the environment
      is deployment configuration set by the pod spec, and RUNTIME carries intentional overrides from code;
      neither scope is served by a source depending on the Kubernetes storage, so there is no circular read.
    - **Scope Mapping**: If environment variables or system properties are required, map them to the `SESSION` scope using `mConfig`'s standard providers.
    - **Module Isolation**: Source modules should use separate configuration handles (e.g., `vault`, `aws`, `kubernetes`) and separate files (e.g., `vault.properties`) to maintain modularity. A bootstrap configuration is an ordinary configuration **of the application** (company/app), resolved through the usual layers and storages within the scopes the module reads: bundled defaults (`src/main/resources/.config/<company>/<app>/vault.properties`), host- or user-wide files, other storages, values the application stores for later starts. It is **not** read from mConfig's internal `.config/metabit/mConfig/` namespace, so each component that uses mConfig configures its own modules. Modules read it once, in `postInit()`.
- **No DI Frameworks**: Core avoids Spring/Guice; uses simple Constructor/Builder patterns.
- **Logging**: Uses `ConfigLoggingInterface` (no `System.out`).
    - Text from outside the library (file names, OS- or environment-derived paths, names from remote
      services) goes into messages through `LogSafe.text(value)` (`impl.logging`), which escapes
      control, line-separator and bidi characters (CWE-117). Never put outside text into the format
      string of the format variants; pass it as an argument.
- **Formats and storages** (since 0.8.17): `ConfigFileFormatInterface.readFile()` / `readStream()` return a layer
  or throw `ConfigParseException`, never `null`, and do not log. The storage catches it and calls
  `ParseProblemReporter.report(...)`, which withholds the parser's text where it may quote a secret value. Never log
  `getParserDetail()` / `getParserCause()` yourself. Contract: `devdocs/library_development.md` 2.3 (internal).
- **Internal JSON Parsing**: Use mConfig's built-in `JsonStreamParser` (from `mConfigSchema` module) for lightweight internal JSON parsing needs. Avoid adding external dependencies like Jackson if possible.
- **Library Development**: for extending mConfig (new storages/formats), see `devdocs/library_development.md` (internal).

---

## 6. Glossary
- **`ConfigLocation`**: Pointer to "where" a config might be (Storage + Handle + Scope).
- **`ConfigSource`**: Instantiated `ConfigLocation` bound to a `ConfigLayer`.
- **`ConfigLayer`**: A concrete map of data from a source.
- **`ConfigEntry`**: A leaf node (value + metadata).
- **`ConfigFacade`**: The high-level API (`Configuration` interface).
- **`ConfigFeature`**: A configuration flag for the mConfig library itself.

---

## 7. Working Protocol

Applies to any agent making changes to mConfig itself.

### 7.1 Analysis First
A request for **analysis**, design discussion, or impact assessment is **NOT** an implicit command
to implement.

1. **Analyze** - work through the issue or feature request properly.
2. **Discuss** - share findings, options and side effects. Ask clarifying questions here.
3. **Approve** - wait for explicit approval of a specific path.
4. **Implement** - change code only after that approval.

### 7.2 Discussion and Inquiry
- **Ask open questions** when architectural implications are unclear.
- **Present options** with pros and cons when several paths are viable (library choice,
  implementation style, data structure).
- **Clarify constraints** rather than assuming project or organizational preferences.
- **No assumptions**: if a requirement is ambiguous, ask before implementing.

### 7.3 Breaking Changes
Changes to file extensions, discovery logic, or public APIs always require explicit confirmation:
they propagate into every project using mConfig. Treat them as a discussion item, never as an
incidental part of a larger change.

### 7.4 Verification
- Prefer specialized file and search tools over general terminal commands.
- `examples/` is **not** in the Maven reactor and is not built by a normal `mvn` run.
  `ExampleSourcesCompileTest` in `integrationtests/integrationtestsBase` compiles it in-process;
  run it after touching example sources.
- Do not add checks that start emulators or runtime environments to the default test lifecycle.
  Keep verification hermetic, or gate it behind a profile.
