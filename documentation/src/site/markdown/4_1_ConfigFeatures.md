# 4.1 Config Features

ConfigFeature flags tune how a ConfigFactory is built and how it behaves.
Set them on the ConfigFactoryBuilder before calling `build()`.

example:
```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true)
        .setFeature(ConfigFeature.DEFAULT_ON_MISSING_ENTRY, true)
        .setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of("/opt/acme/overrides"));
```

Notes:
- Booleans default to `false` unless a default is explicitly listed below.
- Some features are only used when the corresponding module is on the classpath
  (e.g., ZooKeeper, Registry, Secrets, JAR).
- Runtime environment variables can override a subset of settings if
  `ALLOW_MCONFIG_RUNTIME_SETTINGS` is enabled. See "Runtime overrides" below.

## Runtime overrides

If `ALLOW_MCONFIG_RUNTIME_SETTINGS` is set to `true`, mConfig accepts runtime
settings via environment variables with the prefix `MCONFIG_RUNTIME_`:

- `MCONFIG_RUNTIME_DEBUG_LEVEL` -> `LOGLEVEL_NUMBER` (accepts numbers or: `off`, `error`, `warn`, `info`, `debug`, `trace`)
- `MCONFIG_RUNTIME_DEBUG_OUTPUT` -> `LOGGING_REDIRECT_TARGET`
- `MCONFIG_RUNTIME_DISABLE_MODULES` -> `DISABLED_MODULE_IDS` (comma-separated)
- `MCONFIG_RUNTIME_MODULES` -> `ADDITIONAL_MODULE_PATHS` (comma-separated)
- `MCONFIG_RUNTIME_TEST_MODE` -> `TEST_MODE`

## Feature reference

### Test mode
- `PERMIT_TEST_MODE` (Boolean, default: true): Master gate for test mode usage.
- `TEST_MODE` (Boolean, default: false): Activate test-mode search locations.
- `TESTMODE_DIRECTORIES` (`List<String>`, default: empty): Scoped paths in `SCOPE:PATH` format.
- `TESTMODE_PARAMETERS` (`Map<String,String>`): Additional test parameters.

### Logging
- `QUIET` (Boolean, default: false): Reduce internal mConfig logging.
- `LOGGING_TO_USE_IN_CONFIGLIB` (String, default: ""): Force a specific logging module.
- `LOGLEVEL_NUMBER` (Integer, default: 4): Numeric log level for basic loggers.
- `LOGGING_REDIRECT_TARGET` (String): Redirect internal logging (`stderr`, `stdout`, `quiet`, `file:<path>`).

### Filesystem and registry paths
- `ADDITIONAL_RUNTIME_DIRECTORIES` (`List<String>`): Extra filesystem directories (prepended) for `RUNTIME` scope.
- `ADDITIONAL_USER_DIRECTORIES` (`List<String>`): Extra filesystem directories (prepended) for `USER` scope.
- `REGISTRY_BASE_PATH` (`String`): Registry root path (default `Software/[<company>/]<application>`; omits company if blank).
- `FILENAME_EXTENSION_MAPPINGS` (`List<String>`): Extra filename extensions to try when reading config files.
- `DEFAULT_TEXTFILE_CHARSET` (`String`, default: `UTF-8`): Charset for text formats.
- `TRIM_TEXTVALUE_SPACES` (Boolean, default: true): Trim leading/trailing spaces in text values.
- `WRITE_CONDENSED_FORMAT` (Boolean, default: false): Prefer compact output when writing text formats.

### Format priorities
- `FILE_FORMAT_READING_PRIORITIES` (`List<String>`, default: `DHALL,TOML,YAML,JSON5,JSON,ASN1,properties,INI`): Read order.
- `FILE_FORMAT_WRITING_PRIORITIES` (`List<String>`, default: `TOML,YAML,JSON5,JSON,properties`): Write order.
- `FILE_FORMAT_READING_ALLOW_ALL_FORMATS` (Boolean, default: true): Fallback to non-listed formats.
- `FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS` (Boolean, default: true): Fallback to non-listed formats.

### Storage type priorities
- `STORAGE_TYPE_PRIORITIES` (`List<String>`, default: `RAM,secrets,files,registry,registryjni,zookeeper,JAR`): Init/search order within a scope.
- `STORAGE_TYPE_ALLOW_ALL_STORAGES` (Boolean, default: true): Allow storages not explicitly listed.

### Schemes and defaults
- `CONFIG_SCHEME_LIST` (`Map<String,ConfigScheme>`): Provide schemes programmatically.
- `SCHEME_STRICT_MODE` (Boolean, default: false): Reject keys not in the scheme.
- `SCHEME_RESETS_DEFAULTS` (Boolean, default: false): Replace existing defaults instead of merging.
- `FREE_CONFIGURATION` (Boolean): Allow configs without a scheme (behavior may evolve).

### Missing entry behavior
- `EXCEPTION_ON_MISSING_ENTRY` (Boolean, default: true): Throw when a key is absent.
- `DEFAULT_ON_MISSING_ENTRY` (Boolean, default: false): Return scheme defaults if missing (requires exceptions disabled).
- `EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND` (Boolean, default: false): Throw if a configuration name is not found at all.
- `FALLBACKS_ACROSS_SCOPES` (Boolean, default: true): Allow fallback to less specific scopes on reads.
- `WRITE_FALLBACK_ACROSS_SCOPES` (Boolean, default: false): Allow fallback to more specific scopes on writes.

### Cache and update checks
- `UPDATE_CHECK_FREQUENCY_MS` (Integer, default: 2000): Polling interval for cached sources.
- `UPDATE_CHECK_SCOPES` (`List<String>`, default: all scopes): Limit update checks by scope.
- `CACHE_CONFIGS` (Boolean): Cache configurations in memory (experimental).
- `AUTOMATIC_CONFIG_LOADING` (Boolean): Auto-load configs before first read (experimental).
- `AUTOMATIC_CONFIG_CREATION` (Boolean): Auto-create configs on write (experimental).
- `WRITE_SYNC` (Boolean): Sync writes immediately (storage-dependent).
- `NO_DEFAULT_DIRECTORIES` (Boolean): Disable default filesystem paths (experimental).

### Modules and runtime configuration
- `ALLOW_MCONFIG_RUNTIME_SETTINGS` (Boolean, default: false): Allow `MCONFIG_RUNTIME_*` env overrides.
- `ENABLE_SELF_CONFIGURATION` (Boolean, default: true): Allow classpath self-configuration.
- `ADDITIONAL_MODULE_PATHS` (`List<String>`): Add module IDs/paths during discovery.
- `DISABLED_MODULE_IDS` (`List<String>`): Disable specific module IDs.

### Secrets providers (mConfigSecrets)
- `SECRETS_PROVIDER_ID` (String): Primary provider ID.
- `SECRETS_PROVIDER_CONFIG` (`Map<String,Object>`): Provider configuration.
- `ADDITIONAL_SECRETS_PROVIDERS` (`List<Map<String,Object>>`): Additional providers with optional scope.

### ZooKeeper (mConfigSourceZooKeeper)
- `ZOOKEEPER_CONNECT_STRING` (String): Connection string.
- `ZOOKEEPER_ROOT_PATH` (String, default: `/mconfig`): Root path.
- `ZOOKEEPER_SESSION_TIMEOUT_MS` (Integer, default: 60000): Session timeout.
- `ZOOKEEPER_RETRY_BASE_SLEEP_MS` (Integer, default: 1000): Retry base sleep.
- `ZOOKEEPER_RETRY_MAX_RETRIES` (Integer, default: 3): Retry count.
- `ZOOKEEPER_BOOTSTRAP_CONFIG_NAME` (String, default: `zookeeper`): Bootstrap config name.

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
- `HOSTNAME` (String, auto-detected): Hostname of the current machine/container.
  These are automatically set by the library; they are available as a ConfigFeature so you can override them if needed.


## Patterns to avoid boilerplate

- Missing values: prefer `EXCEPTION_ON_MISSING_ENTRY=false` and `DEFAULT_ON_MISSING_ENTRY=true` over manual `try/catch` or `null` wrapping.
- Strict config contracts: set `SCHEME_STRICT_MODE=true` instead of filtering keys by hand.
- Default overrides: use `SCHEME_RESETS_DEFAULTS` or `STORAGE_TYPE_PRIORITIES` rather than manual "merge" code.
- Scope behavior: use `FALLBACKS_ACROSS_SCOPES` and `WRITE_FALLBACK_ACROSS_SCOPES` instead of explicit scope iteration.
