# mConfig CLI tool

<!--
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareSourceCode",
  "name": "mConfigTools",
  "alternateName": "mconfig CLI",
  "description": "The mconfig command: see where every configuration value of an application comes from. Reads, sets and validates configuration across files, environment variables, the Windows registry and Kubernetes ConfigMaps, shows which of mConfig's ten scopes supplies each value (get -v), and watches changes live (monitor). Exports JSON, YAML, TOML or CSV for scripts and CI pipelines; secrets show as [REDACTED] unless explicitly revealed.",
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
  "keywords": "cli, devops, configuration, configuration-audit, monitoring, validation, environment-variables, kubernetes, windows-registry, json, yaml, toml, secrets, picocli"
}
</script>
-->

mConfig CLI: 60-Second "Magic" Cheat Sheet

Configuration doesn't need to be a "black box."
Use the mconfig tool to audit, troubleshoot, and automate your environment instantly.

1. The "Why is it doing that?" Audit
   Ever wonder which of your 10 ConfigScopes is actually providing the value you get in the end?
   Stop guessing and see the "Effective Value" along with its precise origin.

```bash
# Get the effective value, scope (USER, HOST, etc.), and source URI
mconfig mycompany:myapp:database get port -v
```

2. The "Live-Tail" Monitor
   Stop restarting your application to test configuration changes. 
   Watch your configuration tree update in real-time as you edit files or registry keys
   (or when K8s ConfigMap changes are pushed to your cluster).

```bash
# Monitor settings for changes and dump the delta to the console
mconfig mycompany:myapp:settings monitor --dump -v
``` 

3. The "Instant Schema" Creator
   Have a mess of legacy properties and no documentation? 
   Let mConfig write your first Config Scheme for you.

```bash
# Scan existing configs and propose a type-safe JSON schema
mconfig -X mycompany:myapp:legacy-app propose-schema > schema.json
```
Move from "loose strings" to Type-Safe Validation in seconds.

> **Not available yet:** `propose-schema` is experimental and currently disabled; it is being
> rewritten for 0.9.2. See [Experimental commands](../documentation/src/site/markdown/41_mconfig_tool.md#experimental-commands).

4. The DevOps Pipeline Bridge
   Automate your infrastructure by extracting values in machine-readable format for jq, Python, or Bash scripts.

```bash
# Export the effective configuration as a JSON object
mconfig mycompany:myapp:network show -f JSON | jq '.timeout'
```

## Shortcuts

```bash
# Provide context first, then key
mconfig mycompany:myapp:config get database/port

# Set using key=value shorthand (scope still required)
mconfig mycompany:myapp:config set database/port=5432 --scope user
```

## Output formats

```bash
# YAML and TOML output are supported alongside JSON/CSV/HUMAN
mconfig mycompany:myapp:config show -f YAML
mconfig mycompany:myapp:config show -f TOML
```

## Secrets

Values of secret entries (schema `"SECRET": true`, or from a secrets source such as Vault) are
shown as `[REDACTED]` in every command and output format; key, scope and source stay visible.
`set` does not echo a secret value either. To see one value in plain text, ask for it explicitly:

```bash
mconfig mycompany:myapp:database get password --reveal
```

See [Where secret values may appear](../documentation/src/site/markdown/26_secrets_concept.md#where-secret-values-may-appear).

## Scope filtering and file format hints

```bash
# Restrict get to specific scopes
mconfig mycompany:myapp:config get database/port --scope USER,HOST

# Prefer a file format when a new layer must be created (set)
mconfig mycompany:myapp:config set database/port=5432 --scope USER --file-format TOML
```

## Shell completion

Generate shell completion dynamically (bash, zsh, fish if supported by picocli):

```bash
# Load completion for the current shell session
source <(mconfig __complete bash)
```

Packaging also generates a completion artifact at `mConfigTools/target/completions/mconfig.bash`.
To include additional names in that artifact, build with:

```bash
mvn -pl mConfigTools -am package -Dmconfig.completion.names=mconfig
```

## Debian package (jar + wrapper)

Build the `.deb` (includes jar, wrappers, and bash completion):

```bash
mvn -pl mConfigTools -am package
```

Install locally:

```bash
sudo dpkg -i mConfigTools/target/*.deb
```

The package installs:
- `/usr/bin/mconfig`
- `/usr/share/mconfig/mConfigTools.jar`
- `/usr/share/bash-completion/completions/mconfig`

-------
Dependencies:
- Java 11
- mConfig, of course
- picocli
- Jackson (for JSON, YAML, TOML)
- slf4j 

