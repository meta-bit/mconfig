# mConfigSourceZooKeeper

> [!WARNING]
> This module is currently **EXPERIMENTAL**. The API and configuration structure may change in future releases. Feedback and bug reports are welcome.

This module provides an Apache ZooKeeper implementation of `ConfigSource` for mConfig. It allows for hierarchical configuration management within a ZooKeeper cluster with real-time updates through watchers.

## Configuration

The ZooKeeper source is self-configuring through mConfig using the configuration handle `zookeeper`. 
Settings can be provided via any lower-precedence source, such as `mconfig.properties` OR a separate file like `zookeeper.properties` in the standard `.config/metabit/mConfig/` resource directory.
Using a separate file is recommended for modularity and to keep the main `mconfig.properties` focused on core library settings.

### Feature Settings

| Key                     | Type   | Default     | Description                                                                 |
|-------------------------|--------|-------------|-----------------------------------------------------------------------------|
| `CONNECT_STRING`        | STRING | -           | The ZooKeeper connection string (e.g., `localhost:2181`).                   |
| `ROOT_PATH`             | STRING | `/mconfig`  | The root path for mConfig within ZooKeeper.                                 |
| `SESSION_TIMEOUT_MS`    | NUMBER | `60000`     | ZooKeeper session timeout in milliseconds.                                  |
| `RETRY_BASE_SLEEP_MS`   | NUMBER | `1000`      | Base sleep time for the retry policy in milliseconds.                       |
| `RETRY_MAX_RETRIES`     | NUMBER | `3`         | Maximum number of retries for ZooKeeper operations.                         |
| `BOOTSTRAP_CONFIG_NAME` | STRING | `zookeeper` | The configuration handle used to bootstrap ZooKeeper settings (indirectly). |

### Direct Property Mapping

The ZooKeeper storage can also be configured directly via a configuration handle (defined by `BOOTSTRAP_CONFIG_NAME`):

| Property Path                | Type    | Description                      |
|------------------------------|---------|----------------------------------|
| `zookeeper/connectString`    | STRING  | The ZooKeeper connection string. |
| `zookeeper/rootPath`         | STRING  | The root path for mConfig.       |
| `zookeeper/sessionTimeoutMs` | INTEGER | Session timeout.                 |
| `zookeeper/retryBaseSleepMs` | INTEGER | Base sleep time for retry.       |
| `zookeeper/retryMaxRetries`  | INTEGER | Maximum retries.                 |

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
