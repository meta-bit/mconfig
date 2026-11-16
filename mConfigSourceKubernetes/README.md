<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigSourceKubernetes",
  "description": "Zero-dependency mConfig module for Kubernetes. Fetches configuration from ConfigMaps and Secrets via API or filesystem, with real-time update support.",
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
  "keywords": "kubernetes, configmap, secrets, zero-dependency, change-detection"
}
</script>
-->

# mConfigSourceKubernetes

mConfig module for Kubernetes API (ConfigMaps/Secrets) and filesystem lookups.

A zero-dependency Kubernetes configuration source for mConfig.

It provides both HTTP(S)-based Direct Application Integration, 
and filesystem-based configuration (CSI) sources.

## Overview

`mConfigSourceKubernetes` provides a lightweight way to fetch configuration 
from Kubernetes `ConfigMap` and `Secret` resources. For zero external dependencies, 
it is leveraging the JDK 11 `HttpClient`for API communication
and the library's internal `JsonStreamParser` for parsing responses.

This module is ideal for:
- Slim containers and microservices.
- Applications where avoiding dependency conflicts (e.g., Jackson versions) is critical.
- Environment with strict JAR size constraints.

## Key Features

- **Zero Dependencies**: Uses only standard JDK 11+ APIs and mConfig core.
- **ConfigMap & Secret Support**: Can fetch data from both resource types.
- **Auto-Discovery**: Automatically discovers and adds standard Kubernetes filesystem paths (`/etc/config`, `/etc/secrets`) to the configuration search list.
- **In-Cluster Authentication**: Automatically uses ServiceAccount tokens and cluster CA certificates.
- **Live Updates**: Supports asynchronous change detection using Kubernetes Watches via HTTP streaming.
- **Binary Data Support**: Handles both `data` and `binaryData` fields in Kubernetes resources.
- **dynamic token rotation**: Automatically detects changes to, and reloads ServiceAccount tokens.
- **dynamic secret rotation**: Automatically detects and reloads secrets when the API server rotates them, or the files are updated atomically.
- **dynamic configmap rotation**: Automatically detects and reloads configmaps when the API server rotates them, or the files are updated atomically.

## Configuration

### Bootstrapping

The module can be configured via system properties for early bootstrap, if needed.
They are runtime settings: they only take effect if the application enabled
`ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS` in code. The same settings are available as
environment variables (`MCONFIG_RUNTIME_KUBERNETES_API_ENDPOINT`, ...).

| Property Name                                | Description               | Default Value                                           |
|----------------------------------------------|---------------------------|---------------------------------------------------------|
| mconfig.runtime.kubernetes.api.endpoint      | API server URL            | https://kubernetes.default.svc                          |
| mconfig.runtime.kubernetes.config.dir        | Custom config path(s)     | /etc/config                                             |
| mconfig.runtime.kubernetes.secrets.dir       | Custom secrets path(s)    | /etc/secrets                                            |
| mconfig.runtime.kubernetes.sa.token.path     | Path to SA token          | /var/run/secrets/kubernetes.io/serviceaccount/token     |
| mconfig.runtime.kubernetes.sa.namespace.path | Path to SA namespace      | /var/run/secrets/kubernetes.io/serviceaccount/namespace |
| mconfig.runtime.kubernetes.sa.ca.cert.path   | Path to SA CA certificate | /var/run/secrets/kubernetes.io/serviceaccount/ca.crt    |

The settings can also be placed in a configuration named `kubernetes` (e.g. `kubernetes.properties`,
keys as in the feature names, such as `KUBERNETES_API_ENDPOINT`). For this bootstrap configuration the
module reads the scopes PRODUCT, HOST, USER, APPLICATION, **SESSION and RUNTIME**. This differs
deliberately from the Vault, AWS and ZooKeeper modules, which read only the first four: in a pod, the
environment is set by the pod spec and the kubelet, so SESSION is deployment configuration rather than
foreign input, and RUNTIME carries intentional overrides from code. Neither scope is served by a source
that depends on this module, so there is no circular read.

### ConfigFeatures

You can also use `ConfigFactoryBuilder.setFeature()` to configure the module:

- `ConfigFeature.TEST_MODE`: Standard mConfig feature. When `true`, this module will disable its API client logic to prevent accidental calls to real clusters during unit tests, unless `BYPASS_TEST` is also enabled.
- `KubernetesFeatures.API_ENDPOINT`: The API server URL.
- `KubernetesFeatures.MAX_KEY_SIZE`: Maximum allowed size for a single configuration value (default: 1MB).
- `KubernetesFeatures.ALLOW_ENV_SECRETS`: If true, allows secrets to be used as environment variables (default: `false`).
- `KubernetesFeatures.BYPASS_TEST`: If true, skips the environment check and ignores `TEST_MODE` restrictions (useful for integration testing with mock servers).
- `KubernetesFeatures.CONFIG_DIR_PATHS`: Comma-separated list of directories to search for configuration files.
- `KubernetesFeatures.SECRETS_DIR_PATHS`: Comma-separated list of directories to search for secret files.
- `KubernetesFeatures.SA_TOKEN_PATH`: Path to the ServiceAccount token file.
- `KubernetesFeatures.SA_NAMESPACE_PATH`: Path to the ServiceAccount namespace file.
- `KubernetesFeatures.SA_CA_CERT_PATH`: Path to the ServiceAccount CA certificate file.
- `KubernetesFeatures.ALLOW_INSECURE_TRANSPORT` (`KUBERNETES_ALLOW_INSECURE_TRANSPORT`): If true, the ServiceAccount token is also sent to an `http` endpoint, and any server certificate is accepted when no CA certificate file is present. For local development and tests only (default: `false`).

## Usage

### Resource Handles

When adding a Kubernetes source to your configuration search list, use the following handle format:

- `[namespace/]configmap/name`
- `[namespace/]secret/name`

If the namespace is omitted, the module attempts to detect it from the configured `SA_NAMESPACE_PATH` or defaults to `default`.

### Multi-Key and Format Detection

Kubernetes ConfigMaps and Secrets can contain multiple keys. `mConfigSourceKubernetes` will:
1. Iterate over all keys in the resource.
2. **ConfigMaps:** values are read as the plain text the API serves (`binaryData` as Base64). If a key has an
   extension a structured format on the classpath claims (e.g. `.yaml`, `.json`, `.properties`), its value is
   parsed with that format, and its entries are read as keys of the configuration; include the respective mConfig
   format modules. A key without such an extension, or with one of the raw formats (`.txt`, `.cfg`, `.bin`), stays
   one entry, named as the key. 
   **Secrets** are not parsed: each key stays one secret entry, whatever its extension, since a parsed value would
   lose the secret handling below.
3. For Secrets, it automatically decodes Base64 data directly into `byte[]` buffers and handles binary data, wrapping the result in a `SecretValue` object. This avoids creating intermediate `String` objects in memory for sensitive data.

### Example

```java
ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApp")
    .build();

// The module automatically adds /etc/config and /etc/secrets (or custom paths) to the CLUSTER scope if they exist.
Configuration config = factory.getConfig("my-service");

// Accessing a value that might be in a ConfigMap
String dbHost = config.getString("db.host");
```

## Security

- **Secret Masking**: Secrets fetched from Kubernetes are handled as `SecretValue` objects, ensuring they are redacted in logs and handled via byte arrays to avoid `String` interning.
- **Memory Safety (Wiping)**: The module implements the `erase()` signal. When the `Configuration` handle is closed, the module zero-fills its internal secret buffers to ensure sensitive data is wiped from memory.
- **TLS Verification**: The module automatically loads the cluster CA certificate to verify the API server's identity. Without a CA certificate file, the JVM's default trust store applies; certificate checks are switched off only with `ALLOW_INSECURE_TRANSPORT`.
- **Token Transport**: The ServiceAccount token is sent only to an `https` API endpoint. For an `http` endpoint the module logs a WARNING and sends requests without the token, unless `ALLOW_INSECURE_TRANSPORT` is set.
- **Token Rotation**: The module monitors the ServiceAccount token file for changes. When Kubernetes rotates the token (e.g., via atomic symlink updates), the module automatically reloads it for subsequent API requests. The token is stored as a byte array and zero-filled when reloaded to minimize its presence in memory.

## Testing and Test Mode

The module interacts with the library's `ConfigFeature.TEST_MODE` to ensure safe behavior during automated tests.

### Behavior in TEST_MODE
When `ConfigFeature.TEST_MODE` is set to `true`:
- The module will **not** attempt to connect to the Kubernetes API server unless `KubernetesFeatures.BYPASS_TEST` is also set to `true`.
- It will still attempt to use the filesystem-based ConfigMap/Secret paths if they point to existing directories.
- This prevents unit tests from accidentally making network requests or failing because they are not running in a real cluster.

### Integration Testing with Mock Servers
To test Kubernetes integration in a non-cluster environment (e.g., local machine or CI):
1. Set `ConfigFeature.TEST_MODE` to `true` (standard for tests).
2. Set `KubernetesFeatures.BYPASS_TEST` to `true` to allow API interaction despite being outside a cluster.
3. Use `KubernetesFeatures.API_ENDPOINT` to point to a mock server (e.g., `MockWebServer`).
4. If the mock server uses `http` (or `https` without a CA certificate file) and should receive the token, set `KubernetesFeatures.ALLOW_INSECURE_TRANSPORT` to `true`.

Example:
```java
ConfigFactory factory = ConfigFactoryBuilder.create("test", "app")
    .setFeature(ConfigFeature.TEST_MODE, true)
    .setFeature(KubernetesFeatures.BYPASS_TEST, true)
    .setFeature(KubernetesFeatures.API_ENDPOINT, "http://localhost:8080")
    .setFeature(KubernetesFeatures.ALLOW_INSECURE_TRANSPORT, true)
    .build();
```

## Requirements

- JDK 11 or higher (for `java.net.http.HttpClient`).
- A Kubernetes environment with ServiceAccount volume projection.
- Alternatively, a Kubernetes environment with ConfigMap and/or Secret volumes mapped to the expected filesystem paths.

## Recommendations

## Validation and defaults

Use mConfig Schemas to describe the configuration keys and secrets your application is using.
They will automatically validate and set defaults at runtime,  adding a layer of validation and flexibility.
See [mConfigSchema](https://github.com/m-config/mConfigSchema) for more details.

### Reading

To make full use of mConfig providing "live updates", use the mConfig API objects.
If you store configuration values in your application's own data structures, they won't reflect changes.

### Writing

Kubernetes does not allow writing of ConfigMaps and secrets from the pods.

If your code needs to write operative data, local/temporary overrides, etc.,
you can write to the lower-level config scopes for such purposes.

If writeable directories/volumes are available, consider using the 
[mConfigSourceFileSystem](https://github.com/m-config/mConfigSourceFileSystem) module
if not already using it.

The RUNTIME scope goes to memory, is not persisted, but readily available.
(InMemoryLayerSource is part of the mConfig core.)

