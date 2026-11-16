# 3.10 Better Practice: Replace This With That

A list of things to replace by better practice, for people and for AI coding assistants.
Each item shows the code to look for, what to write instead, and why.

- [Part 1](#part-1-replace-your-own-boilerplate-with-mconfig) is for code that does not use
  mConfig yet: boilerplate you can delete.
- [Part 2](#part-2-if-you-already-use-mconfig-do-it-this-way) is for code that uses mConfig,
  but in a way that works against it.

The code to copy is in
[examples/snippets.md](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md)
("templates" below); names and settings are in the [Snippets Reference](snippets.md).

## Part 1: Replace your own boilerplate with mConfig

### 1.1 Loading a properties file by hand

Instead of:
```java
Properties defaults = new Properties();
try (InputStream in = getClass().getClassLoader().getResourceAsStream("defaultconfig.properties"))
    {
    defaults.load(in);
    }
catch (IOException e)
    {
    e.printStackTrace();
    }
```

Do this: put the file into `src/main/resources/.config/<company>/<app>/network.properties` and read
through mConfig.
```java
Configuration net = factory.getConfig("network");
```

Why: mConfig finds defaults in the JAR by itself and layers the files of the host, the user and
the environment on top. The loading block goes away entirely. Template: "Bootstrap".

### 1.2 A hand-written list of places to look

Instead of:
```java
File file = new File(System.getProperty("user.home"), ".myapp/config.properties");
if (!file.exists())
    { file = new File("/etc/myapp/config.properties"); }
```

Do this: nothing. The search list is built in.
```java
Configuration cfg = factory.getConfig("config");
```

Why: mConfig knows the standard locations of each operating system (`~/.config`, `/etc`,
`%APPDATA%`, `%PROGRAMDATA%`, the registry, …) and their order of precedence.
`mconfig <company>:<app> search-paths` lists them.

### 1.3 Detecting the operating system to find the configuration directory

Instead of:
```java
String os = System.getProperty("os.name").toLowerCase();
Path dir = os.contains("win") ? Paths.get(System.getenv("APPDATA"), "MyApp")
                              : Paths.get(System.getProperty("user.home"), ".config", "myapp");
```

Do this: for reading, nothing (see 1.2). For a directory to write your own files to:
```java
Path dir = ConfigUtil.whereToPutMyFiles(factory, ConfigScope.USER, true);
```

Why: the rules differ per platform and change over time; mConfig keeps them in one place.

### 1.4 Reading environment variables or system properties directly

Instead of:
```java
String host = System.getenv("MYAPP_HOST");
int    port = Integer.parseInt(System.getProperty("myapp.port", "8080"));
```

Do this: add `mconfigsourceenvvar` (part of `mconfigstandard`) and read the configuration.
```java
String  host = net.getString("server/host");
Integer port = net.getInteger("server/port");
```

Why: the variable `MYAPP_NETWORK_SERVER_HOST` then overrides the files without any code, the
default lives in the schema, and the tool can show where a value came from. Template:
"Environment variables".

### 1.5 Defaults and validation as `if` chains

Instead of:
```java
String value = props.getProperty("port");
int port = (value == null) ? 8080 : Integer.parseInt(value);
if (port < 1 || port > 65535)
    { throw new IllegalArgumentException("port out of range"); }
```

Do this: declare it once in `network.mconfig-schema.json`, then read it.
```json
[ { "KEY": "server/port", "TYPE": "NUMBER", "DEFAULT": 8080, "PATTERN": "uint16" } ]
```
```java
Integer port = net.getInteger("server/port");
```

Why: name, type, limits, default and description sit in one place, apply wherever the value is
read, and can be turned into documentation. Template: "Defaults: schema and resource file".

### 1.6 A file watcher or a restart to pick up changes

Instead of:
```java
WatchService watcher = FileSystems.getDefault().newWatchService();
configDir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
// ... a thread that waits, reloads the file and swaps the values ...
```

Do this: read the value when you need it; subscribe only if you must react.
```java
net.subscribeToUpdates(location -> connectionPool.resize(net.getInteger("pool/size")));
```

Why: mConfig checks its sources for changes (every `UPDATE_CHECK_FREQUENCY_MS`) and the next read
returns the new value. Template: "Hot reload".

### 1.7 Passwords as `String`

Instead of:
```java
String password = props.getProperty("db.password");
log.debug("connecting with {}", props);
```

Do this: mark the entry with `"SECRET": true` in the schema, then:
```java
SecretValue password = cfg.getSecret("db/password");
```

Why: a `SecretValue` shows as `[REDACTED]` in logs and `toString()`, stays out of exports, and can
be wiped with `erase()`. Template: "Secrets".

### 1.8 Redirecting configuration paths for tests by hand

Instead of:
```java
System.setProperty("user.home", tempDir.toString());   // and hope nothing else reads it
```

Do this:
```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "demoApp");
builder.setTestMode(true);
builder.setTestConfigPaths(ConfigScope.USER, List.of(tempDir.toString()));
```

Why: test mode keeps tests away from the real configuration of the machine and the user, without
changing production code. Template: "Tests"; details: [Test Mode](15_test_mode.md).

## Part 2: If you already use mConfig, do it this way

### 2.1 A default at the call site

Instead of:
```java
Integer port = cfg.getInteger("server/port");
if (port == null)
    { port = 8080; }
```

Do this: declare `"DEFAULT": 8080` in the schema.
```java
Integer port = cfg.getInteger("server/port");
```

Why: a default at the call site is invisible, unvalidated and undocumented, and the second place
that reads the key will use a different one. With the default settings a missing value throws
`ConfigException` anyway, so the `null` check is dead code.

### 2.2 Parsing or validating by hand

Instead of:
```java
int port = Integer.parseInt(cfg.getString("server/port"));
```

Do this:
```java
Integer port = cfg.getInteger("server/port");
```

Why: the typed getters convert, and `TYPE` and `PATTERN` in the schema have already checked the value.

### 2.3 Copying values into fields at startup

Instead of:
```java
this.port = cfg.getInteger("server/port");   // in the constructor, used for the lifetime
```

Do this: keep the `Configuration` and read on use, or subscribe to updates (1.6).
```java
int port() { return cfg.getInteger("server/port"); }
```

Why: a copy never changes; hot reload does nothing for it.

### 2.4 A factory per lookup

Instead of:
```java
Configuration c = ConfigFactoryBuilder.create("ACME", "demoApp").build().getConfig("network");
```

Do this: build one `ConfigFactory` for the application, share it, close it on shutdown.

Why: every factory scans the search locations again and owns watcher threads; a factory that is
never closed leaks them.

### 2.5 A secret read as a string, or marked the wrong way

Instead of:
```java
String password = cfg.getString("db/password");
```
```json
{ "KEY": "db/password", "TYPE": "STRING", "FLAGS": ["SECRET"] }
```

Do this:
```json
{ "KEY": "db/password", "TYPE": "STRING", "SECRET": true }
```
```java
SecretValue password = cfg.getSecret("db/password");
```

Why: `"FLAGS": ["SECRET"]` is accepted (with a WARNING) but protects nothing; only
`"SECRET": true` does. And a `String` cannot be wiped and ends up in a log line sooner or later.

### 2.6 A dot as separator in hierarchical keys

Instead of:
```java
cfg.getString("server.host");   // against a JSON, YAML or TOML file with nested "server"
```

Do this:
```java
cfg.getString("server/host");
```

Why: the separator is `/`. A dot is part of the key; in flat formats such as `.properties` the
key `server.host` is one literal name.

### 2.7 Chaining off `setTestMode()`

Instead of:
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp").setTestMode(true).build();
```

Do this:
```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "demoApp");
builder.setTestMode(true);
ConfigFactory factory = builder.build();
```

Why: `setTestMode()`, `setTestConfigPaths()` and `setTestParameters()` return `void`; the chained
form does not compile. Only the `setFeature(...)` family, `setSchemas()`, `addSource()` and
`addSecretsProvider()` return the builder.

### 2.8 Reading the environment next to mConfig

Instead of:
```java
String host = System.getenv("DEMOAPP_NETWORK_SERVER_HOST");
if (host == null)
    { host = cfg.getString("server/host"); }
```

Do this:
```java
String host = cfg.getString("server/host");
```

Why: with `mconfigsourceenvvar` the variable already arrives in the `SESSION` scope and overrides
the files. Your own precedence logic duplicates the hierarchy and hides the origin of the value.

### 2.9 Your own override or fallback logic

Instead of:
```java
String value = overrides.containsKey(key) ? overrides.get(key) : cfg.getString(key);
```

Do this:
```java
cfg.put(key, value, ConfigScope.RUNTIME);
```

Why: `RUNTIME` is the scope for in-memory overrides by your own code; it wins over everything
except `POLICY`. Fallback between scopes is built in (`FALLBACKS_ACROSS_SCOPES`). The
`ConfigUtil.withOverrides()` view is for cases where the configuration must not be changed.

### 2.10 Packing a single JAR with `jar-with-dependencies`

Instead of:
```xml
<descriptorRef>jar-with-dependencies</descriptorRef>
```

Do this: use a packaging that merges the `META-INF/services` files (the merging assembly
descriptor, shade's `ServicesResourceTransformer`, or Shadow's `mergeServiceFiles()`).

Why: that descriptor keeps one file per service interface, so storages and formats go missing;
`build()` fails with `JAVA_SERVICE_CONFIGURATION_ERROR`, or sources are lost without notice.
Template: "Dependencies"; details: [Packaging as a single JAR](13_getting_started.md#138-packaging-as-a-single-jar).

### 2.11 Library settings in the global `mconfig.properties`

Instead of: `src/main/resources/.config/metabit/mConfig/mconfig.properties`

Do this: `src/main/resources/.config/<company>/<app>/mconfig.properties`, or `setFeature(...)` in code.

Why: the global file is deprecated and removed before 0.9.0; a component configures the library
for itself only. Features marked `CODE_ONLY` can be set in code only.

### 2.12 POLICY locations registered by the application

Instead of:
```java
builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, List.of("POLICY:/opt/myapp/policy"));
```

Do this: leave `POLICY` to the administrator's locations (Group Policy, enforced settings). Use
`HOST` or `APPLICATION` for what the application ships.

Why: `POLICY` overrides everything, so an application must not grant it to itself. Outside test
mode such a location fails unless `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code.

### 2.13 A version copied from documentation

Instead of: copying `<version>${mconfig.version}</version>`, or a version number from an example,
into a build file.

Do this: take the version the project already uses (its `pom.xml`, `build.gradle`, or the local
repository). If none can be found, ask.

Why: examples age; a placeholder without a definition does not build.

## Settings that replace manual code

| Instead of writing                          | Set                                                                    |
|---------------------------------------------|------------------------------------------------------------------------|
| checks for keys the schema does not know    | `SCHEMA_STRICT_MODE=true`                                              |
| `null` handling for missing values          | schema `DEFAULT`; `EXCEPTION_ON_MISSING_ENTRY` decides between exception and `null` |
| loops over scopes for reading or writing    | `FALLBACKS_ACROSS_SCOPES`, `WRITE_FALLBACK_ACROSS_SCOPES`               |
| probing file formats in a fixed order       | `FILE_FORMAT_READING_PRIORITIES`, `FILE_FORMAT_WRITING_PRIORITIES`      |

All settings: [Config Features](25_config_features.md).
