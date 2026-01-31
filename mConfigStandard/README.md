# mConfigStandard

mConfigStandard is the recommended starting point for mConfig. It is a `pom`-packaged
aggregator that pulls in the core API plus the essential sources and formats.

## What it includes

- `mConfigCore` (core API)
- `mConfigScheme` (schema validation and defaults)
- `mConfigSecrets` (secret handling)
- `mConfigUtil` (utility helpers)
- `mConfigSourceJAR` (read config from classpath/JAR resources)
- `mConfigSourceFilesystem` (OS-aware filesystem locations)
- `mConfigFormatJavaProperties` (Java properties format)

## Usage

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

Note: Do not omit the `pom` type in Maven.

## Runtime requirements

- Minimum JDK: 11
- Required JDK modules: `java.base`, `java.logging`

full JPMS `requires`:

```java
requires metabit.mconfig.core;
requires metabit.mconfig.scheme;
requires metabit.mconfig.secrets;
requires metabit.mconfig.util;
requires metabit.mconfig.modules.jar;
requires metabit.mconfig.modules.filesystem;
requires metabit.mconfig.format.javaproperties;
```

The above set is needed for the runtime service discovery to find all modules.
