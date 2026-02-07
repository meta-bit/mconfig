# mConfig Starters 🚀

Frustration-free boilerplates to start with mConfig in seconds.

## Starters

| Starter | Build Tool | JPMS | Run Command |
|---------|------------|------|-------------|
| [maven-minimal](maven-minimal) | Maven | No | `mvn compile exec:java` |
| [gradle-kotlin](gradle-kotlin) | Gradle Kotlin DSL | No | `./gradlew run` |
| [jpms-maven](jpms-maven) | Maven | **Yes** | `mvn compile exec:java` |

## How to Use
1. `git clone https://github.com/meta-bit/mconfig`
2. `cd examples/starters/<starter>`
3. Follow README.md in starter.
4. Success—no deps/JPMS errors!

## Why?
- Copy-paste ready.
- Includes logging, sample config/scheme.
- Matches FAQ checklist.
- Demonstrates CLI, hints.

More coming (Full, Secrets, EnvVar...)!

[![CLI](https://img.shields.io/badge/CLI-mconfig-brightgreen)](../../mConfigTools/README.md)

**Pro Tip:** Update `mconfig.version` (Maven) or `mconfigVersion` (Gradle) to the [latest release](https://github.com/meta-bit/mconfig/releases).