# Integration with CI/CD

This example shows how to make mConfig predictable in CI/CD and automated pipelines.

## 1. Enable Test Mode via environment variables
mConfig can enable Test Mode without code changes when `ALLOW_MCONFIG_RUNTIME_SETTINGS` is on.

```bash
export MCONFIG_RUNTIME_TEST_MODE=true
export MCONFIG_RUNTIME_DEBUG_LEVEL=debug
```

In code, allow runtime settings:
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true)
        .build())
    {
    Configuration cfg = factory.getConfiguration("network");
    }
```

## 2. Keep test configs in test resources
CI usually runs tests from the build tree. Keep your test configs here:
- `src/test/resources/.config/ACME/ourApp/network.properties`

## 3. Prefer configuration schemes in CI
Schemes are recommended in CI because they:
1. Prevent most cases of incomplete configs by providing defaults.
2. Enforce mandatory values and validation rules early.

Add a scheme in test resources:
- `src/test/resources/.config/ACME/ourApp/network.scheme.json`

```json
[
  { "KEY": "server/host", "TYPE": "STRING", "DEFAULT": "127.0.0.1" },
  { "KEY": "server/port", "TYPE": "NUMBER", "DEFAULT": 8080 },
  { "KEY": "server/tls", "TYPE": "BOOLEAN", "DEFAULT": false }
]
```

## 4. Use explicit test directories (optional)
If your CI environment mounts a special directory, point mConfig at it:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setTestMode(true)
        .setFeature(ConfigFeature.TESTMODE_DIRECTORIES,
            List.of("USER:/mnt/ci/config/user", "APPLICATION:/mnt/ci/config/app"));
```

## 5. Pin formats and storage priority (optional)
Avoid surprises from environment changes by pinning priorities:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES,
            List.of("TOML", "YAML", "JSON", "properties"))
        .setFeature(ConfigFeature.STORAGE_TYPE_PRIORITIES,
            List.of("files", "JAR"));
```

Related docs:
- [Test Mode](../test-mode.md)
- [Configuration Schemes](../configuration-schemes.md)
- [Config Features](../4_1_ConfigFeatures.md)
