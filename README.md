# mConfig

The "Hen-and-Egg" Problem Solved.

To run, your program needs data. But how does it know where that data lives?
mConfig bridges the gap. It performs lookups exactly where Operating Systems 
and users expect them to be, providing a unified, type-safe API that spans 
from embedded I2C storage to cloud-native orchestration.

## Features (Why mConfig?)

* Universal Reach: A single library for the entire compute spectrum;
  from resource-constrained embedded systems to cloud-native microservices.
* Minimal Footprint: The core library is less than 200kB (JAR size). 
  This is exceptionally small compared to frameworks like Spring Boot or Apache Commons, making it ideal for microservices, serverless functions, and environments where memory/disk space is critical.
* Zero-Dependency Core: Keep your footprint small. 
  The core is lightweight, modular, and dependency-free.
* Deep Origin Traceability: 
  No more "ghost" settings. Every configuration value can be traced back to its
  specific source and layer.
* Enterprise-Ready Policy Enforcement: 
  Includes a native POLICY scope to respect Windows Group Policy (GPO) 
  and mandatory administrative overrides.
* the mConfig Windows module is Registry-Aware, automatically mapping Windows Group Policy (GPO) to the high-priority POLICY scope.
* Self-Healing & Late-Binding: 
  Built-in file watcher detects changes and handles "late-bound" configurations
  (e.g., Kubernetes ConfigMaps mounted after startup).
* OS-Native Intelligence:
  Respects XDG Base Directory standards on Linux and 
  Registry/Known Folder conventions on Windows.
* Modular Drop-in Support:
  mConfig implements Modular Directory Discovery, automatically layering .d fragments into the configuration stack without manual setup.
* Multi-Format Support: comes with format modules for TOML, YAML, JSON5, JSON, and Java Properties.¹
* Type-Safe Config Schemes: Define your schema once. Includes validation, range-checking, and centralized defaults.
* Sensible Defaults, Total Control: 
  Adheres to the "principle of least surprise" with its defaults.
  Every aspect is tunable via ConfigFeature flags for enterprise-level customization.
* Dynamic Updates: do not buffer values locally. Changes are immediately reflected in mConfig objects,
  so you always have the latest configuration available. ("zero-code data binding")

¹ parsing of TOML,YAML,JSON5,JSON is done using mature external libraries,
so using these formats will increase your application's footprint. 

## usage

Choose the entry point that fits your project architecture:

### 1. Standard Core (`mConfigStandard`)

Best for: Lightweight Java projects requiring standard filesystem support.

Maven:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigStandard</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mConfigStandard:${mconfig.version}'
```

### 2. Binary Signed Package (`mConfigBase`)

Best for: Enterprise environments requiring signed artifacts or a single "fat" JAR.
Fully compatible with the Java Module System (JPMS) since v0.7.26.

Maven:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigBase</artifactId>
    <version>${mconfig.version}</version>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mConfigBase:${mconfig.version}'
```

### 3. Full Mature Set (`mConfigFull`)

Best for: Modern cloud applications.
Includes Environment Variables, Windows Registry, Slf4j, and full JSON/YAML support via Jackson.

Maven:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigFull</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mConfigFull:${mconfig.version}'
```

## quick links
* [getting started - usage](documentation/src/site/markdown/2_how_do_I_use_it.md)
* the [documentation](documentation/src/site/markdown/index.md) - read from the start
* [notes for code-generating tools](documentation/src/site/markdown/ai-guidance.md)
* Jump to [design documentation](documentation/src/site/markdown/design_consolidated.md)


## Why a CLI for a Java Library?
[![CLI](https://img.shields.io/badge/CLI-mconfig-brightgreen)](mConfigTools/README.md)

Modern systems are complex, and maintenance can be a PITA.

The [mconfig tool](mConfigTools/README.md) 
provides instant auditability, allowing you to troubleshoot, 
monitor, and update your application's environment without writing a single line 
of code or restarting your service.

### Quick CLI Examples
```bash
# Diagnose a value's origin
mconfig mycompany:myapp get database.port -v

# List all discovered configs
mconfig mycompany:myapp list

# Monitor for changes (live-tail)
mconfig mycompany:myapp:network monitor --dump

# Generate schema from existing configs
mconfig mycompany:myapp propose-scheme > network.scheme.json
```
**Pro Tip:** Install via `.deb` package: `mvn -pl mConfigTools package && sudo dpkg -i mConfigTools/target/*.deb`

## Starters 🚀

[![Starters](https://img.shields.io/badge/Starters-examples/starters-brightgreen)](examples/starters)

Zero-config boilerplates for Maven, Gradle, JPMS. Copy → Run → No errors!

[examples/starters/README.md](examples/starters)

## Snippets 🚀

[![Snippets](https://img.shields.io/badge/Snippets-config-brightblue)](documentation/src/site/markdown/snippets.md)

Copy-paste fixes for everyday config problems: hardcodes, env vars, hot-reload, secrets, JPMS, tests.

[snippets.md](documentation/src/site/markdown/snippets.md)

## Design Philosophy
### Centralized Schemes, Not Call-Site Defaults
In mConfig, defaults are a first-class citizen of the Config Scheme.
Unlike java.util.Properties, where you provide a default at every get() call, mConfig lets you define the name, type, restrictions, and default value in one central schema.
Result: Your business logic remains clean.
`myStringValue = config.getString("db.timeout")`
The library automatically handles type coercion, validation, and default fallbacks based on your predefined scheme.

### Security by Design

Preventing "Log Leaks": By having a dedicated Secret type, mConfig can automatically mask these values (e.g., ********) in debug logs or administrative UIs, even if a developer accidentally calls toString() on the config object.
Late Decryption: Keeping the value encrypted in memory until the exact moment it is needed (JIT Decryption) reduces the window of opportunity for "heap dump" attacks.

#### Security & Secret Management

Configuration files often contain the "keys to the kingdom." 
mConfig doesn't just read data; it protects it.
##### Dedicated "Secret" Entry Types
Unlike standard libraries that treat passwords as plain strings, 
mConfig introduces a first-class Secret type. 

* Automatic Masking: 
  Secrets are protected from accidental exposure in logs, traces, and "ghost" file dumps.
* Schema-Enforced Security:
  Security is defined at the Scheme level. If a key is marked as a Secret,
  mConfig ensures it is handled with elevated safety protocols throughout its lifecycle.

##### JCE & BouncyCastle Integration
For environments requiring high-grade encryption, mConfig integrates 
with the Java Cryptography Extension (JCE).


##### security feature roadmap
as of version 0.8, the following features are planned for the next major release:
* keeping secrets encrypted in memory until they are needed:
* Plug-and-Play Providers: Easily use BouncyCastle for FIPS-compliant or Post-Quantum cryptographic algorithms.
* Hardware Security: Support for hardware-backed keys and certificate-based configuration decryption.
* Standardized Formats: Natively handle PEM, PKCS#12, and other cryptographic containers as configuration sources.


# get started
see the [documentation](documentation) module, and the examples!
