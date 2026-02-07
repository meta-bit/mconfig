# 3.5 Handling Secrets

mConfig provides a secure way to handle sensitive information like passwords and keys.

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

## 3.5.2 Scheme Integration
To ensure an entry is treated as a secret, you must mark it with the `SECRET` flag in your `ConfigScheme`.

```json
{
  "database/password": {
    "type": "STRING",
    "flags": ["SECRET"],
    "description": "Database connection password"
  }
}
```

When an entry is marked as a secret:
- It is automatically redacted in logs and `toString()` calls.
- It is stored in a specialized `SecretConfigEntryLeaf` that avoids converting the value to a standard `String` where possible.

## 3.5.3 Binary Secrets and Crypto Libraries
mConfig seamlessly integrates with JCE and other crypto libraries. You can store raw key bytes as `BYTES` entries and retrieve them as `SecretValue`.

```java
// Retrieving a private key
SecretValue privKeySecret = cfg.getSecret("server/privateKey");
byte[] keyBytes = privKeySecret.getValue();

// Reconstruct the key using JCE
KeyFactory kf = KeyFactory.getInstance("RSA");
PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
```
