# mConfig JPMS Maven Starter

## Quick Start

1. Copy files to new folder.
2. Run:
   ```
   mvn clean compile exec:java
   ```
   (Maven downloads mConfig modules; compiler handles JPMS.)

3. Expected output: Same as Maven minimal.

## Key Differences
- `mConfigBase` dep (JPMS-ready).
- `module-info.java` with correct `requires`.
- Compiler `--add-modules ALL-MODULE-PATH` for auto deps.

## Run with explicit modules (optional)
```
mvn exec:java -Dexec.args="--module-path target/classes:external-jars --add-modules ALL-MODULE-PATH com.example.starter.Main"
```

## Logging & Customize
See Maven README.

Proves no `IllegalAccessError`!