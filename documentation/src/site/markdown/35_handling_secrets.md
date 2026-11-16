# 3.5 Handling Secrets

mConfig provides a secure way to handle sensitive information like passwords and keys.

## Background
For the security philosophy and rationale behind secrets handling, see [Secrets Concept](26_secrets_concept.md).

## 3.5.1 Retrieving Secrets
The `getSecret(key)` method returns a `SecretValue` object, which provides safe access to the sensitive data.

```java
SecretValue dbPassword = cfg.getSecret("database/password");
if (dbPassword != null)
    {
    char[] passwordChars = dbPassword.getChars();
    // Use passwordChars ...
    // Note: SecretValue.erase() can be used to clear internal buffers
    dbPassword.erase();
    }
```

## 3.5.2 Schema Integration
To ensure an entry is treated as a secret, set `"SECRET": true` on its entry in your `ConfigSchema`.

```json
[
  {
    "KEY": "database/password",
    "TYPE": "STRING",
    "DESCRIPTION": "Database connection password",
    "SECRET": true
  }
]
```

A schema file is a JSON **array of entry objects**, each carrying its own `KEY`. An object keyed
by configuration key — `{"database/password": {...}}` — parses to an *empty* schema without
raising an error, so the entry would silently not be protected.

> **Warning:** `"FLAGS": ["SECRET"]` does **not** mark an entry as secret. The flag is stored but
> never consulted; all secret handling reads the separate `SECRET` property. Use `"SECRET": true`.
> Since 0.8.15, loading a schema that uses the flag without `"SECRET": true` logs a WARNING naming
> the entry. See [Which markers are enforced today](23_configuration_schemas.md#which-markers-are-enforced-today).

Note the difference between a secret and merely sensitive data: `SECRET` is for cryptographic
secrets and passwords, while `SENSITIVE` marks information that must not leak into view, such as
personal data under GDPR. Most secrets are also sensitive, but not the other way round. See
[Secret and Sensitive](26_secrets_concept.md#secret-and-sensitive). `SENSITIVE` is defined but
not yet enforced, so do not rely on it for compliance today.

When an entry is marked as a secret:
- It is automatically redacted in logs and `toString()` calls.
- `getSecret()` returns it as a `SecretValue`, a byte buffer you can wipe with `erase()`, instead of a `String`.
- **Wiping**: when the configuration is closed, the in-memory, environment-variable, Vault, AWS
  and Kubernetes layers zero the buffers holding the secret. Layers read from files do not yet
  (planned for 0.9.1). See [Memory wiping](26_secrets_concept.md#memory-wiping).

mConfig prevents accidental disclosure; it does not protect secrets from anyone who can read the
memory of the process. See
[What mConfig protects against, and what not](26_secrets_concept.md#what-mconfig-protects-against-and-what-not).

## 3.5.3 Storage Considerations

mConfig limits how secrets are exposed inside the application; it does not make the underlying storage secure.

- **Secure Stores**: Prefer HashiCorp Vault, AWS Secrets Manager, or Kubernetes Secrets for production credentials. These provide encryption-at-rest and access auditing.
- **Non-Secure Stores**: If you store a secret in the **Windows Registry**, **Environment Variables**, or **Plain Text Files**, mConfig will still treat it as a secret *inside the application* (redacting it, allowing erasure), but the data remains as secure as the storage itself (e.g., Registry ACLs or OS user permissions).

Secrets stored in plain-text files are a legitimate setup: their protection is who may read the
file. When mConfig writes to such a file (`put()`, `mconfig set`), the secrets in it stay intact.

## 3.5.4 Secrets in Exports and Tool Output

Secret values never leave their storage except into the application itself. The rules and their
rationale are in [Where secret values may appear](26_secrets_concept.md#where-secret-values-may-appear);
in practice:

- **`ConfigMapper.saveTo()`** writes every entry except secrets. The omitted keys are reported as a
  WARNING event with detail code `EXPORT_SECRETS_OMITTED`:

  ```java
  mapper.saveTo(config, Paths.get("export.json"));
  config.getEvents().stream()
        .filter(e -> "EXPORT_SECRETS_OMITTED".equals(e.getDetailCode()))
        .forEach(e -> System.err.println(e.getMessage()));   // names the keys, never the values
  ```

- **The `mconfig` CLI** shows `[REDACTED]` instead of a secret value in every command and output
  format; key, scope and source stay visible. `mconfig get` also reports `Secret: yes`
  (`"secret": true` in JSON, YAML and TOML).
- **Reveal a single value** on explicit request:

  ```bash
  mconfig mycompany:myapp:database get password --reveal
  ```

- **`ConfigUtil.copyPrefixedEntriesToJavaProperties()`** and
  **`copySchemeDefinedEntriesToJavaProperties()`** copy secrets in plain text by design, for
  in-process hand-over to legacy interfaces such as database drivers. Do not log, store or export
  the resulting `Properties`.

## 3.5.5 Binary Secrets and Crypto Libraries
mConfig seamlessly integrates with JCE and other crypto libraries. You can store raw key bytes as `BYTES` entries and retrieve them as `SecretValue`.

```java
// Retrieving a private key
SecretValue privKeySecret = cfg.getSecret("server/privateKey");
byte[] keyBytes = privKeySecret.getValue();

// Reconstruct the key using JCE
KeyFactory kf = KeyFactory.getInstance("RSA");
PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
```

## 3.5.6 Where secrets come from

A secret is either marked in the schema (`"SECRET": true`) for an entry from any storage, or
delivered by a source that knows it is secret. For the second case there are two routes.

**1. Secret-aware storage modules.** This is the route in use today. Vault
(`mconfigsourcevault`), AWS Secrets Manager (`mconfigsourceawssecretsmanager`) and Kubernetes
(`mconfigsourcekubernetes`, `mconfigsourcekubernetesfabric8`) are storages like the filesystem one:
add the module, configure it in its bootstrap configuration (`vault.properties`, `aws.properties`,
…; see each module's README), and its entries take part in the scope hierarchy. Entries from
Vault, AWS Secrets Manager and Kubernetes Secrets arrive marked secret; Kubernetes ConfigMaps do
not.

- They are **opt-in**: neither `mconfigbasic` nor `mconfigstandard` contains them. Add the
  dependency explicitly.
- Vault and AWS Secrets Manager sources default to the `CLOUD` scope. Their source builders take
  another scope (`VaultConfigSource.builder().withScope(...)`, then `addSource(...)`).
- **POLICY**: a source the application registers at `POLICY` scope fails in productive mode unless
  `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code. See
  [POLICY locations registered by the application](25_config_features.md#policy-locations-registered-by-the-application).

**2. Secrets providers.** Core defines the extension point `ConfigSecretsProviderInterface`;
providers are found with `ServiceLoader` and activated with
`ConfigFactoryBuilder.addSecretsProvider(id, config, scope)`. `mconfigsecrets` (part of
`mconfigbasic`) places each active provider in the scope hierarchy at the requested scope. It is
meant for backends that hand out individual secrets on request, such as a KMS or an HSM.
**mConfig ships no provider**; the interface is for your own or third-party implementations. A provider should return a new `SecretValue` per request, so that `erase()` by the
caller does not wipe its stored value.
