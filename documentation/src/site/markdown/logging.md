# Logging in mConfig

mConfig uses a custom logging abstraction defined by the `ConfigLoggingInterface`. This allows the library to remain independent of specific logging frameworks while still providing necessary diagnostic information.

## Available Implementations

| Implementation   | Module                | Priority | Service Name   | Use Case                                         |
|:-----------------|:----------------------|:---------|:---------------|:-------------------------------------------------|
| `NullLogging`    | `mConfigCore`         | 0        | `null`         | Default; silent operation.                       |
| `ConsoleLogging` | `mConfigCore`         | 1        | `console`      | Dev/tests; prints to stdout/stderr. **Not for production.** |
| `Slf4j2Logger`   | `mConfigLoggingSlf4j` | 5        | `slf4j`        | Production use; forwards to SLF4J.               |

### Internal and Hidden Implementations
- `InternalLogger` (`mConfigCore`, `impl.core`): Activated if `ConfigFeature.LOGGING_REDIRECT_TARGET` is set. Redirects to specified target.

## Using SLF4J2Logger in Production

`Slf4j2Logger` (priority 5) bridges mConfig logging to SLF4J.

### 1. Add Dependencies

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

### 2. Auto-Discovery

With `mConfigLoggingSlf4j` on classpath, auto-selected (highest prio).

Override with `ConfigFactoryBuilder.setFeature(ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB, "slf4j")`.

### 3. Configuration

Configure SLF4J via `logback.xml`, etc. 

`Slf4j2Logger` uses logger name from `ConfigFactoryBuilder` company/app or default "mConfig".

## ConsoleLogging for Development and Tests

`ConsoleLogging` is a lightweight console logger (stdout for lower levels, stderr for WARN/ERROR) for debugging mConfig internals. No external dependencies required.

### 1. Build Configuration (Maven & Gradle)

To use `ConsoleLogging` in modules other than `mConfigCore`, you must include the `mConfigCore` `test-jar` dependency in your build:

Maven:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigCore</artifactId>
    <version>${mconfig.version}</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
```

Gradle:

```gradle
testImplementation 'org.metabit.platform.support.config:mConfigCore:${mconfig.version}:tests'
```

### 2. Manual Activation

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

### 3. Usage with ConfigFactoryBuilder

`ConfigFactoryBuilder` uses `ServiceLoader` to discover `ConfigLoggingInterface` implementations. 

**Note for Modular Projects:** `ConsoleLogging` is fully supported via `provides` in the `mConfigCore` `module-info.java`.

To force the use of `ConsoleLogging` when using `ConfigFactoryBuilder` in tests, you can set the feature:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("company", "app");
builder.setFeature(ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB, "console");
```

This will only work if the `ConsoleLogging` class is discoverable by `ServiceLoader`. If discovery fails, it will fall back to `NullLogging`.

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

## How Logging is Discovered and Selected

In `DefaultConfigFactoryBuilder.findLoggerForConfigUse()`:

1. If [`ConfigFeature.LOGGING_REDIRECT_TARGET`](4_1_ConfigFeatures.md#logging_redirect_target) is set, instantiate `InternalLogger` with target and init.

2. Load `ServiceLoader<ConfigLoggingInterface>`:
   - Prefer exact match to [`ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB`](4_1_ConfigFeatures.md#logging_to_use_in_configlib) by service name (case-insensitive).
   - Otherwise, select highest priority (`getServiceModulePriority()`) among those that `init()` successfully.
3. Fallback to `NullLogging`.

All loggers must implement `init(ConfigFactorySettings)` (usually returns `true`) and `exit()`.
