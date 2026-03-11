
### 0.8.8
* upgraded Jackson dependencies to 2.21.1 across all Jackson-based format modules.
* refined CLI tool and configuration writing to better support "Moderate Whitesmiths" indentation, including fixes for brace alignment.
* bugfix: fixed `show` verb output format bug in the CLI tool.
* metadata: added JSON-LD descriptions for multiple modules to enhance discoverability.
* documentation: updated project URL and module links in the main `pom.xml` and module descriptors.

### 0.8.7
* introduced `ConfigEvent` mechanism for recording lifecycle events (e.g., parse failures, discovery skips).
* added thread-safe, capped event retention with de-duplication and severity-based filtering.
* enhanced CLI tool (`mconfig`):
    * added support for showing and adding comments via CLI.
    * improved "monitor" mode with timestamped changes and source tracking.
    * improved error messaging and verb/vector argument order flexibility.
    * added support for "Moderate Whitesmiths" indentation in configuration writing.
* bugfix: JAR configuration source now correctly filters by path.
* bugfix: fixed "wrong level" nesting issues in TOML and INI format writers.
* bugfix: hierarchical `.mconfig-schema.json` now correctly matches the internal structure.
* metadata: added JSON-LD descriptions to `README.md` and several modules for better discoverability.
* unified `getEvents()` access across `Configuration` and `ConfigFactory` APIs.

### 0.8.6
* finalized transition from "Scheme" to "Schema" across all modules, APIs, and documentation.
* modernized schema discovery using a modular, provider-based SPI architecture (`ConfigSchemaProvider`).
* introduced `mConfigSchema` module for core schema implementation and parsing.
* enhanced validation framework with specialized validators: `PortValidator`, `EmailValidator`, `DurationValidator`, `SizeValidator`, and `TemporalValidator` (with `now` alias).
* introduced `mConfigFormatJsonSchema` module for standard JSON Schema (json-schema.org) support. (do not confuse with `mConfigSchema`)
* added `ConfigSchemaExporterComponent` for exporting registered schemas to local filesystem.
* improved JPMS compliance in `mConfigFormatJsonSchema` by using modular dependencies and explicit module requirements.
* maintained backward compatibility for legacy `*.scheme.json` and `*.schema.json` files during discovery.

### 0.8.5
* improved SnakeYAML module to persist comments in YAML files.
* improved mConfig TOML module to fully persist comments
* improved mConfig INI module to persist comments
* enabled write support via ConfigCursor.

### 0.8.4
* new standalone TOML module (mConfigFormatTOML).
* improved GitHub CI workflows for publication.
* miscellaneous documentation fixes and build improvements.

### 0.8.3
* fixes for publication workflow; source unchanged.

### 0.8.0
* public release moved to github.

### 0.7.x

* substantial changes to the API
* adding many modules.
* schemes, change notification, etc.
* adding support for JPMS and Java 11+; 
* public releases via maven central 

### 0.6.0
* overhaul, separate modules
* compiles separately for JDK8, and JDK9+

## 0.5.3
first public release

The first implementation goes back to 1996, when Java 1 was still fresh.