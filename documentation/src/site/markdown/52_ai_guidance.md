# 5.3 AI Guidance (Notes for Code-Generating Tools)

We just know people will use code generators.
So it's probably for the best to give the AI some hints.

----

This document summarizes mConfig behaviours that code generators often miss.
The goal is to prevent boilerplate, duplicate logic, or incorrect assumptions.

Three companions carry the concrete material:

- [Snippets Reference](snippets.md): the exact API names, schema fields, settings and scopes.
- [examples/snippets.md](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md):
  code templates to copy; the Java ones are compiled against the current API on every build.
- [Better Practice: Replace This With That](38_code_improvements.md): what to replace, as pairs of
  "instead of" and "do this". Use part 1 when migrating code to mConfig, part 2 when reviewing code
  that uses it.

## Read the documentation before reconstructing behaviour from sources

Before attempting to explain or reverse-engineer mConfig behaviour from library source code or JAR artifacts:
- Read the core chapters first:
  - [20 Scopes](20_scopes.md) and [21 How it works](21_how_it_works.md) (10-tier scope hierarchy and architecture)
  - [22 Priorities and hierarchies](22_priorities_and_hierarchies.md) (Precedence rules: within a scope, later entries override earlier ones for reading)
  - [15 Test mode](15_test_mode.md) (Isolation and runtime settings activation requirements)
  - [23 Configuration schemas](23_configuration_schemas.md) (Schema format, type constraints, defaults)
  - [42 Logging](42_logging.md) (Available loggers and bootstrap selection)
- **Search list vs. Active layers**: `getSearchList()` returns all candidate `ConfigLocation`s considered across all scopes and storages, including directories that do not exist yet on disk (which are retained so that lazily created configurations are detected at runtime). To find only the locations that actually exist and contributed active data layers, use `configuration.getSourceLocations()`.

## Logging and runtime debug control

- Runtime logging overrides are handled by mConfig core (`RuntimeConfigurator`) and are only
  enabled if the application set `ALLOW_MCONFIG_RUNTIME_SETTINGS` to `true` in code
  (it cannot be enabled from outside or from `mconfig.properties`).
- Environment variables start with `MCONFIG_RUNTIME_`:
  - `MCONFIG_RUNTIME_DEBUG_LEVEL` = off|0, error|1, warn|2, info|3, debug|4, trace|5
  - `MCONFIG_RUNTIME_DEBUG_OUTPUT` = "stderr", "stdout", "quiet",
    or `file:<path>` (appended if present, created if missing)
- `MCONFIG_RUNTIME_DEBUG_OUTPUT` is handled internally by `InternalLogger`.
  You do not need to implement a custom logger for this.

Related docs:
- [mConfigSourceEnvVar README](https://github.com/meta-bit/mconfig/blob/master/mConfigSourceEnvVar/README.md)
- [logging](42_logging.md)

## Console logging for dev/tests

- `ConsoleLogging` in `mConfigCore` main jar (service `"console"`).
- Production logging should use `mConfigLoggingSlf4j` or the default
  `NullLogging`.
- Enable with `ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB = "console"`.

Related docs:
- [logging](42_logging.md)

## Defaults belong in resources, not in code

- `mConfigSourceJAR` automatically loads defaults from resources located at
  `.config/<company>/<app>/<config>.<ext>`.
- For a standard Maven layout, the default location is:
  - `src/main/resources/.config/<company>/<app>/<config>.properties`
- Schemas (typed defaults and validation) live next to the config file:
  - `src/main/resources/.config/<company>/<app>/<config>.mconfig-schema.json`
- Avoid manual loading of `Properties` defaults. mConfig already layers JAR
  defaults with filesystem, environment, and other sources.
- Avoid null checks. mConfig has ConfigFeature flags to control behaviour, 
  whether to throw an exception or return a default value. Prefer setting defaults
  as described above.

Related docs:
- [simple configuration loading](examples/simple_configuration_loading.md)
- [3.8 code improvements](38_code_improvements.md)
- [regular use](14_regular_use.md)

## Fallbacks and conversion are built in

- Fallback across scopes is a first-class feature:
  - `FALLBACKS_ACROSS_SCOPES` controls reads (default: true)
  - `WRITE_FALLBACK_ACROSS_SCOPES` controls writes (default: false)
- Typed getters (`getInteger`, `getBoolean`, etc.) perform conversion for you.
  Do not parse values manually.
- Schema defaults apply automatically: a missing key with a `DEFAULT` in the schema returns it.
  Set `EXCEPTION_ON_MISSING_ENTRY=false` only if keys with neither value nor default should return
  `null` instead of throwing. Do not generate `DEFAULT_ON_MISSING_ENTRY`: it is deprecated and has
  no effect.

Related docs:
- [2.5 config features](25_config_features.md)
- [design consolidated](44_design_consolidated.md)
- [3.8 code improvements](38_code_improvements.md)

### converters and mappers

- ConfigMapper is provided to map from and to POJOs.
- ConfigUtil.fromProperties(...) is a bridge to convert legacy `Properties`.
- ConfigUtil.withOverrides(...) is a bridge to merge custom maps.
- ConfigUtil.remapped(...) is a bridge to prefix sub-configs.
- See [examples/configuration_views.md](examples/configuration_views.md) for more.

## Overrides: use scopes, not ad-hoc logic

- For in-process overrides, prefer `ConfigScope.RUNTIME` via `put(...)`.
  It is the highest regular scope and overrides everything except `POLICY`.
- For environment/session overrides, use `ConfigScope.SESSION` (env vars and CLI).
  Avoid custom precedence logic.

Related docs:
- [regular use](14_regular_use.md)
- [2.1 how it works](21_how_it_works.md)

## Test Mode is a first-class feature

- Enable with `ConfigFactoryBuilder.setTestMode(true)` or via
  `MCONFIG_RUNTIME_TEST_MODE=true` when `ALLOW_MCONFIG_RUNTIME_SETTINGS` is on.
  A component may also set its own test mode in its self-configuration file
  `src/test/resources/.config/<company>/<app>/mconfig.properties` (`TEST_MODE=true`);
  never put that into `src/main/resources`. The global `.config/metabit/mConfig/mconfig.properties`
  is deprecated and cannot set it (see [who may set a feature](25_config_features.md#who-may-set-a-feature)).
- Do not generate POLICY-scope locations (`POLICY:` directory prefixes, POLICY sources):
  outside test mode they fail unless the application sets `ALLOW_APPLICATION_POLICY_LOCATIONS`.
- Test Mode bypasses OS paths and prefers test resources such as
  `src/test/resources/.config/<company>/<app>/` and `src/test/resources/config/{SCOPE}`.
- Test Mode can be locked down globally with `ConfigFactoryBuilder.forbidTestMode()`.

Related docs:
- [test mode](15_test_mode.md)
- [testing configuration](examples/testing_configurations.md)

## Prefer mConfigUtil adapters for legacy interop

- `ConfigUtil.withOverrides(...)`: immutable overlay; prefer over custom map merges.
- `ConfigUtil.remapped(...)`: prefix remapping for sub-config views.
- `ConfigUtil.fromProperties(...)`: bridge legacy `Properties` without re-parsing.

Related docs:
- [configuration views](examples/configuration_views.md)
- `mConfigUtil/src/main/java/org/metabit/platform/support/config/util/ConfigUtil.java`


## Cloud and Secret Storages are Opt-in

mConfig has storage modules for cloud-native and secret configuration. They are opt-in: neither
`mConfigBasic` nor `mConfigStandard` includes them. They are storage modules, not secrets providers
(`addSecretsProvider()`); mConfig ships no secrets provider in this version yet. Check for 0.9.x versions regarding secrets providers.

- **Vault (`mConfigSourceVault`)**: sources default to the `CLOUD` scope; `withScope(...)` on the builder places them elsewhere.
- **AWS Secrets Manager (`mConfigSourceAwsSecretsManager`)**: same; `CLOUD` by default.
- **Kubernetes (`mConfigSourceKubernetes`, `mConfigSourceKubernetesFabric8`)**: ConfigMaps and Secrets; the mounted directories go into the `CLUSTER` scope.

An additional source registered at `POLICY` scope fails in productive mode unless `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code.

Before concluding that a source is missing or unsupported, check for these `mConfigSource*` modules.

Related docs:
- [3.5 Handling Secrets](35_handling_secrets.md)
- [26 Security Model (Secrets Concept)](26_secrets_concept.md)
