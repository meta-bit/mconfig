# Snippets: Quick Solutions to Common Config Problems

mConfig shines for everyday config pains. Copy-paste these snippets—no setup hassles.

Each solves a **general problem** using mConfig's magic (OS paths, layers, reloads, schemes).

**[Starters](../../../examples/starters) for full projects • [FAQ](4_FAQ.md) for fixes • [CLI](../../../mConfigTools/README.md)**

## 1. Replace Hardcoded Strings/Ports (Zero Magic Strings)

**Problem:** Ports, hosts scattered in code—hard to change/deploy.

```java
Config cfg = ConfigUtil.quickConfig("myco", "myapp", "network");
String peer = cfg.getString("peer");  // localhost
int port = cfg.getInteger("port");    // 8080
double prob = cfg.getDouble("probability");  // 0.5
```

**Place:** `~/.config/myco/myapp/network.properties` (auto-discovered).

**Why mConfig?** Searches OS standards (XDG, AppData), formats (properties/YAML/JSON), reloads live.

[Getting Started](getting-started.md)

## 2. Env Vars → Structured Config (No Manual Parsing)

**Problem:** `process.env` mess; want hierarchical config.

*Add dep:* `mConfigSourceEnvVar`

Env vars:
```
myapp_network_peer=localhost
myapp_network_port=8080
```

```java
try (ConfigFactory f = ConfigFactoryBuilder.create("myco", "myapp").build()) {
    String peer = f.getConfig("network").getString("peer");  // SESSION override
}
```

**Why?** `<app>_<config>_<key>` convention; layers over files.

[mConfigSourceEnvVar](../../../mConfigSourceEnvVar/README.md)

## 3. Type-Safe Config with Defaults/Validation

**Problem:** Runtime `NumberFormatException`; scattered defaults.

`src/main/resources/.config/myco/myapp/network.scheme.json`:
```json
[{"name":"port","type":"int","default":8080,"min":1,"max":65535}]
```

```java
ConfigScheme scheme = ConfigScheme.fromClasspath("network.scheme.json");
Config cfg = factory.getConfig("network", scheme);
int port = cfg.getInteger("port");  // Validates!
```

**Why?** Centralized scheme—no call-site defaults.

[Configuration Schemes](configuration-schemes.md)

## 4. Hot-Reload on File Changes

**Problem:** Restart for config tweaks? No thanks.

```java
Config cfg = factory.getConfig("network");  // Watches files
// Edit network.properties → cfg.getInteger("port") updates instantly
```

**Why?** Built-in watcher; no polling/Spring @Refresh.

[Priorities](3_2_priorities_and_hierarchies.md)

## 5. Discover Available Configs (No Guesswork)

**Problem:** "Where's my config?"

```java
factory.listAvailableConfigurations().forEach(cdi ->
    System.out.println(cdi.getConfigName() + " @ " + cdi.getUri())
);
```

**Why?** Traces sources/scopes; CLI equiv: `mconfig myco:myapp list`

## 6. Secrets Without Log Leaks

**Problem:** Passwords in logs/heap.

In scheme: `"type":"secret"`

```java
SecretValue pw = (SecretValue) cfg.getEntry("db.password");  // Masked: ********
String clear = pw.clearValue();  // JIT decrypt/use
```

[mConfigSecrets](../../../mConfigSecrets/README.md)

## 7. JPMS-Compatible (No IllegalAccessError)

`module-info.java`:
```java
module com.example {
    requires metabit.mconfig.core;
    requires metabit.mconfig.util;
}
```

*Deps:* `mConfigStandard` (BOM) or `mConfigBase`.

[JPMS FAQ](4_FAQ.md#jpms-module-access-errors)

## 8. Tests: Isolated Configs

```java
ConfigFactoryBuilder.setTestMode(true);
ConfigFactoryBuilder.setTestConfigPaths(ConfigScope.USER, List.of("src/test/resources"));
Config cfg = ConfigFactoryBuilder.create("myco", "myapp").build().getConfig("network");
```

`src/test/resources/.config/myco/myapp/network.properties`

[Test Mode](test-mode.md)

**Pro Tip:** Enable logging (`mConfigLoggingSlf4j` + DEBUG) for traces.

[Full Docs](index.md)