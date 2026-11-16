# 2.5 Config Features

ConfigFeature flags tune how a ConfigFactory is built and how it behaves.
Set them on the ConfigFactoryBuilder before calling `build()`.

mConfig uses a two-tier system for features:
1.  **Core Features**: Standard settings defined in the `ConfigFeature` enum (e.g., `TEST_MODE`, `LOGLEVEL_NUMBER`).
2.  **Extension Features**: Module-specific settings (e.g., `VaultFeatures.BOOTSTRAP_CONFIG_NAME`) that are automatically registered when the module is on the classpath.

Example using both:
```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        // Core features (using enum)
        .setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true)
        .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
        // Extension feature (using module-specific class)
        .setFeature(VaultFeatures.BOOTSTRAP_CONFIG_NAME, "custom-vault");
```

## Config Feature Registry

mConfig maintains a central `ConfigFeatureRegistry` that handles all features case-insensitively. This allows you to set features using their string names, which is particularly useful if you want to avoid a compile-time dependency on a specific extension module:

```java
// Using a string key for a core feature
builder.setFeature("ALLOW_MCONFIG_RUNTIME_SETTINGS", true);

// Using a string key for an extension feature
builder.setFeature("VAULT_BOOTSTRAP_CONFIG_NAME", "custom-vault");
```

If a feature is not yet registered (e.g., because the module hasn't been initialized), the value is stored as a raw property and validated once the module attempts to access it.

Notes:
- Booleans default to `false` unless a default is explicitly listed below.
- Extension features are only available when the corresponding module is on the classpath.
- Runtime environment variables can override a subset of settings if
  `ALLOW_MCONFIG_RUNTIME_SETTINGS` is enabled. See "Runtime overrides" below.
- Security-relevant features can only be set in code, or by the component itself.
  See "Who may set a feature" below.

## Self-configuration per component

An application often contains several components that use mConfig, each building its own
`ConfigFactory` with its own company and application name. Each component can ship its own settings
as a classpath resource:

```
src/main/resources/.config/<company>/<application>/mconfig.properties
```

(without a company name: `.config/<application>/mconfig.properties`). Keys are feature names, values are
strings; lists are comma-separated:

```properties
COMMENTS_READING=true
FILE_FORMAT_READING_PRIORITIES=YAML,properties
```

The file applies **only** to factories created with that company and application name, so a component
configures itself without changing the behaviour of the other components or of the main application.

Precedence, highest first: **code** (`setFeature()`), **runtime settings** (if delegated, see below),
the **component file**, the deprecated **global file**. A file only fills in features that are not set yet.

Self-configuration is read during `build()`, before the test-mode gates are evaluated. Switch it off with
`ConfigFeature.ENABLE_SELF_CONFIGURATION=false` (in code).

**Deprecated: the global file** `.config/metabit/mConfig/mconfig.properties`. It applies to every factory in the
process and cannot be attributed to a component; arbitrary resources on the classpath may come from any dependency.
It is still read in 0.8.x, with a WARNING naming its location, may only set ordinary features (level `ANY`),
and **will be removed before 0.9.0**. Move its settings to the component file.

## Who may set a feature

Features reach the `ConfigFactory` through these channels:

| Channel                | How                                                                                       | Who decides                                     |
|------------------------|-------------------------------------------------------------------------------------------|-------------------------------------------------|
| **Code**               | `ConfigFactoryBuilder.setFeature()`                                                       | the component (or application)                  |
| **Delegated**          | runtime settings: `MCONFIG_RUNTIME_*` environment variables, `-Dmconfig.runtime.*` system properties | whoever starts the process, after the component enabled `ALLOW_MCONFIG_RUNTIME_SETTINGS` in code |
| **Component file**     | `.config/<company>/<application>/mconfig.properties` on the classpath                     | the component's author                          |
| **Global file** (deprecated) | `.config/metabit/mConfig/mconfig.properties` on the classpath                       | any JAR or directory on the classpath           |

Each feature has a **setting level** (`ConfigFeatureInterface.getSettingLevel()`) that says which
channels may set it:

| Level       | Code | Delegated | Component file | Global file |
|-------------|:----:|:---------:|:--------------:|:-----------:|
| `CODE_ONLY` | yes  | no        | no             | no          |
| `COMPONENT` | yes  | yes       | yes            | no          |
| `ANY`       | yes  | yes       | yes            | yes         |

A channel that may not set a feature ignores the value and logs a WARNING. Most features are `ANY`;
module-specific features are `ANY` unless the module says otherwise. The restricted ones:

| Feature                              | Level       | Why                                                                          |
|--------------------------------------|-------------|------------------------------------------------------------------------------|
| `ALLOW_MCONFIG_RUNTIME_SETTINGS`     | `CODE_ONLY` | it *is* the delegation to the environment; that is a decision for code       |
| `ENABLE_SELF_CONFIGURATION`          | `CODE_ONLY` | a file cannot decide whether files are read                                  |
| `ALLOW_APPLICATION_POLICY_LOCATIONS` | `CODE_ONLY` | see [POLICY locations](#policy-locations-registered-by-the-application)      |
| `ADDITIONAL_MODULE_PATHS`            | `CODE_ONLY` | it loads code                                                                |
| `TEST_MODE`                          | `COMPONENT` | each component is responsible for its own test mode                          |
| `TESTMODE_DIRECTORIES`               | `COMPONENT` | belongs to test mode                                                         |
| `PERMIT_TEST_MODE`                   | `COMPONENT` | belongs to test mode; code wins, so a file cannot undo a lockdown set in code |
| `ALLOW_INCOMPLETE_SERVICE_REGISTRATION` | `COMPONENT` | a component accepts its own packaging fault, not every component's      |
| `LOG_PARSE_ERROR_DETAILS`            | `CODE_ONLY` | may put secret values into the log; an explicit decision of the developer     |
| `LENIENT_BOOLEAN_CONVERSION`         | `COMPONENT` | how a component reads its own files, not every component's                   |

**Why "delegated" is different from a classpath file.**
Runtime settings only work after the component enabled them in code, so it has explicitly handed these
decisions to whoever starts the process (a CI pipeline, an operator). That is why a file can never enable them.

**Test mode and the main application.** Each component is responsible for its own testing and test mode;
it may switch test mode on in code, through runtime settings, or in its own file. A component file that
requests test mode is logged as a WARNING with the file's location, so an accidentally shipped file is visible
at once. The main application keeps control over all components: `ConfigFactoryBuilder.forbidTestMode()` locks
test mode off for the whole JVM, and `PERMIT_TEST_MODE=false` per factory (see [Test Mode](15_test_mode.md)).

## Runtime overrides

If `ALLOW_MCONFIG_RUNTIME_SETTINGS` is set to `true` in code, mConfig accepts runtime
settings via environment variables with the prefix `MCONFIG_RUNTIME_`, and via system properties
(`-Dmconfig.runtime.test_mode=true`, or verbatim `-DMCONFIG_RUNTIME_TEST_MODE=true`; the system property wins):

- `MCONFIG_RUNTIME_DEBUG_LEVEL` -> `LOGLEVEL_NUMBER` (accepts numbers or: `off`, `error`, `warn`, `info`, `debug`, `trace`)
- `MCONFIG_RUNTIME_DEBUG_OUTPUT` -> `LOGGING_REDIRECT_TARGET`
- `MCONFIG_RUNTIME_DISABLE_MODULES` -> `DISABLED_MODULE_IDS` (comma-separated)
- `MCONFIG_RUNTIME_TEST_MODE` -> `TEST_MODE`
- `MCONFIG_RUNTIME_KUBERNETES_*` -> Kubernetes module paths and endpoint (see the module README)
- `MCONFIG_RUNTIME_MODULES` -> `ADDITIONAL_MODULE_PATHS`: **refused** since 0.8.15, as the feature is `CODE_ONLY`.

## POLICY locations registered by the application

POLICY is meant for administrator-controlled locations, which the storages derive from the operating
system (e.g. `/etc/...`, the Windows registry policy keys). In productive mode, mConfig refuses POLICY
locations that the application registers itself, with a `ConfigException` (`ARGUMENT_REFUSED`) at `build()`:

- a `POLICY:` prefix in `ADDITIONAL_RUNTIME_DIRECTORIES` or `ADDITIONAL_USER_DIRECTORIES`;
- a secrets provider in `ADDITIONAL_SECRETS_PROVIDERS` with `"scope": "POLICY"`;
- a POLICY-scope source added with `ConfigFactoryBuilder.addSource()`.

They are allowed in test mode, and with `ALLOW_APPLICATION_POLICY_LOCATIONS` set in code, for
applications that legitimately ship their own policy location (e.g. administration tools).

## Feature reference

### Test mode
- `PERMIT_TEST_MODE` (Boolean, default: true; level `COMPONENT`): Master gate for test mode usage.
- `TEST_MODE` (Boolean, default: false; level `COMPONENT`): Activate test-mode search locations.
- `TESTMODE_DIRECTORIES` (`List<String>`, default: empty; level `COMPONENT`): Scoped paths in `SCOPE:PATH` format.
- `TESTMODE_PARAMETERS` (`Map<String,String>`): Additional test parameters.

### Logging
- `QUIET` (Boolean, default: false): Reduce internal mConfig logging.
- `LOGGING_TO_USE_IN_CONFIGLIB` (String, default: ""): Force a specific logging module.
- `LOGLEVEL_NUMBER` (Integer, default: 4): Numeric log level for basic loggers.
- `LOGGING_REDIRECT_TARGET` (String): Redirect internal logging (`stderr`, `stdout`, `quiet`, `file:<path>`).

### Filesystem and registry paths
- `ADDITIONAL_RUNTIME_DIRECTORIES` (`List<String>`): Extra filesystem directories (prepended) for `RUNTIME` scope.
- `ADDITIONAL_USER_DIRECTORIES` (`List<String>`): Extra filesystem directories (prepended) for `USER` scope.
  Entries of both may carry a scope prefix (`HOST:/opt/app/conf`); `POLICY:` needs `ALLOW_APPLICATION_POLICY_LOCATIONS`
  in productive mode (see [POLICY locations](#policy-locations-registered-by-the-application)).
- `FILENAME_EXTENSION_MAPPINGS` (`List<String>`): Extra filename extensions to try when reading config files.
- `DEFAULT_TEXTFILE_CHARSET` (`String`, default: `UTF-8`): Charset for text formats.
- `TRIM_TEXTVALUE_SPACES` (Boolean, default: true): Trim leading/trailing spaces in text values.
- `LENIENT_BOOLEAN_CONVERSION` (Boolean, default: false; component; since 0.8.17): Read Boolean values as
  `Boolean.parseBoolean()` does: `true` in any case is true, every other value, typing errors included, is false.
  By default `getBoolean()` accepts `true/false`, `yes/no`, `on/off` and `1/0` (any case, surrounding blanks) and
  throws `ConfigException` (`CONVERSION_FAILURE`) for anything else, an empty value included. For compatibility with
  files written for the plain Java behaviour. **Limitation:** it acts in the `Configuration` getters only. Code
  that converts a `ConfigEntry` itself (`getValueAsBoolean()`), the ConfigMapper, and the schema check of `BOOLEAN`
  entries always convert strictly. Boolean features themselves (in `mconfig.properties`, `setTestParameters()`)
  are always read strictly as well.
- `WRITE_CONDENSED_FORMAT` (Boolean, default: false): Prefer compact output when writing text formats.

### Format priorities
- `FILE_FORMAT_READING_PRIORITIES` (`List<String>`, default: `DHALL,TOML,YAML,JSON5,JSON,ASN1,properties,INI`): Read order.
- `FILE_FORMAT_WRITING_PRIORITIES` (`List<String>`, default: `TOML,YAML,JSON5,JSON,properties`): Write order.
- `FILE_FORMAT_READING_ALLOW_ALL_FORMATS` (Boolean, default: true): Fallback to non-listed formats.
- `FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS` (Boolean, default: true): Fallback to non-listed formats.

### Storage type priorities
- `STORAGE_TYPE_PRIORITIES` (`List<String>`, default: `RAM,secrets,vault,aws-secrets,files,registry,registryjni,zookeeper,JAR`): Init/search order within a scope.
- `STORAGE_TYPE_ALLOW_ALL_STORAGES` (Boolean, default: true): Allow storages not explicitly listed. Set to `false` to use only the storages covered by `STORAGE_TYPE_PRIORITIES`; the others are not initialised and read nothing. An entry covers the storage ID itself and the IDs it prefixes up to a `-` or `.` (`RAM` covers `RAM-RUNTIME` and `RAM-SESSION`, `secrets` covers `secrets.<provider>`). Core's own storages (`defaults`, `RAM-*`) are always used. Storages missing from the default list, such as `envvar` (environment variables) or `kubernetes`, must be added explicitly. Sources added with `addSource()` are not affected. The INFO line at factory start lists the storages in use.

### Schemas and defaults
- `CONFIG_SCHEMA_LIST` (`Map<String,ConfigSchema>`): Provide schemas programmatically.
- `SCHEMA_STRICT_MODE` (Boolean, default: false): Reject keys not in the schema.
- `SCHEMA_RESETS_DEFAULTS` (Boolean, default: false): Replace existing defaults instead of merging.

Configurations without a schema are allowed by default; there is no feature flag to switch
that on. Set `SCHEMA_STRICT_MODE` to true if you want unschemad keys rejected instead.

### Missing entry behavior
- `EXCEPTION_ON_MISSING_ENTRY` (Boolean, default: true): Throw when a key is absent.
- `DEFAULT_ON_MISSING_ENTRY` (Boolean): deprecated since 0.8.17, never evaluated, removed in 0.9.0. Schema
  `DEFAULT` values apply anyway; with `EXCEPTION_ON_MISSING_ENTRY=false`, a key with neither value nor default
  returns `null`.
- `EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND` (Boolean, default: false): Throw if a configuration name is not found at all.
- `FALLBACKS_ACROSS_SCOPES` (Boolean, default: true): Allow fallback to less specific scopes on reads.
- `WRITE_FALLBACK_ACROSS_SCOPES` (Boolean, default: false): Allow fallback to more specific scopes on writes.

### Cache and update checks
- `UPDATE_CHECK_FREQUENCY_MS` (Integer, default: 2000): Polling interval for cached sources.
- `UPDATE_CHECK_SCOPES` (`List<String>`, default: all scopes): Limit update checks by scope.
- `CACHE_CONFIGS` (Boolean): Cache configurations in memory.
- `AUTOMATIC_CONFIG_LOADING` (Boolean): Auto-load configs before first read (experimental).
- `AUTOMATIC_CONFIG_CREATION` (Boolean): Auto-create configs on write (experimental).
- `WRITE_SYNC` (Boolean): Sync writes immediately (storage-dependent).
- `NO_DEFAULT_DIRECTORIES` (Boolean): Disable default filesystem paths (experimental).

### Modules and runtime configuration
- `ALLOW_MCONFIG_RUNTIME_SETTINGS` (Boolean, default: false; code only): Allow `MCONFIG_RUNTIME_*` env overrides.
- `ENABLE_SELF_CONFIGURATION` (Boolean, default: true; code only): Allow classpath self-configuration.
- `ADDITIONAL_MODULE_PATHS` (`List<String>`; code only): Add module IDs/paths during discovery.
- `DISABLED_MODULE_IDS` (`List<String>`): Disable specific module IDs.
- `ALLOW_INCOMPLETE_SERVICE_REGISTRATION` (Boolean, default: false; component): Start even though core's
  service registrations were lost to faulty single-JAR packaging; the report is logged as a WARNING instead of
  failing `build()`. Fix the packaging instead; see [Packaging as a single JAR](13_getting_started.md#138-packaging-as-a-single-jar).
- `LOG_PARSE_ERROR_DETAILS` (Boolean, default: false; code only; since 0.8.17): Add the parser's own message to
  every parse error report, also where the content may hold secret values. For development; by default the message
  is withheld for secret stores, cloud bootstrap configurations and schemas with `SECRET` entries at, above or below
  the error position. See [Logging: parse errors](42_logging.md#parse-errors).

### Scopes
- `ALLOW_APPLICATION_POLICY_LOCATIONS` (Boolean, default: false; code only): Allow the application to register
  POLICY-scope locations in productive mode. See [POLICY locations](#policy-locations-registered-by-the-application).

### Secrets providers (mConfigSecrets)
- `SECRETS_PROVIDER_ID` (String): Primary provider ID.
- `SECRETS_PROVIDER_CONFIG` (`Map<String,Object>`): Provider configuration.
- `ADDITIONAL_SECRETS_PROVIDERS` (`List<Map<String,Object>>`): Additional providers with optional scope;
  scope `POLICY` needs `ALLOW_APPLICATION_POLICY_LOCATIONS` in productive mode.

### ZooKeeper (mConfigSourceZooKeeper)
- `ZOOKEEPER_CONNECT_STRING` (String): Connection string.
- `ZOOKEEPER_ROOT_PATH` (String, default: `/mconfig`): Root path.
- `ZOOKEEPER_SESSION_TIMEOUT_MS` (Integer, default: 60000): Session timeout.
- `ZOOKEEPER_RETRY_BASE_SLEEP_MS` (Integer, default: 1000): Retry base sleep.
- `ZOOKEEPER_RETRY_MAX_RETRIES` (Integer, default: 3): Retry count.
- `ZOOKEEPER_BOOTSTRAP_CONFIG_NAME` (String, default: `zookeeper`): Bootstrap config name.
  (Available via `ZooKeeperFeatures` in `mConfigSourceZooKeeper`)

### Vault (mConfigSourceVault)
- `VAULT_BOOTSTRAP_CONFIG_NAME` (String, default: `vault`): Bootstrap config name.
  (Available via `VaultFeatures` in `mConfigSourceVault`)

### AWS Secrets Manager (mConfigSourceAwsSecretsManager)
- `AWS_BOOTSTRAP_CONFIG_NAME` (String, default: `aws`): Bootstrap config name.
  (Available via `AwsFeatures` in `mConfigSourceAwsSecretsManager`)

### Windows Registry (mConfigWinRegistry / mConfigWinRegistryJNI)
- `REGISTRY_BASE_PATH` (String): Registry root path (default `Software/[<company>/]<application>`; omits company if blank).
  (Available via `WinRegistryFeatures` in `mConfigWinRegistry` or `WinRegistryJNIFeatures` in `mConfigWinRegistryJNI`)

### Class loading
- `USE_CONTEXT_CLASS_LOADER` (Boolean, default: false): Prefer thread context loader.
- `USE_SYSTEM_CLASS_LOADER` (Boolean, default: false): Prefer system class loader.
- 
### Identity and context
- `COMPANY_NAME` (String, optional): Company/organization name used in OS paths. Blank/null/whitespace intentionally omits the company segment across all sources.
- `APPLICATION_NAME` (String, required): Application name used in OS paths.
- `SUB_PATH` (String, optional): Sub-path appended to application paths (e.g. `"profile/tenantA"`).
- `CURRENT_USER_ID` (String, default: OS user): User ID for the current process.
- `CURRENT_PLATFORM_OS` (OperatingSystem, auto-detected): Detected OS family.
- `HOSTNAME` (String, auto-detected): Hostname of the current machine/container. Determined when first read,
  without relying on name resolution where the system provides the name directly; `null` if it cannot be determined.
  These are automatically set by the library; they are available as a ConfigFeature so you can override them if needed.


## Patterns to avoid boilerplate

- Missing values: give the entry a `DEFAULT` in the schema; it applies automatically. Set `EXCEPTION_ON_MISSING_ENTRY=false` only if a key without value and default should return `null` instead of throwing; avoid manual `try/catch` or `null` wrapping.
- Strict config contracts: set `SCHEMA_STRICT_MODE=true` instead of filtering keys by hand.
- Default overrides: use `SCHEMA_RESETS_DEFAULTS` or `STORAGE_TYPE_PRIORITIES` rather than manual "merge" code.
- Scope behavior: use `FALLBACKS_ACROSS_SCOPES` and `WRITE_FALLBACK_ACROSS_SCOPES` instead of explicit scope iteration.
