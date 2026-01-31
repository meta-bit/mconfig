# mConfigFull

mConfigFull is the "everything included" distribution: it aggregates mConfigStandard
plus mature sources, formats, mapper, and SLF4J logging.

## What it includes

- `mConfigStandard` (core + schemes + secrets + util + JAR/filesystem + properties)
- `mConfigSourceEnvVar` (environment variable source)
- `mConfigFormatJavaProperties` (properties format)
- `mConfigFormatJSONwithJackson` (JSON/JSON5 via Jackson)
- `mConfigFormatYAMLwithJackson` (YAML via Jackson)
- `mConfigFormatTOMLwithJackson` (TOML via Jackson)
- `mConfigFormatINI` (INI format)
- `mConfigLoggingSlf4j` (SLF4J logging)
- `mConfigMapper` (map configs to Java objects)
- `mConfigWinRegistry` (Windows Registry source, JNR-FFI based)

## Usage

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

Note: Do not omit the `pom` type in Maven.

## Runtime requirements

- Minimum JDK: 11
- Required JDK modules: `java.base`, `java.logging`

External runtime deps (via included modules):
- Jackson: `jackson-core`, `jackson-databind`, `jackson-dataformat-yaml`, `jackson-dataformat-toml`
- SLF4J API: `org.slf4j:slf4j-api`
- JNR-FFI (Windows Registry): `org.jnrproject:jnr-ffi`

JPMS `requires` (modules with explicit `module-info.java`):

```java
requires metabit.mconfig.core;
requires metabit.mconfig.scheme;
requires metabit.mconfig.secrets;
requires metabit.mconfig.util;
requires metabit.mconfig.modules.jar;
requires metabit.mconfig.modules.filesystem;
requires metabit.mconfig.format.javaproperties;
requires metabit.mconfig.format.ini;
requires metabit.mconfig.modules.envvar;
requires metabit.mconfig.winregistry;
requires metabit.mconfig.modules.jsonwithjackson;
requires metabit.mconfig.modules.yamlwithjackson;
requires metabit.mconfig.modules.tomlwithjackson;
requires metabit.mconfig.mapper;
requires metabit.mconfig.modules.mConfigLoggingSlf4j;
```
