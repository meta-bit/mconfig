# 4.5 AI Guidance (Notes for Code-Generating Tools)

We just know people will use code generators.
So it's probably for the best to give the AI some hints.

----

This document summarizes mConfig behaviours that code generators often miss.
The goal is to prevent boilerplate, duplicate logic, or incorrect assumptions.

## Logging and runtime debug control

- Runtime logging overrides are provided by `mConfigSourceEnvVar` and are only
  enabled if `ALLOW_MCONFIG_RUNTIME_SETTINGS` is set to `true`.
- Environment variables start with `MCONFIG_RUNTIME_`:
  - `MCONFIG_RUNTIME_DEBUG_LEVEL` = off|0, error|1, warn|2, info|3, debug|4, trace|5
  - `MCONFIG_RUNTIME_DEBUG_OUTPUT` = "stderr", "stdout", "quiet",
    or `file:<path>` (appended if present, created if missing)
- `MCONFIG_RUNTIME_DEBUG_OUTPUT` is handled internally by `InternalLogger`.
  You do not need to implement a custom logger for this.

Related docs:
- [mConfigSourceEnvVar README](../../../../mConfigSourceEnvVar/README.md)
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
- Schemes (typed defaults and validation) live next to the config file:
  - `src/main/resources/.config/<company>/<app>/<config>.scheme.json`
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
- To return scheme defaults on missing entries, use:
  - `EXCEPTION_ON_MISSING_ENTRY=false`
  - `DEFAULT_ON_MISSING_ENTRY=true`

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
