# Handling Cryptographic Keys and Certificates with mConfig

mConfig supports storing and retrieving cryptographic material (keys, certificates) securely using `SecretValue`. 
Binary data (DER) is stored as `BYTES` entries marked as `SECRET` in schemas. 
PEM can be stored as `STRING` or `BYTES` and parsed with BouncyCastle.

**Note**: Sensitive data is redacted in logs. Use `SecretValue.erase()` after use.

## 1. Prerequisites

- **JCE**: Included in JDK.
- **BouncyCastle** (optional, for PEM/EC/FIPS): Add to classpath:
  Maven:
  ```xml
  <dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78</version>
  </dependency>
  ```
  Gradle:
  ```gradle
  implementation 'org.bouncycastle:bcprov-jdk18on:1.78'
  ```
- Mark schema entries with `"SECRET": true`. The schema file is an array of entry objects:
  ```json
  [
    {
      "KEY": "keys/rsa/private",
      "TYPE": "BYTES",
      "SECRET": true
    },
    {
      "KEY": "certs/server",
      "TYPE": "BYTES",
      "SECRET": true
    }
  ]
  ```
  `"FLAGS": ["SECRET"]` does not mark an entry as secret (see
  [Handling Secrets](../35_handling_secrets.md)). The schema has no field for the kind of secret;
  pass the `SecretType` when storing the value, as shown below.

## 2. Setup ConfigFactory

```java
String schemaJson = ...; // the JSON array shown above
ConfigFactory factory = ConfigFactoryBuilder.create("mycompany", "myapp").build();
factory.addConfigSchema("crypto", schemaJson); // throws ConfigCheckedException
Configuration config = factory.getConfig("crypto");
```

`ConfigSchema.fromJSON(String, ConfigFactoryInstanceContext)` needs the factory-internal context and is
not meant for application code; `addConfigSchema()` parses the JSON in the factory's context.

Instead of specifying the schema programmatically, you can also 
place the schema file in the respective resource directory within the jar/classpath,
as `.config/mycompany/myapp/crypto.mconfig-schema.json`
(separate folders below "main" for production, below "test" for integration tests.)

## 3. Generate and Store RSA Keypair (JCE)

> **Status (0.8.17):** writing a `SecretValue` with `config.put(key, SecretValue, scope)` is **not
> available yet**; `Configuration` has no such method. How secrets are written is still being decided
> (MLIB-152). The lines marked `NOT AVAILABLE YET` below show the intended use and do not compile.
> Reading (`getSecret()`, sections 4 and 6) works today.

```java
import java.security.*;

// Generate
KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
kpg.initialize(2048);
KeyPair kp = kpg.generateKeyPair();

// DER bytes
byte[] privDer = kp.getPrivate().getEncoded();
byte[] pubDer = kp.getPublic().getEncoded();

// Store as SecretValue (auto-typed if schema helps)  -- NOT AVAILABLE YET, see status note above
// config.put("keys/rsa/private", new BasicSecretValue(privDer, SecretType.PRIVATE_KEY), ConfigScope.USER);
config.put("keys/rsa/public", pubDer, ConfigScope.USER); // Public OK as non-secret

// To HOST scope (elevated privs may be needed)  -- NOT AVAILABLE YET
// config.put("keys/rsa/private", new BasicSecretValue(privDer, SecretType.PRIVATE_KEY), ConfigScope.HOST);
```

## 4. Read DER Private Key (JCE)

```java
SecretValue privSecret = config.getSecret("keys/rsa/private");
if (privSecret != null) 
    {
    byte[] derBytes = privSecret.getValue();
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(derBytes);
    PrivateKey privateKey = kf.generatePrivate(keySpec);
    privSecret.erase(); // Secure wipe
    }
```

## 5. Generate and Store Self-Signed X.509 Certificate (BouncyCastle)

```java
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.cert.*; // etc.
// ...
Security.addProvider(new BouncyCastleProvider());

KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
kpg.initialize(2048);
KeyPair kp = kpg.generateKeyPair();

long now = System.currentTimeMillis();
X500Name issuer = new X500Name("CN=TestCA");
BigInteger serial = BigInteger.valueOf(now);
Date notBefore = new Date(now - 5000);
Date notAfter = new Date(now + 365L * 24 * 3600 * 1000);

ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(kp.getPrivate());
X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, issuer, kp.getPublic());
X509CertificateHolder holder = builder.build(signer);
X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

// Store DER  -- NOT AVAILABLE YET, see the status note in section 3
byte[] certDer = cert.getEncoded();
// config.put("certs/server", new BasicSecretValue(certDer, SecretType.CERTIFICATE), ConfigScope.USER);
```

## 6. Read X.509 Certificate (JCE)

```java
SecretValue certSecret = config.getSecret("certs/server");
if (certSecret != null)
    {
    byte[] derBytes = certSecret.getValue();
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(derBytes));
    certSecret.erase();
    }
```

## 7. PEM Handling (BouncyCastle)

### Generate PEM Private Key

```java
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter; // Simpler

try (StringWriter sw = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
    pemWriter.writeObject(kp.getPrivate());
    String pem = sw.toString();
    config.put("keys/rsa/private.pem", pem, ConfigScope.USER); // STRING
}
```

### Read PEM Private Key

```java
SecretValue pemSecret = config.getSecret("keys/rsa/private.pem"); // mark the entry "SECRET": true in the schema
try (PEMParser pemParser = new PEMParser(new CharArrayReader(pemSecret.getChars())))
    {
    Object obj = pemParser.readObject();
    if (obj instanceof PEMKeyPair)
        {
        PEMKeyPair pemPair = (PEMKeyPair) obj;
        JcaPEMKeyConverter conv = new JcaPEMKeyConverter().setProvider("BC");
        PrivateKey privKey = conv.getPrivateKey(pemPair.getPrivateKeyInfo());
        }
    }
finally
    {
    pemSecret.erase();
    }
```

## 8. Writing to Specific Scopes/Dirs

mConfig writes to OS-standard paths:
- **USER**: `~/.config/<company>/<app>/crypto` (JSON/TOML/etc.)
- **HOST**: `/etc/<app>/crypto` (Linux; admin req'd)

Use `config.put(key, value, scope)`.

## Tips
- Use `SecretType.PRIVATE_KEY`, `CERTIFICATE` for typing.
- Tests: integrationtestSecretsBC/JCE verify roundtrip.
- Prod: Rotate keys via CLI `mconfig set --scope USER`.

See [../44_design_consolidated.md](../44_design_consolidated.md) for more.
