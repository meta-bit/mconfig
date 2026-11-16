# mConfigSourceAwsSecretsManager

<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigSourceAwsSecretsManager",
  "description": "mConfig module providing an AWS Secrets Manager storage for mConfig (a `ConfigStorageInterface` implementation). Supports regional secret retrieval and polling.",
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
  "keywords": "aws-secrets-manager, cloud-config, secrets-management"
}
</script>
-->

This module provides an AWS Secrets Manager storage for mConfig (a `ConfigStorageInterface` implementation). It is opt-in: neither `mconfigbasic` nor `mconfigstandard` contains it.

## Module Footprint
- **Jar Size**: ~22 KB (Storage logic only)
- **Effective Code Size (with dependencies)**: ~8.37 MB
    - *AWS SDK Secrets Manager (~8.3 MB)*: SDK for Secrets Manager.
    - *Reactive Streams (~0.02 MB)*: Required by AWS SDK.
    - *EventStream (~0.05 MB)*: Required by AWS SDK.

## Configuration

The AWS source is self-configuring through mConfig using the configuration handle `aws`. 
The module reads its settings from the application's configuration named `aws` (changeable with `AWS_BOOTSTRAP_CONFIG_NAME`). It is an ordinary mConfig configuration of the application, resolved through the usual layers and storages within the scopes the module reads (PRODUCT, HOST, USER, APPLICATION): bundled defaults (`src/main/resources/.config/<company>/<application>/aws.properties`), host- or user-wide files, other storages, and values the application stores there itself for later starts. It is *not* read from mConfig's internal `.config/metabit/mConfig/` namespace. Settings are read once, when the factory is built.
Using a separate file is recommended for modularity and to keep the main `mconfig.properties` focused on core library settings.

### Feature Settings

| Key                         | Type    | Default | Description                                                  |
|-----------------------------|---------|---------|--------------------------------------------------------------|
| `AWS_BOOTSTRAP_CONFIG_NAME` | STRING  | `aws`   | The configuration handle used to bootstrap AWS settings.     |
| `AWS_BYPASS_TEST`           | BOOLEAN | `false` | If true, allows AWS source to be active even in `TEST_MODE`. |

### Direct Property Mapping

The AWS storage can be configured directly via the configuration handle (defined by `AWS_BOOTSTRAP_CONFIG_NAME`):

| Key            | Type    | Default  | Description                                                                                                |
|----------------|---------|----------|------------------------------------------------------------------------------------------------------------|
| `region`       | STRING  | -        | The AWS region to use (e.g., `us-east-1`). If not provided, the default AWS region provider chain is used. |
| `pollInterval` | INTEGER | `300000` | Polling interval in ms to check for updates (0 to disable).                                                |

## Precedence and Scope

mConfig uses a **10-tier hierarchy** to resolve configuration values. Sources of this module default to the `CLOUD` scope; `AwsSecretsManagerConfigSource.builder().withScope(...)` places one in another scope. A source the application registers at `POLICY` scope fails in productive mode unless `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code (see Config Features, "POLICY locations registered by the application").

1. **Scope Priority**: A value in a higher scope (e.g., `POLICY`) will always override a value in a lower scope (e.g., `CLOUD` or `USER`).
2. **Storage Priority**: If two sources exist in the same scope (e.g., both are `CLOUD`), mConfig uses the `STORAGE_TYPE_PRIORITIES` list as a tie-breaker. 
   - `aws-secrets` is prioritized over standard `files` and `JAR` storages to ensure centralized secrets are preferred over local defaults when they share a scope.

## Usage

Add the module as a dependency to your project. mConfig will automatically discover the AWS Secrets Manager storage via JPMS or `ServiceLoader`.

To activate AWS-based configuration for your application, you can:
1.  Configure the AWS connection (e.g., region) via `mconfig.properties`, environment variables, or other standard mConfig sources.
2.  Add AWS-based search locations to your configuration setup (e.g., in `mconfig.properties` or during `ConfigFactoryBuilder` initialization).

A source built with `AwsSecretsManagerConfigSource.builder()` brings its own storage instance with its
own `region`; `ConfigFactoryBuilder.addSource()` initialises it with the factory (since 0.8.16; before,
such a source read nothing).

A secret that cannot be read (no access, network failure, client failed to start) is logged as a WARNING
and recorded as a config event of kind `SOURCE_UNAVAILABLE` naming the location. Lower-precedence
sources may then supply the values; the event makes that visible. A secret that does not exist is not a
failure.

Example `aws.properties`:
```properties
# Configure AWS region
region=us-east-1
# global default. europe e.g. would be eu-central-1
```

For detailed information on how mConfig resolves values across different storages and scopes, refer to the [mConfig Core documentation](documentation/src/site/markdown/22_priorities_and_hierarchies.md).

## Security and Memory Safety

This module provides high-level security for cloud-based secrets and follows strict **Memory Safety** principles:

- **Byte Array Storage**: Retrieved secrets are decoded and stored internally as `byte[]` rather than `String` to prevent sensitive data from being interned in the JVM heap.
- **Explicit Wiping**: The module implements the `erase()` lifecycle. When the configuration is closed, all internal buffers holding secrets are zero-filled.
- **Safe JSON Parsing**: If a secret contains multiple keys in JSON format, the module parses it directly into byte arrays, minimizing the creation of immutable `String` objects for sensitive values.
- **Redaction**: AWS-sourced entries are automatically flagged as secrets, ensuring they are redacted in logs and `toString()` outputs.

## Testing and Test Mode

By default, the AWS Secrets Manager source is **disabled** when `ConfigFeature.TEST_MODE` is set to `true`, also for sources added with `AwsSecretsManagerConfigSource.builder()` and `addSource()`. This prevents unit tests from accidentally connecting to real AWS services, which can cause slow timeouts or unintended side effects.

### Using Mocks in Tests

To use AWS Secrets Manager in a test environment (e.g., with LocalStack or a custom mock), you must:

1.  Enable test mode: `builder.setFeature(ConfigFeature.TEST_MODE, true)`
2.  Bypass the test safety check: `builder.setFeature(AwsFeatures.BYPASS_TEST, true)`
3.  Configure a mock endpoint or region (e.g., `aws/region=us-east-1` in `src/test/resources/mconfig.properties`).

```java
ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
    .setFeature(ConfigFeature.TEST_MODE, true)
    .setFeature(AwsFeatures.BYPASS_TEST, true)
    .build();
```

## License

This module is licensed under the Apache License, Version 2.0 (see `LICENSE`). It depends on mConfig core, which has its own license (`mConfigCore/LICENSE.md`).
