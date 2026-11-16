<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigSourceEnvVar",
  "description": "mConfig module for accessing environment variables and controlling library behavior at runtime. Supports secure wiping of environment overrides.",
  "programmingLanguage": {
    "@type": "ComputerLanguage",
    "name": "Java",
    "url": "https://www.java.com"
  },
  "isPartOf": {
    "@type": "SoftwareApplication",
    "name": "mConfig",
    "url": "https://github.com/meta-bit/mconfig"
  },
  "keywords": "environment-variables, runtime-control, debugging-tools, memory-safety"
}
</script>
-->

# mConfigSourceEnvVar

This is a mConfig module for accessing environment variables for configuration purposes.

The default naming scheme is `<application name>_<config name>_<config key>`.

The environment variable name is **converted to UPPER CASE** before lookup.
Within the config key, the hierarchy separator `/` is mapped to `_`,
and any literal `_` already in the key is doubled to `__`.

So for `ConfigFactoryBuilder.create("myco", "myapp")`, configuration `network`,
key `server/host`, the environment variable looked up is:

```
MYAPP_NETWORK_SERVER_HOST
```

A key with an underscore of its own, `max_connections`, is looked up as:

```
MYAPP_NETWORK_MAX__CONNECTIONS
```

Up to 0.8.16 the underscore was not doubled, contrary to this description, so `max_connections`
and `max/connections` shared one variable. Since 0.8.17 `MYAPP_NETWORK_MAX_CONNECTIONS` stands for
`max/connections` only. If a key with an underscore is requested and only a variable in the old
naming is set, a WARNING names the variable to rename (this notice is removed in 0.9).

Limits of the mapping:

* Three or more underscores in a row are ambiguous: `a_/b` and `a/_b` both give `A___B`.
  Avoid key parts that start or end with an underscore.
* The variable name does not keep the case of the key. Lookup of `maxConnections` works;
  when keys are listed, it appears as `maxconnections`.
* Application and configuration name are put into the prefix as they are, in upper case.

Note that environment variables are case-sensitive on Linux and macOS:
a lowercase `myapp_network_server_host` will *not* be found there.

The ConfigScope the environment variables are seen in is `SESSION`.
(more specific than USER, so they can override USER settings.)

Writing should not happen to env by default, unless the scope SESSION is actively specified.

## How this compares to other conventions

There is no RFC or other standard for mapping configuration keys to environment variable names.
The only standard on the names themselves is POSIX
([IEEE 1003.1, Base Definitions, chapter 8](https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap08.html)):
portable names consist of upper-case letters, digits and `_`, and do not start with a digit.
Every tool maps its keys into that small alphabet in its own way. Three customs are common:

| Custom                                   | Rule                                                        | Examples                                                                                                                                                                                                                                                                         |
|:-----------------------------------------|:------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `_` separates, nothing is escaped        | every character outside the alphabet becomes `_` (or is dropped) | Spring Boot relaxed binding, [MicroProfile Config / Quarkus](https://quarkus.io/guides/config-reference), Grafana (`GF_...`)                                                                                                                                                     |
| `_` separates, a literal `_` is doubled  | separator becomes `_`, `_` becomes `__`                     | **mConfig**, [Elasticsearch](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-elasticsearch-docker-configure) (`ES_SETTING_BOOTSTRAP_MEMORY__LOCK`), [Confluent Platform images](https://docs.confluent.io/platform/current/installation/docker/config-reference.html) |
| `__` separates, a single `_` is literal  | separator becomes `__`                                      | [.NET configuration](https://learn.microsoft.com/aspnet/core/fundamentals/configuration/) (`Logging__LogLevel__Default`), Airflow (`AIRFLOW__CORE__EXECUTOR`)                                                                                                                    |

What follows from this for mConfig:

* **Coming from .NET, Airflow or similar:** the double underscore means the opposite here.
  `MYAPP_NETWORK_DB__HOST` is the key `db_host`, not `db/host`; write `MYAPP_NETWORK_DB_HOST`
  for `db/host`.
* **Coming from Spring Boot or MicroProfile:** names for keys without an underscore look the
  same. A key with an underscore needs the doubled form, since mConfig keeps `db_host` and
  `db/host` apart.
* **Keys with `-` or `.`:** only `/` and `_` are mapped. Other characters go into the variable
  name as they are, so the variable for `server/read-timeout` is `MYAPP_NETWORK_SERVER_READ-TIMEOUT`.
  A POSIX shell cannot `export` such a name; `env`, Docker and Kubernetes (1.32 and later, by
  default) can set it. Prefer keys without these characters where environment variables are to
  override them.
* **Kubernetes service links:** unless the pod sets `enableServiceLinks: false`, Kubernetes puts
  variables such as `REDIS_SERVICE_HOST`, `REDIS_PORT` and `REDIS_PORT_6379_TCP_ADDR` into every
  container, for each Service of the namespace. An application whose name equals a Service name,
  with a configuration named `port` or `service`, would read them as configuration values.
  Choose different names, or switch the service links off.

## Security and Memory Safety

While environment variables are naturally persistent in the OS, the mConfig module provides **Memory Safety** within the JVM:

- **Explicit Wiping**: The module implements the `erase()` lifecycle. When the configuration handle is closed, the module clears its internal cache of environment overrides to minimize their presence in the JVM heap.
- **Redaction**: Environment variables marked as secrets in the Schema are automatically redacted in logs and diagnostic outputs.
- **Runtime Control Safety**: Features that allow runtime changes to mConfig operation (via `MCONFIG_RUNTIME_` variables) must be explicitly activated in code using `ALLOW_MCONFIG_RUNTIME_SETTINGS`.

-----

In addition to accessing environment variables,
this is a special module: It allows runtime changes to mConfig operation.
This makes it useful for debugging and development purposes;
but it could also be seen as a security risk. Because of this, you have to
activate the mConfig runtime changes feature explicitly with
`ALLOW_MCONFIG_RUNTIME_SETTINGS` set to `true`.

These settings start with "MCONFIG_RUNTIME_", and are:

* `MCONFIG_RUNTIME_DEBUG_LEVEL` with values `off|0`, `error|1`, `warn|2`, `info|3`, `debug|4`, `trace|5`,  
* `MCONFIG_RUNTIME_DEBUG_OUTPUT` with values `"stderr"`, `"stdout"`, `"quiet"`, `"file:"+filename with path`;
`"syslog"` considered for future extension.
file is appended to if exists, created if not; if access fails, that is silent.
Internally, this is handled by `InternalLogger`.

* `MCONFIG_RUNTIME_DISABLE_MODULES` with a "," separated list of module IDs to disable.
if takes precedence over the module loading
* `MCONFIG_RUNTIME_MODULES` list which is a "," separated list of module IDs and/or
module file paths. @SECURITY adding modules at runtime is a security risk;
another feature which needs to be activated explicitly in code! (Not implemented yet)

### Writing to Environment Variables
Writing to environment variables is not supported by this module and will throw a `ConfigCheckedException`. 
Environment variables are considered a read-only configuration source for the process lifetime.

```java
// This will throw ConfigCheckedException
config.put("server/port", 8080, ConfigScope.SESSION);
```

Uses `System.getenv()` and `System.getenv(name)`.