# 4.2 Logging in mConfig

mConfig uses a custom logging abstraction defined by the `ConfigLoggingInterface`. This allows the library to remain independent of specific logging frameworks while still providing necessary diagnostic information.

## Available Implementations

| Implementation     | Module                | Priority | Service Name         | Use Case                                                    |
|:-------------------|:----------------------|:---------|:---------------------|:------------------------------------------------------------|
| `NullLogging`      | `mConfigCore`         | 0        | `null`               | Default; silent operation.                                  |
| `ConsoleLogging`   | `mConfigCore`         | 1        | `console`            | Dev/tests; prints to stdout/stderr. **Not for production.** |
| `StdErrLogger`     | `mConfigUtil`         | 1        | `stderr-logger`      | Minimal logger printing to stderr.                          |
| `JavaSystemLogger` | `mConfigUtil`         | 2        | `java.System.Logger` | Uses JDK 9+ `System.Logger`.                                |
| `Slf4j2Logger`     | `mConfigLoggingSlf4j` | 5        | `slf4j`              | Production use; forwards to SLF4J.                          |

### Internal and Hidden Implementations
`InternalLogger` (`mConfigCore`, `impl.core`): Activated if `ConfigFeature.LOGGING_REDIRECT_TARGET` is set. Redirects to specified target.

## Using SLF4J2Logger in Production

`Slf4j2Logger` (priority 5) bridges mConfig logging to SLF4J.

### 4.2.1 Add Dependencies

Maven:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigLoggingSlf4j</artifactId>
    <version>${mconfig.version}</version>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mConfigLoggingSlf4j:${mconfig.version}'
```

**Note:** Also need SLF4J API + impl (e.g., logback-classic).

### 4.2.2 Auto-Discovery

With `mConfigLoggingSlf4j` on classpath, auto-selected (highest prio).

Override with `ConfigFactoryBuilder.setFeature(ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB, "slf4j")`.

### 4.2.3 Configuration

Configure SLF4J via `logback.xml`, etc. 

`Slf4j2Logger` uses logger name from `ConfigFactoryBuilder` company/app or default "mConfig".

## ConsoleLogging for Development and Tests

`ConsoleLogging` is a lightweight console logger (stdout for lower levels, stderr for WARN/ERROR) for debugging mConfig internals. It is included directly in `mConfigCore` with no external dependencies required.

### 4.2.4 Manual Activation

You can instantiate `ConsoleLogging` directly and pass it to components.

```java
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.metabit.platform.support.config.ConfigFeature;

// ... in your test ...
ConsoleLogging logger = new ConsoleLogging("MyModuleTest");
logger.setLevel("DEBUG");

// Use with a component that takes a logger
component.testComponent(settings, logger);
```

### 4.2.5 Usage with ConfigFactoryBuilder

`ConfigFactoryBuilder` uses `ServiceLoader` to discover `ConfigLoggingInterface` implementations. 

`ConsoleLogging` is discoverable via `ServiceLoader` and JPMS `provides` in `mConfigCore`.

To force the use of `ConsoleLogging` when using `ConfigFactoryBuilder` in tests or development:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("company", "app");
builder.setFeature(ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB, "console");
```

## Configuration

### Log Levels
Available levels: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`.
Default level for `ConsoleLogging` is `INFO`.

You can set the level via `setLevel(String)`:
```java
logger.setLevel("TRACE");
```

### Output Redirection
- `INFO`, `DEBUG`, `TRACE` are printed to `System.out`.
- `WARN`, `ERROR` are printed to `System.err`.

## Outside text in log messages

mConfig logs file names, paths and other names that come from outside the application: from the
filesystem, environment variables, or configuration sources. Such text could contain line breaks
that forge extra log lines, terminal escape sequences that rewrite a console, or bidirectional
overrides that make a name look different from what it is (CWE-117).

Since 0.8.15, the filesystem storage writes such text into log messages with these characters
escaped: control characters as `\n`, `\r`, `\t` or `\uXXXX`, Unicode line and paragraph separators
and bidi controls as `\uXXXX`. Everything else, backslashes in Windows paths included, is logged
unchanged, so the logged location matches the real one. Names containing such characters are also
refused as input; see [File and directory names from the filesystem](34_definitions_and_valid_values.md#342-file-and-directory-names-from-the-filesystem).

Limits:

- Since 0.8.17 the other storages and the format parsers escape outside text the same way, including
  exception messages they put into a log line. Names are not refused there; escaping only.
- Exceptions passed to the logger as such, with their stack traces, are not escaped.
- Lookups that a logging backend performs on message content, e.g. `${...}` in old log4j 2
  versions, are the backend's responsibility. Use a current backend version.

For library and module developers: build messages with
`org.metabit.platform.support.config.impl.logging.LogSafe.text(value)` for every outside value, and
never put outside text into the format string of the format variants of `ConfigLoggingInterface`;
pass it as an argument.

## Parse errors

When a configuration file or resource cannot be parsed, mConfig logs one WARNING and records a PARSE
event. It always names the configuration, the location, the format, the type of error, the line and
column, and the key at the error position if the parser can tell it. The file is skipped; other
sources still supply their values.

A parser's own message often quotes the content it failed on. If that content is a secret value, the
message would carry it into the log. Since 0.8.17 the parser's message is therefore added only where
the content cannot hold a secret:

- never for secret stores (e.g. Kubernetes Secrets) and for the bootstrap configurations of the cloud
  modules (`vault`, `aws`, ...), which typically hold credentials without a schema;
- not if the configuration's schema marks the entry at the error position, or an entry above or below
  it, as `SECRET`;
- not if the parser cannot tell the position (e.g. SnakeYAML) and the schema has any `SECRET` entry.

The full list, including the cases that need no schema, is in
[Secrets: parse errors](26_secrets_concept.md#parse-errors). The log line then says that the parser details were withheld. For development, set
[`LOG_PARSE_ERROR_DETAILS`](25_config_features.md) in code to see every parser message. All of it is
escaped like other outside text.

## How Logging is Discovered and Selected

In `DefaultConfigFactoryBuilder.findLoggerForConfigUse()`:

1. If [`ConfigFeature.LOGGING_REDIRECT_TARGET`](25_config_features.md) is set, instantiate `InternalLogger` with target and init.

2. Load `ServiceLoader<ConfigLoggingInterface>`:
   - Prefer exact match to [`ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB`](25_config_features.md) by service name (case-insensitive).
   - Otherwise, select highest priority (`getServiceModulePriority()`) among those that `init()` successfully.
3. Fallback to `NullLogging`.

All loggers must implement `init(ConfigFactorySettings)` (usually returns `true`) and `exit()`.
