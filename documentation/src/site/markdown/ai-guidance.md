# Notes for Code-Generating Tools

I just know people will use code generators these days (2026).
So it's probably for the best to give the AI some hints.

----

This document summarizes mConfig behaviors that code generators often miss.
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
- `mConfigSourceEnvVar/README.md`
- `documentation/src/site/markdown/logging.md`

## Console logging for dev/tests

- `ConsoleLogging` in `mConfigCore` main jar (service `"console"`).
- Production logging should use `mConfigLoggingSlf4j` or the default
  `NullLogging`.
- Enable with `ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB = "console"`.

Related docs:
- `documentation/src/site/markdown/logging.md`

## Defaults belong in resources, not in code

- `mConfigSourceJAR` automatically loads defaults from resources located at
  `.config/<company>/<app>/<config>.<ext>`.
- For a standard Maven layout, the default location is:
  - `src/main/resources/.config/<company>/<app>/<config>.properties`
- Schemes (typed defaults and validation) live next to the config file:
  - `src/main/resources/.config/<company>/<app>/<config>.scheme.json`
- Avoid manual loading of `Properties` defaults. mConfig already layers JAR
  defaults with filesystem, environment, and other sources.

Related docs:
- `documentation/src/site/markdown/examples/simple_configuration_loading.md`
- `documentation/src/site/markdown/4_4_Code_Improvements.md`
- `documentation/src/site/markdown/regular-use.md`

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
- `documentation/src/site/markdown/4_1_ConfigFeatures.md`
- `documentation/src/site/markdown/design_consolidated.md`
- `documentation/src/site/markdown/4_4_Code_Improvements.md`

## Overrides: use scopes, not ad-hoc logic

- For in-process overrides, prefer `ConfigScope.RUNTIME` via `put(...)`.
  It is the highest regular scope and overrides everything except `POLICY`.
- For environment/session overrides, use `ConfigScope.SESSION` (env vars and CLI).
  Avoid custom precedence logic.

Related docs:
- `documentation/src/site/markdown/regular-use.md`
- `documentation/src/site/markdown/3_how_does_it_work.md`

## Test Mode is a first-class feature

- Enable with `ConfigFactoryBuilder.setTestMode(true)` or via
  `MCONFIG_RUNTIME_TEST_MODE=true` when `ALLOW_MCONFIG_RUNTIME_SETTINGS` is on.
- Test Mode bypasses OS paths and prefers test resources such as
  `src/test/resources/.config/<company>/<app>/` and `src/test/resources/config/{SCOPE}`.
- Test Mode can be locked down globally with `ConfigFactoryBuilder.forbidTestMode()`.

Related docs:
- `documentation/src/site/markdown/test-mode.md`
- `documentation/src/site/markdown/examples/testing_configurations.md`

## Prefer mConfigUtil adapters for legacy interop

- `ConfigUtil.withOverrides(...)`: immutable overlay; prefer over custom map merges.
- `ConfigUtil.remapped(...)`: prefix remapping for sub-config views.
- `ConfigUtil.fromProperties(...)`: bridge legacy `Properties` without re-parsing.

Related docs:
- `documentation/src/site/markdown/examples/configuration_views.md`
- `mConfigUtil/src/main/java/org/metabit/platform/support/config/util/ConfigUtil.java`
