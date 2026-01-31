# mConfig Gradle Kotlin DSL Starter

## Quick Start

1. Copy all files from this directory to a new folder (e.g., `my-project`).
2. Ensure Gradle 8+ installed (or run `./gradlew` after wrapper).
3. Run:
   ```
   gradle wrapper --gradle-version 8.5
   ./gradlew run
   ```
4. Expected output:
   ```
   Peer: localhost
   Port: 8080
   Probability: 0.5
   Available configs: 1 found
   ```

## Features Included
- Same as Maven: `mConfigStandard` + Logging + sample config.
- Kotlin DSL `build.gradle.kts`.

## Logging
Add `src/main/resources/logback.xml` (see Maven README).

## Customize
- Update versions in `build.gradle.kts`.
- `./gradlew build` for JAR.

See main mConfig docs!