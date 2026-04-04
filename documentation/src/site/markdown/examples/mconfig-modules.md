# mConfig Modules

mConfig is a modular library with which you can pick and choose the components you need.

## Choosing Your Modules

- **Start simple**: Use `mConfigBasic` for most applications
- **Need more formats?**: Use `mConfigStandard` to get JSON, YAML, and JSON5 support
- **Custom setup**: Include only the specific modules you need
- **Reduce if needed**: start with `mConfigCore` and add only what you need

All modules share the same version number and are designed to work together seamlessly.

Just add the modules you need to your project's dependencies.
mConfig will automatically load them at runtime, when they are in the classpath.

Java JPMS (since Java 9) considerations: see [Java Platform Module System](java-modules.md).

## mConfigBasic Modules

The `mConfigBasic` module provides a sensible minimum configuration to get started quickly.

| Module            | Artifact ID                 | Description                                       |
|-------------------|-----------------------------|---------------------------------------------------|
| Core API          | mconfigcore                 | Core interfaces and base classes                  |
| File Source       | mconfigsourcefilesystem     | configuration from files in standard OS locations |
| JAR Source        | mconfigsourcejar            | configuration from Java classpath/JAR resources   |
| Properties Format | mconfigformatjavaproperties | Support for Java .properties file format          |

Maven:
```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mconfigbasic</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```
Caveat: Do not omit/forget the type being `pom`.

Gradle:
```gradle
implementation 'org.metabit.platform.support.config:mconfigbasic:${mconfig.version}'
```

## mConfigStandard Modules

The `mConfigStandard` module includes everything from `mConfigBasic` plus additional mature, stable modules:

| Module                       | Artifact ID                    | Description                                             |
|------------------------------|--------------------------------|---------------------------------------------------------|
| Environment Variables Source | mconfigsourceenvvar            | Read configuration from environment variables           |
| Windows Registry Source      | mconfigwinregistry             | Read configuration from Windows Registry (Windows only) |
| JSON Format                  | mconfigformatjsonwithjackson   | Support for JSON configuration files using Jackson      |
| YAML (Jackson) Format        | mconfigformatyamlwithjackson   | Support for YAML configuration files using Jackson      |
| TOML Format                  | mconfigformattomlwithjackson   | Support for TOML configuration files using Jackson      |
| INI Format                   | mconfigformatini               | Support for INI configuration files                     |
| Mapper                       | mconfigmapper                  | Map configuration to Java objects                       |
| SLF4J Logging                | mconfigloggingslf4j            | Logging integration with SLF4J                          |

```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mconfigstandard</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

## Other Available Modules

Additional modules that are not included in `mConfigBasic` or `mConfigStandard`:

### Core System
| Module   | Artifact ID    | Description                                         |
|----------|----------------|-----------------------------------------------------|
| Core API | mconfigcore    | `mConfig :: Core` - Resolution logic and interfaces |
| Schema   | mconfigschema  | `mConfig :: Schema` - Type safety and validation    |
| Secrets  | mconfigsecrets | `mConfig :: Secrets` - Secure data handling         |

### Extensions & Add-ons
| Module          | Artifact ID   | Description                                      |
|-----------------|---------------|--------------------------------------------------|
| Mapper          | mconfigmapper | `mConfig :: Extension :: Mapper` - POJO mapping  |
| Utility Library | mconfigutil   | `mConfig :: Extension :: Utils` - Shared helpers |
| Cheese          | mconfigcheese | `mConfig :: Extension :: Cheese` - Shortcut APIs |

### Standalone Tools
| Module    | Artifact ID  | Description                                       |
|-----------|--------------|---------------------------------------------------|
| CLI Tools | mconfigtools | `mConfig :: Tool :: CLI` - Command-line interface |

### Sources
| Module               | Artifact ID                    | Description                                         |
|----------------------|--------------------------------|-----------------------------------------------------|
| HashiCorp Vault      | mconfigsourcevault             | Read secrets and configuration from Vault           |
| AWS Secrets Manager  | mconfigsourceawssecretsmanager | Read secrets from AWS Secrets Manager               |
| Apache ZooKeeper     | mconfigsourcezookeeper         | Read distributed configuration from ZooKeeper       |
| Windows Registry JNI | mconfigwinregistryjni          | Native Windows Registry source (requires JNI)       |

### Formats
| Module           | Artifact ID                    | Description                                            |
|------------------|--------------------------------|--------------------------------------------------------|
| JSON Schema      | mconfigformatjsonschema        | Support for JSON Schema validation                     |
| Raw File         | mconfigformatrawfile           | Read entire files as raw string values                 |
| YAML (SnakeYAML) | mconfigformatyamlwithsnakeyaml | Support for YAML using SnakeYAML instead of Jackson    |
| TOML (Internal)  | mconfigformattoml              | Support for TOML format, without external dependencies |

### Logging
| Module               | Artifact ID                    | Description                                         |
|----------------------|--------------------------------|-----------------------------------------------------|
| SLF4J Logging        | mconfigloggingslf4j            | Logging integration with SLF4J                      |

### Packages
| Module             | Artifact ID        | Description                                      |
|--------------------|--------------------|--------------------------------------------------|
| Basic (POM)        | mconfigbasic       | Standard set of mConfig modules (POM aggregator) |
| Standard (POM)     | mconfigstandard    | Extended set of mConfig modules (POM aggregator) |
| Basic Binary (JAR) | mconfigbasicbinary | Single JAR with all Basic modules (shaded)       |

## planned Modules
| Module            | Artifact ID                   | Description                                |
|-------------------|-------------------------------|--------------------------------------------|
| XML Format        | mconfigformatxml              | Support for XML configuration files        |
| JUL Logging       | mconfigloggingjul             | Logging integration with Java Util Logging |
| (Database Source) | mconfigsource(databasename)   | Read configuration from database           |
| (Remote Source)   | mconfigsource(networkservice) | Read configuration from remote servers     |
