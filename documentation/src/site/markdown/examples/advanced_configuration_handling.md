# Advanced Configuration Handling

This page collects higher-level patterns for controlling how mConfig resolves, validates, and writes configuration data.


## 1. Write with scope control
Writes are scoped and follow a "least surprise" policy:

```java
Configuration cfg = factory.getConfiguration("network");

cfg.put("server/port", 8081, ConfigScope.USER);
```


## 2. Control file format order
Reading and writing can use different format orders:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES,
            List.of("TOML", "YAML", "JSON", "properties"))
        .setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES,
            List.of("TOML", "YAML", "JSON"));
```

## 3. Use strict schemes
To reject unknown keys, turn on strict mode:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.SCHEME_STRICT_MODE, true);
```

## 4. Missing values behavior
If you want scheme defaults instead of exceptions:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
        .setFeature(ConfigFeature.DEFAULT_ON_MISSING_ENTRY, true);
```

## 5. Subscribe to updates
React when values change at runtime:

```java
Configuration cfg = factory.getConfiguration("network");

cfg.subscribeToUpdates(location -> {
    System.out.println("Config changed in scope: " + location.getScope());
});
```

NB: The changes are automatically propagated to the mConfig Configurations anyhow.
If you take the values from the Configurations directly, they will be up-to-date.
Above subscription is to support triggering actions when the configuration changes,
e.g. thread restarts.

## 6. Control scope fallbacks
By default, mConfig falls back to less specific scopes when a value is missing. You can change that:

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.FALLBACKS_ACROSS_SCOPES, false)
        .build())
    {
    Configuration cfg = factory.getConfiguration("network");
    // Only the requested scope (plus PRODUCT defaults) is considered.
    }
```

## 7. Prioritize storage types
If multiple storage backends are available, change their priority within a scope:

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.STORAGE_TYPE_PRIORITIES,
            List.of("files", "registry", "JAR"))
        .build())
    {
    // Files override registry, registry overrides JAR defaults.
    }
```

## 8. Use secrets providers
If you have secrets in schemes, add a secrets provider to resolve them:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.SECRETS_PROVIDER_ID, "my-secrets")
        .setSecretsProviderConfig(Map.of("endpoint", "https://vault.example"));
```

Related docs:
- [Config Features](../25_config_features.md)
- [Priorities and Hierarchies](../22_priorities_and_hierarchies.md)
- [Writing Configurations](../31_writing_configurations.md)
