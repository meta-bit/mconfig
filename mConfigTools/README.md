# mConfig CLI tool

mConfig CLI: 60-Second "Magic" Cheat Sheet

Configuration doesn't need to be a "black box."
Use the mconfig tool to audit, troubleshoot, and automate your environment instantly.

1. The "Why is it doing that?" Audit
   Ever wonder which of your 9 ConfigScopes is actually providing the value you get in the end?
   Stop guessing and see the "Effective Value" along with its precise origin.

```bash
# Get the effective value, scope (USER, HOST, etc.), and source URI
mconfig mycompany:myapp:database:port get -v
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
mconfig mycompany:myapp:legacy-app propose-scheme > schema.json
```
Move from "loose strings" to Type-Safe Validation in seconds.

4. The DevOps Pipeline Bridge
   Automate your infrastructure by extracting values in machine-readable format for jq, Python, or Bash scripts.
   bash

```bash
# Export the entire HOST scope as a JSON object
mconfig mycompany:myapp:network show --scope HOST -o JSON | jq ' .timeout'
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
mconfig mycompany:myapp:config show -o YAML
mconfig mycompany:myapp:config show -o TOML
```

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
Depedencies:
- Java 11
- mConfig, of course
- picocli
- Jackson (for JSON, YAML, TOML)
- slf4j 

