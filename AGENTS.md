# AGENTS.md - mConfig AI Agent Guide

## 1. Project Overview
**mConfig** is a modular, 10-tier hierarchical configuration library for Java. It is designed to be cross-platform (Linux, Windows, Android, embedded) and agnostic of specific storage backends or formats.

### Core Philosophy
- **Layered Stack**: Configurations are resolved through a stack of layers: `Cache` -> `Sources` (ordered by scope) -> `Defaults`.
- **Scope-Based Priority**: Scopes define precedence (e.g., `POLICY` > `USER` > `PRODUCT`).
- **Separation of Concerns**: Decouples `ConfigStorage` (where it is) from `ConfigFormat` (how it is parsed).
- **Extension Features**: Supports module-specific settings (e.g., `VaultFeatures`) that are decoupled from the core library but still type-safe.
- **Immutability & Safety**: Prefers immutable configuration structures and safe, typed access.
- **Embedded-Friendly**: `mConfigCore` is designed to work even on systems without a traditional filesystem.

---

## 2. Quick Start for Application Developers

### 2.1 Bootstrapping mConfig
The entry point is `ConfigFactoryBuilder`. Use it to configure library features and build a `ConfigFactory`.

```java
// Basic initialization
ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "demoApp")
    .build();

// Get a configuration handle
Configuration config = factory.getConfig("network");

// Read typed values
int port = config.getInteger("server.port");
String host = config.getString("server.host");
```

### 2.2 Recommended Resource Layout
Place your default configurations and schemas in your Maven/Gradle resource folder:
- `src/main/resources/.config/<company>/<app>/<config>.<ext>`
- `src/main/resources/.config/<company>/<app>/<config>.mconfig-schema.json`

e.g., `src/main/resources/.config/metabit/demoApp/network.properties` and `src/main/resources/.config/metabit/demoApp/network.mconfig-schema.json`.
---

## 3. Core Concepts

### 3.1 The 10-Tier Scope Hierarchy
Priority from Highest (1) to Lowest (10):
1.  **`POLICY`**: Enforced settings (e.g., GPO).
2.  **`RUNTIME`**: Volatile, in-memory settings (highest normal override).
3.  **`SESSION`**: Environment variables or CLI arguments.
4.  **`USER`**: Personal user settings (e.g., `~/.config/`).
5.  **`APPLICATION`**: App-specific portable root settings.
6.  **`HOST`**: System-wide settings (e.g., `/etc/`).
7.  **`CLUSTER`**: Distributed configuration (e.g., ZooKeeper).
8.  **`CLOUD`**: Shared across multiple clusters.
9.  **`ORGANIZATION`**: Settings shared across an organization.
10. **`PRODUCT`**: Hardcoded defaults (from JAR resources).

### 3.2 mConfig Schema
The Schema is a core feature (like the 10-tier hierarchy) that provides type safety, validation, and documentation for configuration entries.

- **Role**: Defines expected keys, their types, default values, and validation rules.
- **Discovery**: Automatically discovered from `.config/` directories in the classpath and prioritized OS-specific locations (e.g., `/etc/mconfig/schemas`, `~/.config/mconfig/schemas`).
- **Naming**: JSON files named `<config>.mconfig-schema.json`.
- **Core Fields**: `KEY`, `TYPE`, `DEFAULT`, `DESCRIPTION`, `FLAGS`, `PATTERN`, `ARITY`, `MANDATORY`.
- **Detailed Documentation**: [Configuration Schemas](documentation/src/site/markdown/23_configuration_schemas.md).

---

## 4. Application Development Guidelines

### 4.1 Value Access & Conversion
- **Use typed getters**: Prefer `getInteger()`, `getBoolean()`, `getEnum()`, etc. from the `Configuration` facade.
- **Use Features**: Set library-wide settings (core or extension) via `ConfigFactoryBuilder.setFeature()`. Prefer constants (`ConfigFeature.TEST_MODE`, `VaultFeatures.BOOTSTRAP_CONFIG_NAME`) for type safety.
- **Do NOT parse or validate manually**: mConfig handles conversion, schema-based defaults, and mandatory field validation. 
- **Use mConfig Features**: Always prefer library features (Schemas, `MANDATORY` flags, `DEFAULT` values in JSON) over manual code checks or hardcoded fallbacks.
- **Defaults**: Do not hardcode defaults in Java. Use Schemas or resource files (Section 2.2).

### 4.2 Error Handling
- **`ConfigException` (Unchecked)**: Thrown for severe issues or logic errors where recovery is impossible (e.g., invalid schema, missing mandatory entry).
- **`ConfigCheckedException`**: Used for operations that might fail due to external factors (e.g., writing to a read-only location).
- **Rule of Thumb**: If the library can fall back to a default, it will not throw an exception unless `EXCEPTION_ON_MISSING_ENTRY` is explicitly enabled.

### 4.3 Handling Secrets
- **Retrieve**: Use `config.getSecret("key")` which returns a `SecretValue`.
- **Usage**: `SecretValue` obfuscates data in memory. Access raw data via `getValue()` and call `erase()` after use to wipe it from memory.
- **Logging**: Secret values are automatically redacted (`[REDACTED]`) in logs and `toString()` outputs.

### 4.4 Testing (Test Mode)
- **Isolation**: Use `ConfigFactoryBuilder.setTestMode(true)` in tests to bypass actual OS paths and prevent interference with real user data.
- **Test Resources**: Place test-specific configs in `src/test/resources/.config/`.
- **Detailed Documentation**: [Test Mode](documentation/src/site/markdown/15_test_mode.md).

---

## 5. Design Decisions & Reference

### 5.1 Code Style
mConfig uses **Moderate Whitesmiths style**. 
- **Tenet**: Opening and closing braces MUST be in the same column, OR in the same row.
- **Full Rules**: See [devdocs/formatting.md](devdocs/formatting.md).
- this applies to mConfig library development, not for the application developer.

### 5.2 Library Internals
- **Self-Configuration**: mConfig can be configured via `.config/metabit/mConfig/mconfig.properties` in the classpath. Source modules should use separate configuration handles (e.g., `vault`, `aws`) and separate files (e.g., `vault.properties`) in the same directory to maintain modularity.
- **No DI Frameworks**: Core avoids Spring/Guice; uses simple Constructor/Builder patterns.
- **Logging**: Uses `ConfigLoggingInterface` (no `System.out`).
- **Internal JSON Parsing**: Use mConfig's built-in `JsonStreamParser` (from `mConfigSchema` module) for lightweight internal JSON parsing needs. Avoid adding external dependencies like Jackson if possible.
- **Library Development**: For extending mConfig (new storages/formats), see [devdocs/library_development.md](devdocs/library_development.md).

---

## 6. Glossary
- **`ConfigLocation`**: Pointer to "where" a config might be (Storage + Handle + Scope).
- **`ConfigSource`**: Instantiated `ConfigLocation` bound to a `ConfigLayer`.
- **`ConfigLayer`**: A concrete map of data from a source.
- **`ConfigEntry`**: A leaf node (value + metadata).
- **`ConfigFacade`**: The high-level API (`Configuration` interface).
- **`ConfigFeature`**: A configuration flag for the mConfig library itself.
