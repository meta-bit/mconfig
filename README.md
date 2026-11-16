<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "mConfig",
  "alternateName": "metabit mConfig",
  "description": "A 10-tier hierarchical configuration architecture and Java library. Resolves configuration through a strict Scope hierarchy from POLICY down to PRODUCT defaults, abstracting Storages (Filesystem, JAR, environment, Windows registry, Vault, AWS Secrets Manager, Kubernetes, ZooKeeper) from Formats (JSON, YAML, TOML, INI, Java Properties).",
  "applicationCategory": "DeveloperApplication",
  "programmingLanguage": {
    "@type": "ComputerLanguage",
    "name": "Java",
    "url": "https://www.java.com"
  },
  "operatingSystem": "Cross-platform",
  "softwareVersion": "0.8.17",
  "url": "https://github.com/meta-bit/mconfig",
  "keywords": "mConfig, configuration-hierarchy, java-config, policy-based-configuration, zookeeper, i2c-embedded, cross-language-config, type-safe, architecture, configuration-hot-reload, kubernetes-configmap, environment-variables, config-schema, secret-redaction, windows-registry-group-policy, test-isolation",
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
      "description": "System-wide settings located in /etc/ files, %ProgramData%, or the Windows Registry (HKLM).",
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
    "Storage Agnostic: Supports local files, JAR resources, environment variables, Windows registry, HashiCorp Vault, AWS Secrets Manager, Kubernetes ConfigMaps and Secrets, Apache ZooKeeper; embedded storages such as I2C are a design target, not shipped yet",
    "Multi-Format Support: JSON, YAML, TOML, INI, and Java Properties",
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
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigMapper/README.md"
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
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigTools/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceEnvVar",
      "description": "Provides SESSION scope configuration via environment variables.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceEnvVar/README.md"
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
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceJAR/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceZooKeeper",
      "description": "Provides the CLUSTER scope implementation using Apache ZooKeeper for distributed networked configuration.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceZooKeeper/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceVault",
      "description": "Provides CLOUD scope configuration via HashiCorp Vault (opt-in module, not in the Basic or Standard set).",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceVault/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceAwsSecretsManager",
      "description": "Provides CLOUD scope configuration via AWS Secrets Manager (opt-in module).",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceAwsSecretsManager/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceKubernetes",
      "description": "Provides CLUSTER scope configuration via Kubernetes ConfigMaps and Secrets, using the JDK HTTP client (opt-in module).",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceKubernetes/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigSourceKubernetesFabric8",
      "description": "Kubernetes ConfigMaps and Secrets via the Fabric8 client (opt-in module, alternative to mConfigSourceKubernetes).",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigSourceKubernetesFabric8/README.md"
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
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigWinRegistry/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigWinRegistryJNI",
      "description": "Provides native Windows Registry access using JNI for higher performance and smaller footprint.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigWinRegistryJNI/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigCheese",
      "description": "Convenience facade providing alternative API flavors for mConfig.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigCheese/README.md"
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
      "description": "JSON and JSON5 format module for mConfig using the Jackson library."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatTOMLwithJackson",
      "description": "TOML format module for mConfig using the Jackson library."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatYAMLwithSnakeYAML",
      "description": "YAML format module for mConfig using SnakeYAML."
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigFormatJsonSchema",
      "description": "Reads standard JSON Schema files as mConfig schemas (opt-in module)."
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
      "name": "mConfigBasicBinary",
      "description": "Batteries-included base distribution of mConfig. Shaded JAR.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigBasicBinary/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigBasic",
      "description": "Standard set of mConfig modules for basic usage. POM aggregator.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigBasic/README.md"
    },
    {
      "@type": "SoftwareSourceCode",
      "name": "mConfigStandard",
      "description": "Extended set of mConfig modules for advanced usage. POM aggregator.",
      "url": "https://github.com/meta-bit/mconfig/blob/master/mConfigStandard/README.md"
    }
  ]
 }
</script>
-->
# mConfig
status

| Type       | Status                                                                                                                                                                                                              |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build (CI) | [![Build (github)](https://github.com/meta-bit/mconfig/actions/workflows/maven.yml/badge.svg)](https://github.com/meta-bit/mconfig/actions/workflows/maven.yml)                                                     |
| Artifact   | [![Maven Central](https://img.shields.io/maven-central/v/org.metabit.platform.support.config/mconfig.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.metabit.platform.support.config/mconfig) |
| Javadoc    | [![javadoc](https://javadoc.io/badge2/org.metabit.platform.support.config/mconfigcore/javadoc.svg)](https://javadoc.io/doc/org.metabit.platform.support.config/mconfigcore)                                         |
<!-- dependabot, coverage etc. planned ->

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
  Registry/ProgramData conventions on Windows.
* Modular Drop-in Support:
  mConfig implements Modular Directory Discovery, automatically layering .d fragments into the configuration stack without manual setup.
* Multi-Format Support: comes with format modules for TOML, YAML, JSON5, JSON, INI, and Java Properties.¹
* Type-Safe Config Schemas: Define your schema once. Includes validation, range-checking, and centralized defaults.
* Secrets: entries marked `"SECRET": true` in the schema, or coming from a secret store, are returned as `SecretValue`,
  shown as `[REDACTED]` in logs and exports, and can be erased from memory after use.
* Sensible Defaults, Total Control:
  Adheres to the "principle of least surprise" with its defaults.
  Every aspect is tunable via ConfigFeature flags for enterprise-level customization.
* Dynamic Updates: do not buffer values locally. Changes are immediately reflected in mConfig objects,
  so you always have the latest configuration available. ("zero-code data binding")
* Minimal Footprint: The core library is about 230kB (JAR size).
  This is exceptionally small compared to frameworks like Spring Boot or Apache Commons, making it ideal for microservices, serverless functions, and environments where memory/disk space is critical.
* CLI Tool: mConfig comes with a command-line tool for scripting, auditing, and troubleshooting.
* Test Mode: for CI/CD and local development, test mode provides a clean environment separate from deployment paths.

¹ parsing of TOML,YAML,JSON5,JSON can be done using mature external libraries,
so using these formats will increase your application's footprint.
For TOML, INI, and Java Properties formats, mConfig provides native modules, which are smaller and preserve comments.

**Java:** 11 or newer. The Jackson-based format modules (JSON, YAML, TOML via Jackson, JSON Schema) and everything
that includes them, such as `mconfigstandard` and the CLI, need Java 17.

## usage

Choose the entry point that fits your project architecture. `${mconfig.version}` is a placeholder:
use the current release from Maven Central (badge above) or the [CHANGELOG](CHANGELOG.md).

### 1. Basic Set (`mConfigBasic`)

Best for: Lightweight, file-based Java projects. Contains core, schema, secrets, the filesystem and
JAR storages, the Java Properties format and the util adapters. Java 11.

Maven:

```xml

<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mconfigbasic</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mconfigbasic:${mconfig.version}'
```

### 2. Binary Signed Package (`mConfigBasicBinary`)

Best for: Enterprise environments requiring signed artifacts or a single "fat" JAR.
Fully compatible with the Java Module System (JPMS) since v0.7.26.
If you build your own single JAR instead, merge the `META-INF/services` files (shade's
`ServicesResourceTransformer`, Shadow's `mergeServiceFiles()`); otherwise storages go missing and
`build()` refuses to start. See "Packaging as a single JAR" in
[Getting Started](documentation/src/site/markdown/13_getting_started.md).

Maven:

```xml

<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mconfigbasicbinary</artifactId>
    <version>${mconfig.version}</version>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mconfigbasicbinary:${mconfig.version}'
```

### 3. Standard Mature Set (`mConfigStandard`)

Best for: Modern cloud applications. Java 17.
Everything in the Basic set, plus environment variables, the Windows registry, SLF4J logging,
the POJO mapper, INI, and JSON/YAML/TOML via Jackson and YAML via SnakeYAML.

Not in either set, added as separate dependencies when needed: HashiCorp Vault, AWS Secrets Manager,
Kubernetes (two variants), Apache ZooKeeper, and the JSON Schema reader.

Maven:

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mconfigstandard</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

Gradle:

```gradle
implementation 'org.metabit.platform.support.config:mconfigstandard:${mconfig.version}'
```

## quick links

* [getting started - usage](documentation/src/site/markdown/13_getting_started.md)
* the [documentation](documentation/src/site/markdown/index.md) - read from the start
* [AGENTS.md](AGENTS.md): the compact guide for AI agents and code generators (API routing map, rules, pitfalls);
  [notes for code-generating tools](documentation/src/site/markdown/52_ai_guidance.md)
* Jump to [design documentation](documentation/src/site/markdown/44_design_consolidated.md)
* [CHANGELOG](CHANGELOG.md): behaviour changes per release, check before upgrading

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
mconfig mycompany:myapp:database get port -v

# List all discovered configs
mconfig mycompany:myapp list

# Show the full combined config contents
mconfig mycompany:myapp:database show

# Show the combined config contents as JSON (secret values appear as [REDACTED])
mconfig mycompany:myapp:database show --format JSON

# set a value
mconfig mycompany:myapp:database set port=3306 

# set a value, create file if none existed yet. 
mconfig mycompany:myapp:database set frequency=0.5s --create 

# Monitor for changes (live-tail)
mconfig mycompany:myapp:network monitor --dump
```

**Pro Tip:** Install via `.deb` package: `mvn -pl mConfigTools package && sudo dpkg -i mConfigTools/target/*.deb`

## Starters 🚀

[![Starters](https://img.shields.io/badge/Starters-examples/starters-brightgreen)](examples/starters)

Zero-config boilerplates for Maven, Gradle, JPMS. Copy → Run → No errors!

[examples/starters/README.md](examples/starters)

## Snippets 🚀

[![Snippets](https://img.shields.io/badge/Snippets-config-brightblue)](examples/snippets.md)

Copy-paste fixes for everyday config problems: hardcodes, env vars, hot-reload, secrets, JPMS, tests.

- [examples/snippets.md](examples/snippets.md): code templates to copy. The Java templates are compiled against the current API on every build.
- [Snippets Reference](documentation/src/site/markdown/snippets.md): exact API names, schema fields, settings and scopes to look up.
- [Better Practice: Replace This With That](documentation/src/site/markdown/38_code_improvements.md): what to replace in existing code, with and without mConfig.

## Design Philosophy

### Centralized Schemas, Not Call-Site Defaults

In mConfig, defaults are a first-class citizen of the Config Schema.
Unlike java.util.Properties, where you provide a default at every get() call, mConfig lets you define the name, type, restrictions, and default value in one central schema.
Result: Your business logic remains clean.
`myStringValue = config.getString("db/timeout")`
The library automatically handles type coercion, validation, and default fallbacks based on your predefined schema.
(The key separator is `/`; a dot is a literal part of the key.)

### Security by Design

Configuration files often contain the "keys to the kingdom."
mConfig doesn't just read data; it protects it from accidental disclosure.

* Secrets are marked, not typed: there is no separate entry type. An entry is a secret when its schema
  says `"SECRET": true`, or when it comes from a secret store (Vault, AWS Secrets Manager, Kubernetes Secrets).
* Retrieval as `SecretValue` via `config.getSecret("key")`, with `getValue()` / `getChars()` and `erase()`
  to wipe the content from memory after use; never as a plain `String`.
* Automatic redaction: secret values appear as `[REDACTED]` in logs, `toString()`, CLI output and exports.
  A value is only given out in plain text on an explicit single-value request (`mconfig get <key> --reveal`).
* Where it stays: a secret is kept intact in its own storage, but never copied into another context
  (`ConfigMapper.saveTo()` leaves it out).
* Safe diagnostics: when a configuration file cannot be parsed, mConfig reports where and why
  (file, line, column, key), but leaves out the parser's quote of the content wherever that content
  may be a secret: entries marked `SECRET`, secret stores such as Kubernetes Secrets, and the
  credentials of the cloud modules. Text from outside is escaped in all log lines, so a
  configuration file cannot forge log entries.
* Scope of protection: accidental disclosure. mConfig does not protect secrets in process memory
  (heap dumps, debuggers, code in the same process).
* Late decryption, i.e. secrets as opaque handles that are resolved only at the moment of use, is the
  planned secrets model. Status: not implemented yet; today a secret value is held decrypted in memory
  once read. See [Secrets Concept](documentation/src/site/markdown/26_secrets_concept.md).

# get started

see the [documentation](documentation) module, and the examples!