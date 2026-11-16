# mConfigSourceVault

<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigSourceVault",
  "description": "mConfig module providing a HashiCorp Vault storage for mConfig (a `ConfigStorageInterface` implementation). Supports KV engines (v1/v2) and Kubernetes authentication.",
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
  "keywords": "hashicorp-vault, secrets-management, kubernetes-auth"
}
</script>
-->

This module provides a HashiCorp Vault storage for mConfig (a `ConfigStorageInterface` implementation). It is opt-in: neither `mconfigbasic` nor `mconfigstandard` contains it.

## Module Footprint
- **Jar Size**: ~25 KB (Storage logic only)
- **Effective Code Size (with dependencies)**: ~0.22 MB
    - *Vault Java Driver (~220 KB)*: Light-weight driver for Vault.
    - *Dependencies*: Uses core `mConfig` dependencies for HTTP/JSON if available, otherwise standalone.

## Configuration

The module reads its settings from the application's configuration named `vault` (changeable with
`VAULT_BOOTSTRAP_CONFIG_NAME`). It is an ordinary mConfig configuration of the application, resolved through
the usual layers and storages within the scopes the module reads (PRODUCT, HOST, USER, APPLICATION; so it never
depends on a source that is not initialised yet): bundled defaults
(`src/main/resources/.config/<company>/<app>/vault.properties`), host- or user-wide files
(e.g. `~/.config/<company>/<app>/vault.properties`), other storages, and values the application stores there
itself for later starts. It is *not* read from mConfig's internal `.config/metabit/mConfig/` namespace.
Settings are read once, when the factory is built.

### Activation

**The Vault storage is inactive unless `address` is set.** There is no default address: the storage
sends credentials to Vault, so it only talks to an endpoint you configured. Without an address it logs
one INFO line and stays out of the way, even if the module is on the classpath.

In `TEST_MODE`, the storage is inactive unless `VAULT_BYPASS_TEST` is set (see [Testing](#testing-and-test-mode)).

### Feature Settings

| Key                           | Type    | Default | Description                                                    |
|-------------------------------|---------|---------|----------------------------------------------------------------|
| `VAULT_BOOTSTRAP_CONFIG_NAME` | STRING  | `vault` | The configuration handle used to bootstrap Vault settings.     |
| `VAULT_BYPASS_TEST`           | BOOLEAN | `false` | If true, allows Vault source to be active even in `TEST_MODE`. |

### Direct Property Mapping

The Vault storage can be configured directly via the configuration handle (defined by `VAULT_BOOTSTRAP_CONFIG_NAME`):

| Key                           | Type    | Default          | Description                                                                                         |
|-------------------------------|---------|------------------|-----------------------------------------------------------------------------------------------------|
| `address`                     | URI     | -                | The URL of the Vault server. Without it, the storage is inactive.                                   |
| `engine`                      | STRING  | `secret`         | The path to the KV secret engine.                                                                   |
| `kvVersion`                   | NUMBER  | `2`              | The version of the KV secret engine (1 or 2).                                                       |
| `auth.kubernetes.role`        | STRING  | application name | The Vault role for Kubernetes authentication.                                                       |
| `auth.kubernetes.mount`       | STRING  | `kubernetes`     | The mount path of the Kubernetes auth method in Vault.                                              |
| `auth.kubernetes.tokenPath`   | STRING  | see below        | The ServiceAccount token file sent to Vault.                                                        |
| `auth.allowInsecureTransport` | BOOLEAN | `false`          | Allow Kubernetes login over plain `http`, and `VAULT_SSL_VERIFY`. Development and tests only.        |
| `token`                       | STRING  | -                | Static Vault token. If set, it is used as given and no Kubernetes login is made. Secret.            |
| `pollInterval`                | NUMBER  | `60000`          | Polling interval in ms to check for updates (0 to disable).                                         |

### Kubernetes Authentication

**Which credential is used:** an explicitly configured token (`token`, or `VaultConfigSource.builder().withToken(...)`)
is used as given. Only without one, and if a ServiceAccount token file is present, the storage logs in to Vault with that file.
Without either, the driver uses `VAULT_TOKEN`.

If no token is configured and a ServiceAccount token file is present, the storage logs in to Vault with it
(`auth/<auth.kubernetes.mount>/login`, role `auth.kubernetes.role`). The token file is:

1. `auth.kubernetes.tokenPath`, if set;
2. otherwise `KUBERNETES_SA_TOKEN_PATH`, if set (shared with the Kubernetes modules);
3. otherwise `/var/run/secrets/kubernetes.io/serviceaccount/token`.

The file is read fresh at each login and wiped from memory afterwards.

**Recommended:** use a projected ServiceAccount token issued for Vault, rather than the general token
whose audience is the Kubernetes API server:

```yaml
# pod spec
volumes:
  - name: vault-token
    projected:
      sources:
        - serviceAccountToken:
            path: token
            audience: vault          # must match the audience of your Vault role
            expirationSeconds: 600
containers:
  - volumeMounts:
      - name: vault-token
        mountPath: /var/run/secrets/vault
```

```properties
# vault.properties
address=https://vault.example.com:8200
auth.kubernetes.role=myapp
auth.kubernetes.tokenPath=/var/run/secrets/vault/token
```

**Transport security:** the ServiceAccount token is sent only over `https`. With an `http` address the
login is skipped (logged at INFO). That is the expected case for a local Vault Agent sidecar, which
authenticates by itself; mConfig then sends no token. For local development and tests,
`auth.allowInsecureTransport=true` permits the login over `http`. A static `token` over `http` is sent,
with a WARNING in the log.

A failed login is logged as a WARNING; the next refused read tries a fresh login.

**Expired tokens:** with Kubernetes auth, a read that Vault refuses (HTTP 403) is checked with
`auth/token/lookup-self` (allowed by Vault's `default` policy). If the token is still valid, the policy
does not allow the path, and that is reported without a new login. Otherwise the storage logs in once
more (the token file is read again) and retries the read once. A static `token` cannot be renewed by mConfig;
a refused read is then reported as below. Proactive renewal before the token expires is planned for 0.9.1.

**Unavailable Vault:** a read that fails (unreachable, refused, server error) is logged as a WARNING and
recorded as a config event of kind `SOURCE_UNAVAILABLE` naming the location, as is a failed start of the
storage. Lower-precedence sources may then supply the values; the event makes that visible.
A path with nothing stored in it (HTTP 404) is not a failure.

### Environment Variables

mConfig configures the Vault connection itself. Of the environment variables that the underlying
Vault driver would read on its own, only these are passed through:

| Variable                                    | Effect                                                                                  |
|---------------------------------------------|-----------------------------------------------------------------------------------------|
| `VAULT_TOKEN`                               | Static token, if neither `token` nor Kubernetes auth provides one. The file `~/.vault-token` is not read. |
| `VAULT_SSL_CERT`                            | PEM file of the CA certificate to trust.                                                |
| `VAULT_OPEN_TIMEOUT`, `VAULT_READ_TIMEOUT`  | Connection timeouts in seconds.                                                         |
| `VAULT_SSL_VERIFY`                          | Only with `auth.allowInsecureTransport=true`, since `false` disables TLS verification.  |

`VAULT_ADDR` is **not** used in this version; set `address` instead.

## Precedence and Scope

mConfig uses a **10-tier hierarchy** to resolve configuration values. The module's storage is `VaultConfigStorage`; `VaultConfigSource` is the location-side class that places it in the search list. Its default scope is `CLOUD`; `VaultConfigSource.builder().withScope(...)` places a source in another scope. For placing one at `POLICY` scope, see [Where secrets come from](../documentation/src/site/markdown/35_handling_secrets.md#356-where-secrets-come-from).

1. **Scope Priority**: A value in a higher scope (e.g., `POLICY`) will always override a value in a lower scope (e.g., `CLOUD` or `USER`).
2. **Storage Priority**: If two sources exist in the same scope (e.g., both are `CLOUD`), mConfig uses the `STORAGE_TYPE_PRIORITIES` list as a tie-breaker. 
   - `vault` is prioritized over standard `files` and `JAR` storages to ensure centralized secrets are preferred over local defaults when they share a scope.

## Usage

Add the module as a dependency to your project. mConfig will automatically discover the Vault storage via JPMS or `ServiceLoader`.

To activate Vault-based configuration for your application, you can:
1.  Configure the Vault connection in a `vault.properties` (see [Configuration](#configuration)). Environment variables (SESSION scope) are not read for these settings.
2.  Add Vault-based search locations to your configuration setup (e.g., in `mconfig.properties` or during `ConfigFactoryBuilder` initialization).

A source built with `VaultConfigSource.builder()` brings its own storage instance with its own `address`
and `token`; `ConfigFactoryBuilder.addSource()` initialises it with the factory (since 0.8.16; before,
such a source read nothing). The other settings (`engine`, `kvVersion`, Kubernetes auth) come from the
bootstrap configuration as usual.

Example `vault.properties`:
```properties
# Configure Vault connection; without an address the storage stays inactive
address=https://vault.internal:8200
token=s.your-token
```

For detailed information on how mConfig resolves values across different storages and scopes, refer to the [mConfig Core documentation](../documentation/src/site/markdown/22_priorities_and_hierarchies.md).

## Security and Memory Safety

This module is designed for handling high-sensitivity data and follows strict **Memory Safety** principles:

- **Byte Array Storage**: Retrieved secrets are stored internally as `byte[]` rather than `String` to prevent sensitive data from being interned in the JVM heap.
- **Explicit Wiping**: The module implements the `erase()` lifecycle. When the configuration is closed, all internal buffers holding secrets and authentication tokens are zero-filled.
- **Secure Authentication**: When using Kubernetes authentication, the ServiceAccount token is handled as a `byte[]` and wiped immediately after use. It is sent only to a configured address, and only over `https` unless explicitly allowed.
- **Redaction**: Vault-sourced entries are automatically flagged as secrets, ensuring they are redacted in logs and `toString()` outputs.

## Testing and Test Mode

By default, the Vault source is **disabled** when `ConfigFeature.TEST_MODE` is set to `true`,
also for sources added with `VaultConfigSource.builder()` and `addSource()`.
This prevents unit tests from accidentally connecting to real Vault servers, 
which can cause slow timeouts or unintended side effects.

### Using Mocks in Tests

To use Vault in a test environment (e.g., with a mock server or a local container), you must:

1.  Enable test mode: `builder.setFeature(ConfigFeature.TEST_MODE, true)`
2.  Bypass the test safety check: `builder.setFeature(VaultFeatures.BYPASS_TEST, true)`
3.  Point to your mock server in `src/test/resources/.config/<company>/<app>/vault.properties`:
    `address=http://localhost:8200`, plus `auth.allowInsecureTransport=true` if the test uses Kubernetes login over `http`.

```java
ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
    .setFeature(ConfigFeature.TEST_MODE, true)
    .setFeature(VaultFeatures.BYPASS_TEST, true)
    .build();
```

### Bootstrap Configuration

The Vault source performs self-configuration in `postInit()` using a dedicated bootstrap config named `vault` (configurable).

Place your Vault connection settings in any of the application's local mConfig locations using the `vault` config name, for example:

```properties
# ~/.config/<company>/<app>/vault.properties
address=https://vault.mycompany.com:8200
engine=secret
kvVersion=2
pollInterval=30000

# Kubernetes auth (recommended for pods)
auth.kubernetes.role=myapp-prod

# or a static token (local development / CI); if set, it is used instead of the Kubernetes login
# token=hvs.xxxx...   # treat as secret!
```

## License

This module is licensed under the Apache License, Version 2.0 (see `LICENSE`). It depends on mConfig core, which has its own license (`mConfigCore/LICENSE.md`).
