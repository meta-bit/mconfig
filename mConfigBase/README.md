# mConfigBase

mConfigBase is the "batteries-included" base distribution of mConfig packaged as a
single, (signed), shaded JAR. It bundles the core API plus the most common sources
and formats, while remaining JPMS-friendly (it ships a `module-info.class`).

## What it includes

- `mConfigCore` (core API)
- `mConfigScheme` (schema validation and defaults)
- `mConfigSecrets` (secret handling)
- `mConfigUtil` (utility helpers)
- `mConfigSourceJAR` (read config from classpath/JAR resources)
- `mConfigSourceFilesystem` (OS-aware filesystem locations)
- `mConfigSourceEnvVar` (environment variable source)
- `mConfigFormatJavaProperties` (Java properties format)

## When to use

Choose mConfigBase if you want a single artifact with a stable, signed payload
that "just works" out of the box, especially in enterprise or air-gapped
environments where dependency transparency is less important than ease of
deployment.

## How it differs from mConfigStandard

- Packaging: mConfigBase is a shaded JAR;
  mConfigStandard is a `pom` that aggregates dependencies.
- JPMS: mConfigBase provides the `mConfigBase` module and re-exports the core
  modules; with mConfigStandard you must `requires` individual modules in your
  `module-info.java`.
- Dependency control: mConfigStandard keeps dependencies visible and swappable;
  mConfigBase intentionally hides them behind one binary.
- JDK level: mConfigBase targets JDK 9+ (per its module-info); mConfigStandard
  follows the parent build defaults (currently JDK 11+).

## Usage

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

For more details, see the main project documentation in `documentation/`.
