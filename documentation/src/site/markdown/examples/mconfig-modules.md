# mConfig Modules

mConfig is a modular library where you can pick and choose the components you need.

## Choosing Your Modules

- **Start simple**: Use `mConfigStandard` for most applications
- **Need more formats?**: Use `mConfigFull` to get JSON, YAML, and JSON5 support
- **Custom setup**: Include only the specific modules you need
- **reduce if needed**: start with 'mConfigCore' and add only what you need

All modules share the same version number and are designed to work together seamlessly.

Just add the modules you need to your project's dependencies.
mConfig will automatically load them at runtime, when they are in the classpath.

Java JPMS (since Java 9) considerations: see [Java Platform Module System](java-modules.md).

## mConfigStandard Modules

The `mConfigStandard` module provides a sensible minimum configuration to get started quickly.

| Module                   | Artifact ID                   | Description                                            |
|--------------------------|-------------------------------|--------------------------------------------------------|
| Core API                 | mConfigCore                   | Core interfaces and base classes                       |
| File Source              | mConfigSourceFile             | configuration from files in standard OS locations |
| System Properties Source | mConfigSourceSystemProperties | configuration from Java system properties         |
| Properties Format        | mConfigFormatJavaProperties   | Support for Java .properties file format               |

Maven:
```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigStandard</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```
Caveat: Do not omit/forget the type being `pom`.

Gradle:
```gradle
implementation 'org.metabit.platform.support.config:mConfigStandard:${mconfig.version}'
```

## Additional mConfigFull Modules

The `mConfigFull` module includes everything from `mConfigStandard` plus additional mature, stable modules:

| Module                       | Artifact ID                    | Description                                             |
|------------------------------|--------------------------------|---------------------------------------------------------|
| Environment Variables Source | mConfigSourceEnvVar            | Read configuration from environment variables           |
| Windows Registry Source      | mConfigWinRegistry             | Read configuration from Windows Registry (Windows only) |
| JSON Format                  | mConfigFormatJSONwithJackson   | Support for JSON configuration files using Jackson      |
| JSON5 Format                 | mConfigFormatJSONwithJackson   | Support for JSON5 configuration files using Jackson     |
| YAML (Jackson) Format        | mConfigFormatYAMLwithJackson   | Support for YAML configuration files using Jackson      |
| YAML (SnakeYAML) Format      | mConfigFormatYAMLwithSnakeYAML | Support for YAML configuration files using SnakeYAML    |
| TOML Format                  | mConfigFormatTOMLwithJackson   | Support for TOML configuration files using Jackson      |
| INI Format                   | mConfigFormatINI               | Support for INI configuration files                     |
| Mapper                       | mConfigMapper                  | Map configuration to Java objects                       |
| SLF4J Logging                | mConfigLoggingSlf4j            | Logging integration with SLF4J                          |

@TODO: add Maven dependency XML for `mConfigFull`

## Other Available Modules

Additional modules that are not included in `mConfigStandard` or `mConfigFull`:

| Module          | Artifact ID           | Description                                      |
|-----------------|-----------------------|--------------------------------------------------|
| Cheese          | mConfigCheese         | some fragrant code for those with a taste for it |


## planned Modules

| Module            | Artifact ID                   | Description                                |
|-------------------|-------------------------------|--------------------------------------------|
| XML Format        | mConfigFormatXML              | Support for XML configuration files        |
| JUL Logging       | mConfigLoggingJUL             | Logging integration with Java Util Logging |
| (Database Source) | mConfigSource(DatabaseName)   | Read configuration from database           |
| (Remote Source)   | mConfigSource(NetworkService) | Read configuration from remote servers     |
