# mConfigBasicBinary

<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigBasicBinary",
  "description": "Batteries-included base distribution of mConfig. Shaded JAR containing the core API, schema validation, secrets handling, and standard sources (JAR, Filesystem, EnvVar).",
  "programmingLanguage": {
    "@type": "ComputerLanguage",
    "name": "Java",
    "url": "https://www.java.com"
  },
  "runtimePlatform": "Java 9+",
  "isPartOf": {
    "@type": "SoftwareApplication",
    "name": "mConfig",
    "url": "https://github.com/meta-bit/mconfig"
  },
  "keywords": "shaded-jar, batteries-included, mconfig-core"
}
</script>
-->

mConfigBasicBinary is the "batteries-included" base distribution of mConfig packaged as a
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

Choose mConfigBasicBinary if you want a single artifact with a stable, signed payload
that "just works" out of the box, especially in enterprise or air-gapped
environments where dependency transparency is less important than ease of
deployment.

## How it differs from mConfigBasic

- Packaging: mConfigBasicBinary is a shaded JAR;
  mConfigBasic is a `pom` that aggregates dependencies.
- JPMS: mConfigBasicBinary provides the `mConfigBasicBinary` module and re-exports the core
  modules; with mConfigBasic you must `requires` individual modules in your
  `module-info.java`.
- Dependency control: mConfigBasic keeps dependencies visible and swappable;
  mConfigBasicBinary intentionally hides them behind one binary.
- JDK level: mConfigBasicBinary targets JDK 9+ (per its module-info); mConfigBasic
  follows the parent build defaults (currently JDK 11+).

## Usage

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

For more details, see the main project documentation in `documentation/`.
