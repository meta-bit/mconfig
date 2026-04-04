# mConfigSourceAwsSecretsManager

This module provides an AWS Secrets Manager implementation of `ConfigSource` for mConfig.

## Configuration

The AWS source is self-configuring through mConfig using the configuration handle `aws`. 
Settings can be provided via any lower-precedence source, such as `mconfig.properties` OR a separate file like `aws.properties` in the standard `.config/metabit/mConfig/` resource directory.
Using a separate file is recommended for modularity and to keep the main `mconfig.properties` focused on core library settings.

### Feature Settings

| Key                         | Type   | Default | Description                                              |
|-----------------------------|--------|---------|----------------------------------------------------------|
| `AWS_BOOTSTRAP_CONFIG_NAME` | STRING | `aws`   | The configuration handle used to bootstrap AWS settings. |

### Direct Property Mapping

The AWS storage can be configured directly via the configuration handle (defined by `AWS_BOOTSTRAP_CONFIG_NAME`):

| Key            | Type    | Default  | Description                                                                                                |
|----------------|---------|----------|------------------------------------------------------------------------------------------------------------|
| `region`       | STRING  | -        | The AWS region to use (e.g., `us-east-1`). If not provided, the default AWS region provider chain is used. |
| `pollInterval` | INTEGER | `300000` | Polling interval in ms to check for updates (0 to disable).                                                |

## Precedence and Scope

mConfig uses a **10-tier hierarchy** to resolve configuration values. By default, the `AwsSecretsManagerConfigSource` is intended for the `CLOUD` or `POLICY` scopes.

1. **Scope Priority**: A value in a higher scope (e.g., `POLICY`) will always override a value in a lower scope (e.g., `CLOUD` or `USER`).
2. **Storage Priority**: If two sources exist in the same scope (e.g., both are `CLOUD`), mConfig uses the `STORAGE_TYPE_PRIORITIES` list as a tie-breaker. 
   - `aws-secrets` is prioritized over standard `files` and `JAR` storages to ensure centralized secrets are preferred over local defaults when they share a scope.

## Usage

Add the module as a dependency to your project. mConfig will automatically discover the AWS Secrets Manager storage via JPMS or `ServiceLoader`.

To activate AWS-based configuration for your application, you can:
1.  Configure the AWS connection (e.g., region) via `mconfig.properties`, environment variables, or other standard mConfig sources.
2.  Add AWS-based search locations to your configuration setup (e.g., in `mconfig.properties` or during `ConfigFactoryBuilder` initialization).

Example `aws.properties`:
```properties
# Configure AWS region
region=us-east-1
# global default. europe e.g. would be eu-central-1
```

For detailed information on how mConfig resolves values across different storages and scopes, refer to the [mConfig Core documentation](documentation/src/site/markdown/22_priorities_and_hierarchies.md).
