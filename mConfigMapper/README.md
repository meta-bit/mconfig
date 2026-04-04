# mConfigMapper

<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigMapper",
  "description": "Experimental mapping module for mConfig. Aims to provide flexible and efficient ways to map configuration data between different formats and POJO structures.",
  "programmingLanguage": {
    "@type": "ComputerLanguage",
    "name": "Java",
    "url": "https://www.java.com"
  },
  "runtimePlatform": "Cross-platform",
  "isPartOf": {
    "@type": "SoftwareApplication",
    "name": "mConfig",
    "url": "https://github.com/meta-bit/mconfig"
  },
  "keywords": "pojo-mapping, experimental, configuration-binding"
}
</script>
-->

This project provides a flexible way to map configuration data between `ConfigCursor`, POJOs, and files.

## Experimental Status

The `mConfigMapper` module is currently in an **experimental stage**.
While it is fully functional for basic use cases, its API and functionality are subject to change.

### Why it is considered Experimental:

1. **Limited Type Support**: The current implementation supports a core set of Java types (`String`, `Integer`, `Boolean`, `Long`, `Double`, `byte[]`). 
    It lacks robust automatic conversion for:
    - **Collections**: `List` (beyond simple strings) and `Map` structures.
    - **Enums**: Automatic mapping of configuration strings to Enum constants.
    - **Complex Types**: Standard Java types like `Path`, `URI`, or custom date/time formats.
2. **Write-Back Complexity**: The `mapPojoToCursor` functionality, which writes POJO properties back into the configuration, is still being refined to ensure correct scope prioritization and metadata preservation (like comments).
3. **Decoupling Pattern**: Integration with `mConfigCore` is currently handled via reflection to keep the core library small.
   A more standardized service-based or plugin architecture for mapping is under investigation.
4. **API Stability**: As we add support for mapping annotations (e.g., to handle field renaming or default values), the current `ConfigMapper` interface may evolve.

Feedback and suggestions are welcome!

## Usage

Basic reading into a POJO:

```java
ConfigCursor cursor = config.getConfigCursor();
TestPojo pojo = ConfigMapper.create().readObject(cursor, TestPojo.class);
```

For more details, see the [mConfig Documentation](https://github.com/meta-bit/mconfig/tree/master/documentation).

