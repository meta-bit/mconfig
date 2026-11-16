# mConfig Snippets: Code Templates to Copy

Audience: developers and AI coding assistants who need working mConfig code **now**.

This file holds the templates. Two companions hold the rest:

- [Snippets reference](../documentation/src/site/markdown/snippets.md): exact API names, schema
  fields, feature settings, scopes. Look things up there.
- [Better practice: replace this with that](../documentation/src/site/markdown/38_code_improvements.md):
  what to replace in existing code, with and without mConfig.

**The Java templates are compiled.** Each Java block marked "source:" is a copy of a region in a
source file under [`snippets/`](snippets), and `ExampleSourcesCompileTest` compiles those files
against the current API and compares them with this page on every build. Blocks without that mark
(Spring, JPMS, Kubernetes, POJO mapping, build files) need modules or tools the check does not
have; they were compiled by hand against 0.8.17.

Related: [starters/README.md](starters/README.md) (complete projects) ·
[AGENTS.md](../AGENTS.md) · [mConfigTools/README.md](../mConfigTools/README.md)

| # | Template |
|---|:--|
| 1 | [Dependencies and single-JAR packaging](#1-dependencies) |
| 2 | [Bootstrap](#2-bootstrap-the-canonical-five-lines) |
| 3 | [Defaults: schema and resource file](#3-defaults-schema-and-resource-file) |
| 4 | [Environment variables](#4-environment-variables-12-factor) |
| 5 | [Containers and Kubernetes](#5-containers-and-kubernetes) |
| 6 | [Hot reload](#6-hot-reload-without-a-restart) |
| 7 | [Secrets](#7-secrets-without-log-leaks) |
| 8 | [Writing configuration back](#8-writing-configuration-back) |
| 9 | [Tests](#9-tests-isolation-that-leaves-production-code-untouched) |
| 10 | [Diagnostics](#10-diagnostics-where-did-that-value-come-from) |
| 11 | [Spring Boot wiring](#11-spring-boot-wiring) |
| 12 | [JPMS](#12-jpms-module-infojava) |
| 13 | [POJO mapping](#13-pojo-mapping-mconfigmapper) |
| 14 | [Adapters for legacy code](#14-adapters-for-legacy-and-layered-code-mconfigutil) |

---

## 1. Dependencies

<!-- AI-Agent: resolve the real version from the user's pom.xml / build.gradle / ~/.m2 first.
     If nothing can be resolved, say so and ask; do not invent a version or copy the one below.
     Never emit an empty or literal-placeholder version. -->

Pick exactly one bundle:

| Bundle                  | Use when                                                                   |
|-------------------------|-----------------------------------------------------------------------------|
| `mconfigbasic` (pom)    | Lightweight, file-based Java. Core + schema + secrets + filesystem + JAR + `.properties`. |
| `mconfigstandard` (pom) | Cloud-native. Adds env vars, JSON/YAML/TOML via Jackson, INI, Slf4j logging, mapper, Windows registry. |

### Maven

```xml
<properties>
    <mconfig.version>0.8.17</mconfig.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.metabit.platform.support.config</groupId>
        <artifactId>mconfigstandard</artifactId>
        <version>${mconfig.version}</version>
        <type>pom</type>
    </dependency>
</dependencies>
```

### Gradle (Kotlin DSL)

```kotlin
val mconfigVersion = "0.8.17"

dependencies {
    implementation("org.metabit.platform.support.config:mconfigstandard:$mconfigVersion")
}
```

### Optional modules (add on top of a bundle)

| Artifact                              | Adds                                              |
|---------------------------------------|---------------------------------------------------|
| `mconfigsourceenvvar`                 | Environment variables → `SESSION` scope           |
| `mconfigsourcekubernetes`             | ConfigMaps / Secrets, zero extra dependencies     |
| `mconfigsourcevault`                  | HashiCorp Vault KV v1/v2                          |
| `mconfigsourceawssecretsmanager`      | AWS Secrets Manager                               |
| `mconfigmapper`                       | POJO ↔ configuration mapping                      |
| `mconfigwinregistry`                  | Windows registry / GPO `POLICY` scope             |
| `mconfigloggingslf4j`                 | Route library logging into Slf4j                  |
| `mconfigtools`                        | The `mconfig` CLI                                 |

### Single ("fat") JAR: merge `META-INF/services`

mConfig modules, like SLF4J 2, Log4j 2, JDBC drivers and Jackson modules, register themselves in
`META-INF/services/` files of the same name. Packing everything into one JAR must merge these files;
otherwise `build()` fails with `JAVA_SERVICE_CONFIGURATION_ERROR` ("faulty JAR packaging"), or sources
go missing without notice. Do **not** generate `<descriptorRef>jar-with-dependencies</descriptorRef>`.

Maven assembly: own descriptor `src/assembly/jar-with-dependencies-merged-services.xml`, referenced via
`<descriptors><descriptor>…</descriptor></descriptors>`:
```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0">
    <id>jar-with-dependencies-merged-services</id>
    <formats><format>jar</format></formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <containerDescriptorHandlers>
        <containerDescriptorHandler><handlerName>metaInf-services</handlerName></containerDescriptorHandler>
    </containerDescriptorHandlers>
    <dependencySets>
        <dependencySet>
            <outputDirectory>/</outputDirectory>
            <useProjectArtifact>true</useProjectArtifact>
            <unpack>true</unpack>
            <scope>runtime</scope>
        </dependencySet>
    </dependencySets>
</assembly>
```
Maven shade: `<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>`.
Gradle Shadow: `shadowJar { mergeServiceFiles() }`. Details:
[Packaging as a single JAR](../documentation/src/site/markdown/13_getting_started.md#138-packaging-as-a-single-jar).

---

## 2. Bootstrap: the canonical five lines

Source: [`snippets/BootstrapSnippets.java`](snippets/BootstrapSnippets.java)

<!-- snippet: BootstrapSnippets.java#bootstrap-imports -->
```java
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.Configuration;
```

<!-- snippet: BootstrapSnippets.java#bootstrap -->
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp").build())
    {
    Configuration net = factory.getConfig("network");

    String  host = net.getString("server/host");
    Integer port = net.getInteger("server/port");
    Boolean tls  = net.getBoolean("server/tls");

    startServer(host, port, tls);
    }
```

Build the `ConfigFactory` **once per application** and share it. It owns the file watchers,
caches and background threads; `close()` releases them.

With an optional sub-path level below the application (e.g. per-tenant or per-version):

<!-- snippet: BootstrapSnippets.java#bootstrap-subpath -->
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp", "eu-west/v2").build();
```

Throwaway single-config access (skips shared caching, not for hot paths; needs `mconfigutil`):

<!-- snippet: BootstrapSnippets.java#bootstrap-quickconfig -->
```java
Configuration net = ConfigUtil.quickConfig("ACME", "demoApp", "network");
```

---

## 3. Defaults: schema and resource file

**This is the mConfig way. Defaults belong here, never at the call site.**
The fields and their rules are explained in the
[reference](../documentation/src/site/markdown/snippets.md#2-schema-fields-and-gotchas).

`src/main/resources/.config/ACME/demoApp/network.mconfig-schema.json`

```json
[
  {
    "KEY": "server/host",
    "TYPE": "STRING",
    "DESCRIPTION": "Hostname or IP address to bind to.",
    "DEFAULT": "127.0.0.1",
    "FLAGS": ["MANDATORY"]
  },
  {
    "KEY": "server/port",
    "TYPE": "NUMBER",
    "DESCRIPTION": "TCP port to listen on.",
    "DEFAULT": 8080,
    "PATTERN": "uint16",
    "FLAGS": ["MANDATORY"]
  },
  {
    "KEY": "server/tls",
    "TYPE": "BOOLEAN",
    "DESCRIPTION": "Enable TLS.",
    "DEFAULT": false
  },
  {
    "KEY": "server/password",
    "TYPE": "STRING",
    "DESCRIPTION": "Credential for the upstream connection.",
    "SECRET": true,
    "WRITE_SCOPE": "USER"
  }
]
```

Lock the application to its bundled contract (ignore filesystem schema overrides). `ALLOW_LOCAL_SCHEMA_OVERRIDE`
is deprecated since 0.8.17 and replaced in 0.9.0 by an allowlist of schema sources; it keeps working until then:

<!-- snippet: BootstrapSnippets.java#schema-lock -->
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        .setFeature(ConfigFeature.ALLOW_LOCAL_SCHEMA_OVERRIDE, false)
        .setFeature(ConfigFeature.SCHEMA_STRICT_MODE, true)   // reject keys absent from the schema
        .build();
```

Passing a schema explicitly (mainly for tests and generated schemas):

<!-- snippet: BootstrapSnippets.java#schema-explicit -->
```java
Configuration cfg = factory.getConfig("network", schema);   // throws ConfigCheckedException
```

### Fallback defaults as a resource file

Optional, and complementary to the schema: shipped inside the JAR, resolved at `PRODUCT` scope.

`src/main/resources/.config/ACME/demoApp/network.properties`

```properties
server.host=127.0.0.1
server.port=8080
server.tls=false
```

---

## 4. Environment variables (12-factor)

Add `mconfigsourceenvvar` (already in `mconfigstandard`). No code change is needed;
env vars land in the `SESSION` scope and therefore override `USER`, `APPLICATION`, `HOST`, …

The variable name is derived as `<APPLICATION>_<CONFIGNAME>_<KEY>`, **uppercased**,
with `/` mapped to `_` and any literal `_` in the key doubled to `__`
(so the key `max_connections` is `DEMOAPP_NETWORK_MAX__CONNECTIONS`). This is the reverse of the
.NET and Airflow custom; see the
[module README](../mConfigSourceEnvVar/README.md#how-this-compares-to-other-conventions).

```bash
# factory: ConfigFactoryBuilder.create("ACME", "demoApp")
# config : "network",  key: "server/host"
export DEMOAPP_NETWORK_SERVER_HOST=0.0.0.0
export DEMOAPP_NETWORK_SERVER_PORT=9090
```

Source: [`snippets/RuntimeSnippets.java`](snippets/RuntimeSnippets.java)

<!-- snippet: RuntimeSnippets.java#env-read -->
```java
Configuration net = factory.getConfig("network");
net.getString("server/host");   // "0.0.0.0" - from SESSION, overriding the file
```

Environment variables are **read-only**. `put(..., ConfigScope.SESSION)` against this source
is refused by design; `put()` reports that as the unchecked `ConfigException`.

To also allow env-var control of the *library itself* (`MCONFIG_RUNTIME_DEBUG_LEVEL`,
`MCONFIG_RUNTIME_TEST_MODE`, …), off by default since it widens the attack surface:

<!-- snippet: BootstrapSnippets.java#env-runtime-settings -->
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        .setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true)
        .build();
```

---

## 5. Containers and Kubernetes

### 5.1 Generic mounted-volume override (works with any orchestrator)

`ADDITIONAL_*_DIRECTORIES` entries take the form `[SCOPENAME ":"] PATH`.
They are *prepended* to the search list of their scope; unavailable paths are ignored,
so the same build runs unchanged on a laptop.

<!-- snippet: RuntimeSnippets.java#container-directories -->
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES,
                    List.of("HOST:/etc/demoapp", "USER:/var/run/demoapp/config"))
        .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 2000)
        .build();
```

### 5.2 Kubernetes ConfigMap + Secret

Add `mconfigsourcekubernetes`. It auto-discovers `/etc/config` and `/etc/secrets`,
authenticates in-cluster with the ServiceAccount token, reloads on rotation, and
disables its API client automatically under `TEST_MODE`. Changes to a mounted ConfigMap are
picked up like any other change; see [Hot reload](#6-hot-reload-without-a-restart).

The manifest is also available as a file: [`kubernetes/configmap-secret.yaml`](kubernetes/configmap-secret.yaml).

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: demo-app-config
  namespace: demo
data:
  network.properties: |
    server.port=8080
    server.host=0.0.0.0
---
apiVersion: v1
kind: Secret
metadata:
  name: demo-app-secrets
  namespace: demo
type: Opaque
data:
  db.password: c3VwZXItc2VjcmV0LXBhc3N3b3Jk
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
  namespace: demo
spec:
  template:
    spec:
      containers:
        - name: demo-app
          image: acme/demo-app:1.0.0
          volumeMounts:
            - name: app-config
              mountPath: /etc/config
              readOnly: true
            - name: app-secrets
              mountPath: /etc/secrets
              readOnly: true
      volumes:
        - name: app-config
          configMap:
            name: demo-app-config
        - name: app-secrets
          secret:
            secretName: demo-app-secrets
```

Not compiled by the check (needs `mconfigsourcekubernetes`):

```java
import org.metabit.platform.support.config.impl.source.kubernetes.KubernetesFeatures;

ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        // only needed to point at non-standard mount points:
        .setFeature(KubernetesFeatures.CONFIG_DIR_PATHS,  "/etc/config")
        .setFeature(KubernetesFeatures.SECRETS_DIR_PATHS, "/etc/secrets")
        .build();

Configuration net = factory.getConfig("network");
int port = net.getInteger("server.port");            // flat .properties key → literal dots
```

---

## 6. Hot reload without a restart

mConfig polls cached sources every `UPDATE_CHECK_FREQUENCY_MS` (default 2000 ms; `0` disables).
Reads simply return the fresh value; you only need a listener if you must *react*.

<!-- snippet: RuntimeSnippets.java#reload-subscribe -->
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 1000)
        .build())
    {
    Configuration net = factory.getConfig("network");

    java.util.function.Consumer<ConfigLocation> onChange = location ->
        {
        log.info("configuration changed at {}", location.toLocationString());
        connectionPool.resize(net.getInteger("pool/size"));
        };

    net.subscribeToUpdates(onChange);
    // ... later, on component shutdown:
    net.unsubscribeFromUpdates(onChange);
    }
```

Per-key subscription: precise, but it adds per-entry bookkeeping, so use it sparingly.

<!-- snippet: RuntimeSnippets.java#reload-subscribe-key -->
```java
net.subscribeToUpdates("pool/size", location -> connectionPool.resize(net.getInteger("pool/size")));
```

Notes:

- Listeners run on the watcher thread. Keep them short; hand real work to an executor.
- `ConfigFeature.UPDATE_CHECK_SCOPES` limits the scopes that are checked for changes. Its current
  default lists all scopes, `RUNTIME` included; drop `RUNTIME` from the list if changes your own code
  makes should not notify listeners.
- Force an immediate re-read yourself with `net.reload()` (throws `ConfigCheckedException`).

---

## 7. Secrets without log leaks

Mark the entry as a secret **in the schema** (`"SECRET": true`, see [section 3](#3-defaults-schema-and-resource-file)), then:

<!-- snippet: RuntimeSnippets.java#secret-chars -->
```java
SecretValue password = cfg.getSecret("db/password");
if (password != null)
    {
    char[] chars = password.getChars();
    try
        {
        dataSource.connect(user, chars);
        }
    finally
        {
        java.util.Arrays.fill(chars, '\0');
        password.erase();          // zero the library-side buffer
        }
    }
```

Binary secrets (keys, certificates) round-trip through `BYTES`:

<!-- snippet: RuntimeSnippets.java#secret-bytes -->
```java
SecretValue keySecret = cfg.getSecret("server/privateKey");
byte[]      keyBytes  = keySecret.getValue();
try
    {
    PrivateKey key = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    useKey(key);
    }
finally
    {
    java.util.Arrays.fill(keyBytes, (byte) 0);
    keySecret.erase();
    }
```

`SecretValue` is `org.metabit.platform.support.config.interfaces.SecretValue`.

What mConfig guarantees, and what it does not:

- Guaranteed: `toString()` and log output render `[REDACTED]`; the value avoids `String`
  interning where possible; buffers are wiped on `erase()` and on configuration close.
- **Not** guaranteed: the security of the *underlying store*. A secret in a plain properties
  file or an environment variable is exactly as exposed as that store is. Use Vault,
  AWS Secrets Manager, or Kubernetes Secrets for production credentials.
- mConfig prevents accidental disclosure. It does not protect a secret in process memory
  (heap dumps, debuggers, code in the same process).

---

## 8. Writing configuration back

Writes need an explicit scope: the layered read hierarchy has no single "here" to write to.

<!-- snippet: RuntimeSnippets.java#write-scope -->
```java
Configuration cfg = factory.getConfig("network");

cfg.put("server/port", 9090, ConfigScope.USER);
cfg.flush();                                        // throws ConfigCheckedException
```

Fall-forward across several acceptable scopes (most general first):

<!-- snippet: RuntimeSnippets.java#write-scopes -->
```java
cfg.put("server/port", 9090, EnumSet.of(ConfigScope.HOST, ConfigScope.USER));
```

Let the schema decide where a key belongs (`WRITE_SCOPE` in the schema entry):

<!-- snippet: RuntimeSnippets.java#write-schema-scope -->
```java
cfg.put("server/password", secretText);             // no scope argument
```

Creating files and directories that do not exist yet:

<!-- snippet: RuntimeSnippets.java#write-create -->
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        .setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true)  // create the config file
        .setFeature(ConfigFeature.CREATE_MISSING_PATHS, true)       // create parent directories
        .setFeature(ConfigFeature.WRITE_SYNC, true)                 // write through immediately
        .build();
```

Find a writable directory for a scope before writing your own files next to the config (needs `mconfigutil`):

<!-- snippet: RuntimeSnippets.java#write-where -->
```java
Path dir = ConfigUtil.whereToPutMyFiles(factory, ConfigScope.USER, true);   // throws ConfigCheckedException
```

---

## 9. Tests: isolation that leaves production code untouched

`setTestMode()` and `setTestConfigPaths()` return `void`; do **not** chain them.

Source: [`snippets/NetworkConfigTest.java`](snippets/NetworkConfigTest.java)

<!-- snippet: NetworkConfigTest.java#test-mode -->
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkConfigTest
{
    @TempDir Path tempDir;

    @Test
    void readsPortFromUserScope() throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "demoApp");
        builder.setTestMode(true);
        builder.setTestConfigPaths(ConfigScope.USER, List.of(tempDir.toAbsolutePath().toString()));
        builder.setFeature(ConfigFeature.CREATE_MISSING_PATHS, true);
        builder.setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true);

        try (ConfigFactory factory = builder.build())
            {
            Configuration cfg = factory.getConfig("network");
            cfg.put("server/port", 12345, ConfigScope.USER);
            cfg.flush();

            assertEquals(12345, cfg.getInteger("server/port"));
            }
        // @TempDir cleans up; no real user config was touched.
        }
}
```

Alternative to `setTestConfigPaths`, same effect, expressed as a feature
(format is `SCOPENAME:PATH`):

<!-- snippet: RuntimeSnippets.java#test-directories -->
```java
builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES,
                   List.of("USER:" + tempDir, "HOST:" + otherDir));
```

Without explicit paths, test mode falls back to `src/test/resources/config/<SCOPE>/`
and `src/test/config/<company>/<app>/`. **Schemas are never bypassed in test mode**;
the contract stays in force, which is the point.

Hardening for production builds: make test mode impossible to switch on.

<!-- snippet: RuntimeSnippets.java#test-forbid -->
```java
ConfigFactoryBuilder.forbidTestMode();   // static, JVM-wide; call early in main()
```

---

## 10. Diagnostics: "where did that value come from?"

Source: [`snippets/DiagnosticsSnippets.java`](snippets/DiagnosticsSnippets.java)

<!-- snippet: DiagnosticsSnippets.java#diagnostics -->
```java
// 1. every location searched, in resolution order
for (ConfigLocation loc : factory.getSearchList())
    {
    System.out.printf("%-12s %-10s %s%n",
            loc.getScope(), loc.getStorage().getStorageID(), loc.toLocationString());
    }

// 2. every configuration actually discovered
for (ConfigDiscoveryInfo info : factory.listAvailableConfigurations())
    {
    System.out.printf("%s @ %s (%s, scope=%s, writeable=%b)%n",
            info.getConfigName(), info.getUri(), info.getFormatID(),
            info.getScope(), info.isWriteable());
    }

// 3. which layers back this one configuration
cfg.getSourceLocations().forEach(System.out::println);

// 4. non-fatal problems the library recorded (missing files, parse issues, permissions)
for (ConfigEvent event : factory.getEvents())
    {
    System.out.printf("[%s] %s %s: %s%n",
            event.getSeverity(), event.getDomain(), event.getDetailCode(), event.getMessage());
    }

// 5. every key visible in the given scopes
Set<String> keys = cfg.getAllConfigurationKeysFlattened(EnumSet.allOf(ConfigScope.class));

// 6. where one value comes from
ConfigEntry entry = cfg.getConfigEntryFromFullKey("server/port", EnumSet.allOf(ConfigScope.class));
if (entry != null)
    {
    System.out.printf("server/port: scope=%s, location=%s%n", entry.getScope(), entry.getLocation());
    }
```

Raise the event detail level while debugging:

<!-- snippet: DiagnosticsSnippets.java#diagnostics-events -->
```java
ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
        .setFeature(ConfigFeature.EVENTS_DETAIL_LEVEL, "VERBOSE")   // OFF | FAILURES_ONLY | NORMAL | VERBOSE
        .build();
```

Or use the CLI without touching the application at all:

```bash
mconfig ACME:demoApp:network get server/port -v     # effective value + scope + source URI
mconfig ACME:demoApp:network show -f JSON | jq .   # effective configuration as JSON
mconfig ACME:demoApp:network monitor --dump -v      # live-tail changes
```

---

## 11. Spring Boot wiring

mConfig needs no DI framework, but it drops in cleanly as beans. A complete project is in
[`spring/mConfigDemo`](spring/mConfigDemo). Not compiled by the check (needs Spring).

```java
package com.example.demo;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MConfigSpringConfig
{
    @Bean(destroyMethod = "close")
    public ConfigFactory mconfigFactory()
        {
        return ConfigFactoryBuilder.create("ACME", "demoApp").build();
        }

    @Bean
    public org.metabit.platform.support.config.Configuration networkConfiguration(ConfigFactory factory)
        {
        return factory.getConfig("network");   // live handle; hot-reload still applies
        }
}
```

Inject the `Configuration` and read on demand; do **not** copy values into
`@Value`-style fields at startup, or you throw away hot reload.

```java
@Service
public class ConnectorService
{
    private final org.metabit.platform.support.config.Configuration cfg;

    public ConnectorService(org.metabit.platform.support.config.Configuration cfg) { this.cfg = cfg; }

    public String endpoint() { return cfg.getString("server/host") + ":" + cfg.getInteger("server/port"); }
}
```

Coming from Spring and looking for `${...}` placeholders or profiles? See the
[FAQ](../documentation/src/site/markdown/43_faq.md).

---

## 12. JPMS (`module-info.java`)

A complete project is in [`starters/jpms-maven`](starters/jpms-maven).

```java
module com.example.demo {
    requires metabit.mconfig.core;
    requires metabit.mconfig.util;   // only if you use ConfigUtil
    exports com.example.demo;
}
```

Storage, format and logging modules are discovered through `ServiceLoader`. They must be on the
module path but need **no** `requires` clause: `metabit.mconfig.core` already declares the
`uses` directives. If a format silently fails to load, the module is missing from the module
path, not from `module-info.java`.

---

## 13. POJO mapping (`mconfigmapper`)

Not compiled by the check (needs `mconfigmapper`).

```java
import org.metabit.platform.support.config.ConfigCursor;
import org.metabit.platform.support.config.mapper.ConfigMapper;

ConfigMapper  mapper = ConfigMapper.create();
ConfigCursor  cursor = cfg.getConfigCursor();

ServerSettings settings = mapper.readObject(cursor, ServerSettings.class);   // setters
mapper.writeObject(settings, cursor, EnumSet.of(ConfigScope.USER));          // getters/is-ers
```

Export an effective configuration to a file (format inferred from the extension).
Secret entries are left out of the export.

```java
mapper.saveTo(cfg, Path.of("/tmp/effective-network.yaml"));
mapper.saveTo(cfg, Path.of("/tmp/host-only.json"), EnumSet.of(ConfigScope.HOST));
```

---

## 14. Adapters for legacy and layered code (`mconfigutil`)

The adapters are read-only views that help with transition; they are not a second facade.
What they do and do not do:
[Utilities and Adapters](../documentation/src/site/markdown/39_utilities_and_adapters.md).

<!-- snippet: RuntimeSnippets.java#adapters -->
```java
// java.util.Properties -> Configuration (read-only view)
Configuration fromProps = ConfigUtil.fromProperties(legacyProperties);

// Configuration -> java.util.Properties, prefix stripped from the keys
Properties dbProps = ConfigUtil.copyPrefixedEntriesToJavaProperties(cfg, "db.");

// key remapping: view.getString("db.host") reads source.getString("super_db.host")
Configuration view = ConfigUtil.remapped(cfg, "super_db.", "db.");

// ad-hoc overlay (read-only). Prefer put(..., ConfigScope.RUNTIME) when you need to change values.
Configuration overridden = ConfigUtil.withOverrides(cfg, Map.of("server/port", 9999));
```
