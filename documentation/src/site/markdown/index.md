# mConfig Documentation

mConfig is a modular Java library for unified, portable configuration management. It abstracts OS-specific locations (e.g., `~/.config/`, `/etc/`, Windows Registry), multiple sources (files, env vars, JAR resources, ZooKeeper), and formats (JSON, YAML, TOML, Properties, etc.). 

**Core Principle**: Layered resolution stack prioritizes scopes (highest first):

```mermaid
graph LR
  POLICY[POLICY<br/>Enforced/GPO] --> RUNTIME[RUNTIME<br/>Volatile RAM]
  RUNTIME --> SESSION[SESSION<br/>Env/CLI]
  SESSION --> USER[USER<br/>~/.config/]
  USER --> APPLICATION[APPLICATION<br/>Portable/Install]
  APPLICATION --> HOST[HOST<br/>/etc/, HKLM]
  HOST --> CLUSTER[CLUSTER<br/>ZK/Networks]
  CLUSTER --> CLOUD[CLOUD<br/>Providers]
  CLOUD --> ORGANIZATION[ORGANIZATION<br/>Licensee]
  ORGANIZATION --> PRODUCT[PRODUCT<br/>JAR Defaults]

  classDef high fill:#ff9999
  class POLICY,RUNTIME high
  ```

Ties within scope: Later-added > earlier; storage prio: RAM > secrets > files > registry > JAR.

**Quick Start**: [What is mConfig?](0_what_is_this.md) → [Getting Started](getting-started.md)

**Architecture**: [Design Consolidated](design_consolidated.md)

## Getting Started
- [What is mConfig?](0_what_is_this.md)
- [What does it do?](1_what_does_it_do.md)
- [Getting Started](getting-started.md)
- [Regular Use](regular-use.md)
- [Test Mode](test-mode.md)
- [mConfig Tool](mconfig-tool.md)
- [Notes for Code-Generating Tools](ai-guidance.md)
- [Configuration Schemes](configuration-schemes.md)
- [Extensions and Advanced Use](2_6_extensions_and_advanced_use.md)
- [Writing Configurations](writing-configurations.md)
- [Updates](2_8_updates.md)
- [Getting Information](2_9_getting_information.md)
- [Definitions and Valid Values](2_10_definitions_and_valid_values.md)
- [Windows Registry Access](windows-registry-access.md)
- [Handling Secrets](handling-secrets.md)

## Concepts and Behavior
- [How does it work?](3_how_does_it_work.md)
- [Priorities and Hierarchies](3_2_priorities_and_hierarchies.md)
- [Config Features](4_1_ConfigFeatures.md)
- [FAQ](4_FAQ.md)

## Examples and References
- [Examples](examples/index.md)
- [Links and References](5_links.md)
- [Design Consolidated](design_consolidated.md)

## Project
- [Versions](7_versions.md)
- [Contributing](8_how_to_contribute.md)
- [Legal](6_legal.md)
