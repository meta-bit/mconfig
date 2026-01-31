# ZooKeeper Configuration Source (Experimental)

The `mConfigSourceZooKeeper` module allows mConfig to retrieve configuration data from an Apache ZooKeeper ensemble. This is particularly useful for cluster-wide or organization-wide settings.

## 1. Bootstrap Configuration

Following the "self-configuration" principle, the ZooKeeper source doesn't require hardcoded connection strings in your application code. Instead, it looks for its own settings in a configuration named `zookeeper`.

You can provide these bootstrap settings in any standard mConfig location, such as a JAR resource (for defaults) or a local file (for overrides).

### Example: `.config/zookeeper.json` (Bootstrap)
Place this file in `src/main/resources/.config/zookeeper.json` or `/etc/metabit/myapp/zookeeper.json`.

```json
{
  "zookeeper": {
    "connectString": "zk-node-1:2181,zk-node-2:2181,zk-node-3:2181",
    "rootPath": "/mconfig/prod",
    "sessionTimeoutMs": 30000
  }
}
```

## 2. Data Structure in ZooKeeper

By default, the ZooKeeper source registers itself for the `CLUSTER` and `ORGANIZATION` scopes. It maps configuration names to ZNode paths relative to the `rootPath`.

If your `rootPath` is `/mconfig/prod`, and you request a configuration named `database-settings`, the source will look at:
- `/mconfig/prod/cluster/database-settings`
- `/mconfig/prod/organization/database-settings`

The content of the ZNode should be in a supported format (like JSON or Java Properties).

### Example: ZNode content at `/mconfig/prod/cluster/app-config`
```json
{
  "database": {
    "host": "prod-db-server",
    "port": 5432
  },
  "features": {
    "enableNewDashboard": true
  }
}
```

## 3. Usage in Code

To use the ZooKeeper source, simply include the `mConfigSourceZooKeeper` module in your classpath. mConfig will automatically discover and initialize it.

```java
// Standard initialization
try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "myapp").build())
    {
    // Requesting a configuration that might be in ZooKeeper
    Configuration config = factory.getConfig("app-config");

    // Accessing values (mConfig handles the layering)
    String dbHost = config.getString("database/host");
    boolean useDashboard = config.getBoolean("features/enableNewDashboard");
    }
```

## 4. How it Works (Self-Configuration)

The ZooKeeper source follows a specific startup sequence:

1. **Local Initialization**: `JARConfigSource` and `FileConfigStorage` are initialized first.
2. **Bootstrap Lookup**: `ZooKeeperConfigStorage` calls `factory.getConfig("zookeeper")`. This request is resolved using the already initialized local sources.
3. **Activation**: If a `connectString` is found, the storage connects to ZooKeeper and adds its networked locations to the global search list.
4. **Transparent Layering**: Subsequent calls to `factory.getConfig("app-config")` will now automatically include layers found in ZooKeeper, prioritized by scope (e.g., `CLUSTER` settings will override `DEFAULT` settings from the JAR).

## 5. Test Mode Isolation

In `TEST_MODE`, the ZooKeeper source will naturally point to your test resources (e.g., `src/test/resources/.config/zookeeper.json`). This ensures that unit tests do not accidentally connect to production ZooKeeper ensembles.
