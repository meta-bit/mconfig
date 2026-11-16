# mConfigSourceZooKeeper

<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigSourceZooKeeper",
  "description": "Experimental mConfig module providing an Apache ZooKeeper implementation of ConfigSource. Supports real-time updates via watchers.",
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
  "keywords": "zookeeper, distributed-configuration, real-time-updates"
}
</script>
-->

> [!WARNING]
> This module is currently **EXPERIMENTAL**. The API and configuration structure may change in future releases. Feedback and bug reports are welcome.

This module provides an Apache ZooKeeper implementation of `ConfigSource` for mConfig. It allows for hierarchical configuration management within a ZooKeeper cluster with real-time updates through watchers.

## Configuration

The ZooKeeper source is self-configuring through mConfig using the configuration handle `zookeeper`. 
The module reads its settings from the application's configuration named `zookeeper` (changeable with `ZOOKEEPER_BOOTSTRAP_CONFIG_NAME`). It is an ordinary mConfig configuration of the application, resolved through the usual layers and storages within the scopes the module reads (PRODUCT, HOST, USER, APPLICATION): bundled defaults (`src/main/resources/.config/<company>/<application>/zookeeper.properties`), host- or user-wide files, other storages, and values the application stores there itself for later starts. It is *not* read from mConfig's internal `.config/metabit/mConfig/` namespace. Settings are read once, when the factory is built.
Using a separate file is recommended for modularity and to keep the main `mconfig.properties` focused on core library settings.

### Feature Settings

| Key                     | Type    | Default     | Description                                                                 |
|-------------------------|---------|-------------|-----------------------------------------------------------------------------|
| `CONNECT_STRING`        | STRING  | -           | The ZooKeeper connection string (e.g., `localhost:2181`).                   |
| `ROOT_PATH`             | STRING  | `/mconfig`  | The root path for mConfig within ZooKeeper.                                 |
| `SESSION_TIMEOUT_MS`    | NUMBER  | `60000`     | ZooKeeper session timeout in milliseconds.                                  |
| `RETRY_BASE_SLEEP_MS`   | NUMBER  | `1000`      | Base sleep time for the retry policy in milliseconds.                       |
| `RETRY_MAX_RETRIES`     | NUMBER  | `3`         | Maximum number of retries for ZooKeeper operations.                         |
| `BOOTSTRAP_CONFIG_NAME` | STRING  | `zookeeper` | The configuration handle used to bootstrap ZooKeeper settings (indirectly). |
| `BYPASS_TEST`           | BOOLEAN | `false`     | If true, allows ZooKeeper source to be active even in `TEST_MODE`.          |

### Direct Property Mapping

The ZooKeeper storage can also be configured directly via a configuration handle (defined by `BOOTSTRAP_CONFIG_NAME`), allowing for module-specific overrides:

| Property Path                | Type    | Feature Reference        | Description                      |
|------------------------------|---------|--------------------------|----------------------------------|
| `zookeeper/connectString`    | STRING  | `CONNECT_STRING`         | The ZooKeeper connection string. |
| `zookeeper/rootPath`         | STRING  | `ROOT_PATH`              | The root path for mConfig.       |
| `zookeeper/sessionTimeoutMs` | INTEGER | `SESSION_TIMEOUT_MS`     | Session timeout.                 |
| `zookeeper/retryBaseSleepMs` | INTEGER | `RETRY_BASE_SLEEP_MS`    | Base sleep time for retry.       |
| `zookeeper/retryMaxRetries`  | INTEGER | `RETRY_MAX_RETRIES`      | Maximum retries.                 |

## Precedence and Scope

mConfig uses a **10-tier hierarchy** to resolve configuration values. By default, the `ZooKeeperConfigStorage` is intended for the `CLUSTER` or `POLICY` scopes.

1. **Scope Priority**: A value in a higher scope (e.g., `POLICY`) will always override a value in a lower scope (e.g., `CLUSTER`).
2. **Storage Priority**: If two sources exist in the same scope (e.g., both are `CLUSTER`), mConfig uses the `STORAGE_TYPE_PRIORITIES` list as a tie-breaker. 
   - `zookeeper` is prioritized over standard `files` and `JAR` storages when they share a scope.

## Usage

Add the module as a dependency to your project. mConfig will automatically discover the ZooKeeper storage via JPMS or `ServiceLoader`.

To activate ZooKeeper-based configuration for your application, you can:
1.  Configure the ZooKeeper connection via `mconfig.properties` or a separate `zookeeper.properties` file.
2.  Add ZooKeeper-based search locations to your configuration setup (e.g., in `mconfig.properties` or during `ConfigFactoryBuilder` initialization).

Example `zookeeper.properties`:
```properties
# Configure ZooKeeper connection
CONNECT_STRING=zk1.internal:2181,zk2.internal:2181,zk3.internal:2181
ROOT_PATH=/apps/myApp
```

For detailed information on how mConfig resolves values across different storages and scopes, refer to the [mConfig Core documentation](documentation/src/site/markdown/22_priorities_and_hierarchies.md).

## Security and Memory Safety

The ZooKeeper module adheres to mConfig's security standards for distributed environments:

- **Format Detection Security**: The module performs format detection (e.g., identifying JSON or YAML) directly on `byte[]` buffers. This avoids creating potentially sensitive `String` objects of the entire configuration blob in the JVM heap.
- **Wiping**: While ZooKeeper data is typically persistent in the cluster, the module supports the `erase()` lifecycle to clear any in-memory cached layers holding data fetched from ZooKeeper.
- **Redaction**: Configuration entries marked as secrets in the Schema are automatically redacted in all logging and diagnostic outputs.

## Testing and Test Mode

By default, the ZooKeeper source is **disabled** when `ConfigFeature.TEST_MODE` is set to `true`. 
This prevents unit tests from accidentally connecting to real ZooKeeper clusters, 
which can cause slow timeouts or unintended side effects.

### Using Mocks in Tests

To use ZooKeeper in a test environment (e.g., with a mock cluster or a local container), you must:

1.  Enable test mode: `builder.setFeature(ConfigFeature.TEST_MODE, true)`
2.  Bypass the test safety check: `builder.setFeature(ZooKeeperFeatures.BYPASS_TEST, true)`
3.  Point to your mock server: `builder.setFeature(ZooKeeperFeatures.CONNECT_STRING, "localhost:2181")`

```java
ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
    .setFeature(ConfigFeature.TEST_MODE, true)
    .setFeature(ZooKeeperFeatures.BYPASS_TEST, true)
    .setFeature(ZooKeeperFeatures.CONNECT_STRING, "localhost:2181")
    .build();
```

## License

This module is licensed under the Apache License, Version 2.0 (see `LICENSE`). It depends on mConfig core, which has its own license (`mConfigCore/LICENSE.md`).
