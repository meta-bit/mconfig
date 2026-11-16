# mConfigSourceKubernetesFabric8

mConfig module for Kubernetes API (ConfigMaps/Secrets) and filesystem lookups using the **Fabric8 Kubernetes Client**.

## Overview

`mConfigSourceKubernetesFabric8` provides a robust implementation of the Kubernetes configuration source using the well-known Fabric8 client library. This is a "heavy" alternative to the zero-dependency `mConfigSourceKubernetes` module.

This module is ideal for:
- Environments where the Fabric8 client is already in use.
- Complex Kubernetes setups where advanced client features are needed.

## Key Features

- **Fabric8 Client**: Leverages a mature and feature-rich Kubernetes client.
- **ConfigMap & Secret Support**: Can fetch data from both resource types.
- **Auto-Discovery**: Automatically discovers and adds standard Kubernetes filesystem paths (`/etc/config`, `/etc/secrets`).
- **Live Updates**: Supports change detection via Kubernetes Watches.

## Configuration

To use this module, you must explicitly include it in your dependencies, as it is not the default Kubernetes source.

### ConfigFeatures

You can use `ConfigFactoryBuilder.setFeature()` to configure the module. The same keys are read from the
bootstrap configuration `kubernetes` (local scopes, SESSION and RUNTIME, as in `mConfigSourceKubernetes`).

| Feature                                  | Key                                   | Default                                                    |
|------------------------------------------|---------------------------------------|------------------------------------------------------------|
| `KubernetesFabric8Features.API_ENDPOINT` | `KUBERNETES_API_ENDPOINT`             | `https://kubernetes.default.svc`                           |
| `NAMESPACE`                              | `KUBERNETES_NAMESPACE`                | the ServiceAccount namespace (`SA_NAMESPACE_PATH`)         |
| `SA_TOKEN_PATH`                          | `KUBERNETES_SA_TOKEN_PATH`            | `/var/run/secrets/kubernetes.io/serviceaccount/token`      |
| `SA_NAMESPACE_PATH`                      | `KUBERNETES_SA_NAMESPACE_PATH`        | `/var/run/secrets/kubernetes.io/serviceaccount/namespace`  |
| `SA_CA_CERT_PATH`                        | `KUBERNETES_SA_CA_CERT_PATH`          | `/var/run/secrets/kubernetes.io/serviceaccount/ca.crt`     |
| `ALLOW_INSECURE_TRANSPORT`               | `KUBERNETES_ALLOW_INSECURE_TRANSPORT` | `false`                                                    |

The client is configured from these settings only. Fabric8's own auto-configuration (kubeconfig file,
`KUBERNETES_*` environment variables, `kubernetes.*` system properties) is not used, since it could send
the ServiceAccount token to an endpoint nobody configured for this module (changed in 0.8.16).
The token file is read at every request (projected tokens rotate). It is sent only over `https`;
`ALLOW_INSECURE_TRANSPORT=true` permits `http` and, without a CA certificate file, accepts any server
certificate (development and tests only).

In test mode the module stays inactive unless `KUBERNETES_BYPASS_TEST` is set.

## Usage

When using this module, ensure you are using the correct `ConfigStorage` ID or name if multiple Kubernetes sources are present.

### Resource Handles

Handle format:
- `[namespace/]configmap/name`
- `[namespace/]secret/name`
