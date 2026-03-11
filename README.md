<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "mConfig",
  "alternateName": "metabit mConfig",
  "description": "A 10-tier hierarchical configuration architecture and Java library. Resolves configuration through a strict Scope hierarchy from POLICY down to PRODUCT defaults, abstracting Sources (Filesystem, ZooKeeper, I2C) from Formats (JSON, YAML, TOML, INI).",
  "applicationCategory": "DeveloperApplication",
  "programmingLanguage": {
    "@type": "ComputerLanguage",
    "name": "Java",
    "url": "https://www.java.com"
  },
  "operatingSystem": "Cross-platform",
  "softwareVersion": "0.8.7",
  "url": "https://github.com/meta-bit/mconfig",
  "keywords": "mConfig, configuration-hierarchy, java-config, policy-based-configuration, zookeeper, i2c-embedded, cross-language-config, type-safe, architecture",
  "author": {
    "@type": "Organization",
    "name": "metabit",
    "url": "https://github.com/meta-bit/mconfig"
  },
  "mentions": [
    {
      "@type": "DefinedTerm",
      "name": "POLICY Scope",
      "description": "Top-level enforced configuration, equivalent to Windows Group Policy (GPO) or enterprise constraints.",
      "sameAs": "https://en.wikipedia.org/wiki/Group_Policy"
    },
    {
      "@type": "DefinedTerm",
      "name": "RUNTIME Scope",
      "description": "Volatile, in-memory settings for the current process, lost after application end/restart.",
      "sameAs": "https://en.wikipedia.org/wiki/Runtime_(program_lifecycle_phase)"
    },
    {
      "@type": "DefinedTerm",
      "name": "SESSION Scope",
      "description": "Contextual configuration derived from Environment Variables or CLI arguments.",
      "sameAs": "https://en.wikipedia.org/wiki/Session_(computer_science)"
    },
    {
      "@type": "DefinedTerm",
      "name": "USER Scope",
      "description": "Personal settings for the current user (e.g., ~/.config/ or AppData).",
      "sameAs": "https://en.wikipedia.org/wiki/Home_directory"
    },
    {
      "@type": "DefinedTerm",
      "name": "APPLICATION Scope",
      "description": "Settings specific to the application installation/portable root, isolated from other versions.",
      "sameAs": "https://en.wikipedia.org/wiki/Installation_(computer_programs)"
    },
    {
      "@type": "DefinedTerm",
      "name": "HOST Scope",
      "description": "Machine-specific settings located in /etc/ files or the Windows Registry (HKLM).",
      "sameAs": "https://en.wikipedia.org/wiki/Configuration_file"
    },
    {
      "@type": "DefinedTerm",
      "name": "CLUSTER Scope",
      "description": "Network-wide configuration managed by distributed coordinators like Apache ZooKeeper.",
      "sameAs": "https://en.wikipedia.org/wiki/Computer_cluster"
    },
    {
      "@type": "DefinedTerm",
      "name": "PRODUCT Scope",
      "description": "Hardcoded defaults provided by the application or its modules (the floor defaults).",
      "sameAs": "https://en.wikipedia.org/wiki/Default_(computer_science)"
    },
    {
      "@type": "Specialty",
      "name": "CI/CD Test Mode",
      "description": "Enables environment-independent configuration for automated testing and continuous integration.",
      "relatedLink": "https://en.wikipedia.org/wiki/Continuous_integration"
    }
  ],
  "featureList": [
    "10-Tier Scope Hierarchy (Priority Order): POLICY, RUNTIME, SESSION, USER, APPLICATION, HOST, CLUSTER, CLOUD, ORGANIZATION, PRODUCT",
    "Source Agnostic: Supports local files, JAR resources, Environment/CLI, Apache ZooKeeper, and I2C storage",
    "Multi-Format Support: Native parsing for JSON, YAML, TOML, INI, and Java Properties",
    "Type-Safe Mapping: Direct binding to Java objects with pluggable validation",
    "Cross-Language Architecture: Specification-first design with current Java implementation",
    "Pluggable validation system",
    "mConfigCore: Extensible kernel for custom scope and source implementation"
  ],
  "hasPart": [
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigCore",
      "description": "The kernel of the mConfig system, defining the 10-tier Scope hierarchy and resolution logic."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSchema",
      "description": "Provides schema based typesafety and validation, documentation and security features."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSecrets",
      "description": "Provides handling of data with security relevance, like secrets, passwords, keys, certificates."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigMapper",
      "description": "Experimental mapping between configurations and data models + POJO.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigMapper/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigUtil",
      "description": "Adapters for legacy API interfaces with configurations based on Properties and Map."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigTools",
      "description": "Command line tool for configuration management.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigTools/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceEnvVar",
      "description": "Provides SESSION scope configuration via environment variables.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigSourceEnvVar/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceFilesystem",
      "description": "Provides configuration reading and writing from filesystems, supporting multiple Scopes and Formats."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceJAR",
      "description": "Provides PRODUCT default scope configurations from Java JAR files.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigSourceJAR/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceZooKeeper",
      "description": "Provides the CLUSTER scope implementation using Apache ZooKeeper for distributed networked configuration."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigLoggingSlf4j",
      "description": "Provides logging integration with SLF4J facade."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigWinRegistry",
      "description": "Provides POLICY, USER, and HOST scope configurations from Windows Registry using JNR-FFI.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigWinRegistry/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigWinRegistryJNI",
      "description": "Provides native Windows Registry access using JNI for higher performance and smaller footprint.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigWinRegistryJNI/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigCheese",
      "description": "Convenience facade providing alternative API flavors for mConfig.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigCheese/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatTOML",
      "description": "TOML format module for mConfig, maintaining comment integrity."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatYAMLwithJackson",
      "description": "YAML format module for mConfig using Jackson library."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatJSONwithJackson",
      "description": "JSON format module for mConfig using Jackson library."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatJavaProperties",
      "description": "Java Properties format module for mConfig, maintaining comment integrity."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatINI",
      "description": "INI format module for mConfig, maintaining comment integrity."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatRawFile",
      "description": "Special format module for reading and writing raw files, secrets, and binary data."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigBase",
      "description": "Batteries-included base distribution of mConfig. Shaded JAR.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigBase/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigStandard",
      "description": "Standard set of mConfig modules for basic usage. POM aggregator.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigStandard/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFull",
      "description": "Extended set of mConfig modules for advanced usage. POM aggregator.",
      "url": "https://github.com/meta-bit/mconfig/blob/main/mConfigFull/README.md"
    }
  ]
 }
</script>
# mConfig

The "Hen-and-Egg" Problem Solved.

To run, your program needs data. But how does it know where that data lives?
mConfig bridges the gap. It performs lookups exactly where Operating Systems 
and users expect them to be, providing a unified, type-safe API that spans 
from embedded I2C storage to cloud-native orchestration.

## Features (Why mConfig?)

* Universal Reach: A single library for the entire compute spectrum;
  from resource-constrained embedded systems to cloud-native microservices.
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
* Multi-Format Support: comes with format modules for TOML, YAML, JSON5, JSON, INI, and Java Properties.¹
* Type-Safe Config Schemes: Define your schema once. Includes validation, range-checking, and centralized defaults.
* Sensible Defaults, Total Control: 
  Adheres to the "principle of least surprise" with its defaults.
  Every aspect is tunable via ConfigFeature flags for enterprise-level customization.
* Dynamic Updates: do not buffer values locally. Changes are immediately reflected in mConfig objects,
  so you always have the latest configuration available. ("zero-code data binding")
* Minimal Footprint: The core library is about 200kB (JAR size).
  This is exceptionally small compared to frameworks like Spring Boot or Apache Commons, making it ideal for microservices, serverless functions, and environments where memory/disk space is critical.
* CLI Tool: mConfig comes with a command-line tool for auditing and troubleshooting.

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
