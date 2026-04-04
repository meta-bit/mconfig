# mConfigSourceVault

This module provides a HashiCorp Vault implementation of `ConfigSource` for mConfig.

## Configuration

The Vault source is self-configuring through mConfig using the configuration handle `vault`. 
Settings can be provided via any lower-precedence source, such as `mconfig.properties` OR a separate file like `vault.properties` in the standard `.config/metabit/mConfig/` resource directory.
Using a separate file is recommended for modularity and to keep the main `mconfig.properties` focused on core library settings.

### Feature Settings

| Key                           | Type   | Default | Description                                                |
|-------------------------------|--------|---------|------------------------------------------------------------|
| `VAULT_BOOTSTRAP_CONFIG_NAME` | STRING | `vault` | The configuration handle used to bootstrap Vault settings. |

### Direct Property Mapping

The Vault storage can be configured directly via the configuration handle (defined by `VAULT_BOOTSTRAP_CONFIG_NAME`):

| Key                     | Type    | Default                 | Description                                                      |
|-------------------------|---------|-------------------------|------------------------------------------------------------------|
| `address`               | STRING  | `http://127.0.0.1:8200` | The URL of the Vault server. (MANDATORY)                         |
| `engine`                | STRING  | `secret`                | The path to the KV secret engine.                                |
| `kvVersion`             | INTEGER | `2`                     | The version of the KV secret engine (e.g., 1 or 2).              |
| `auth.kubernetes.role`  | STRING  | `default`               | The Vault role for Kubernetes authentication.                    |
| `auth.kubernetes.mount` | STRING  | `kubernetes`            | The mount path for Kubernetes auth.                              |
| `token`                 | STRING  | -                       | Static Vault token (fallback if Kubernetes auth is unavailable). |
| `pollInterval`          | INTEGER | `60000`                 | Polling interval in ms to check for updates (0 to disable).      |

## Precedence and Scope

mConfig uses a **10-tier hierarchy** to resolve configuration values. By default, the `VaultConfigSource` is intended for the `CLOUD` or `POLICY` scopes.

1. **Scope Priority**: A value in a higher scope (e.g., `POLICY`) will always override a value in a lower scope (e.g., `CLOUD` or `USER`).
2. **Storage Priority**: If two sources exist in the same scope (e.g., both are `CLOUD`), mConfig uses the `STORAGE_TYPE_PRIORITIES` list as a tie-breaker. 
   - `vault` is prioritized over standard `files` and `JAR` storages to ensure centralized secrets are preferred over local defaults when they share a scope.

## Usage

Add the module as a dependency to your project. mConfig will automatically discover the Vault storage via JPMS or `ServiceLoader`.

To activate Vault-based configuration for your application, you can:
1.  Configure the Vault connection via `mconfig.properties`, environment variables, or other standard mConfig sources.
2.  Add Vault-based search locations to your configuration setup (e.g., in `mconfig.properties` or during `ConfigFactoryBuilder` initialization).

Example `vault.properties`:
```properties
# Configure Vault connection
address=https://vault.internal:8200
token=s.your-token
```

For detailed information on how mConfig resolves values across different storages and scopes, refer to the [mConfig Core documentation](documentation/src/site/markdown/22_priorities_and_hierarchies.md).
