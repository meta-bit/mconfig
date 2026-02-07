# 4.1 mConfig Tool (`mconfig`)

The `mconfig` tool provides a command-line interface for inspecting and managing configurations. It allows you to list available configurations, show effective values, get detailed metadata for specific keys, search for configuration paths, and more.

## 4.1.1 Installation

The tool is available as a "fat JAR" in the `mConfigTools` module. You can run it using:

```bash
java -jar mConfigTools-*.jar [OPTIONS] [COMMAND] [COMMAND_OPTIONS]
```

## 4.1.2 Subcommands

- `list`: List all available configurations for the given company and application.
- `show`: Print the effective configuration (all keys and values).
- `get`: Show detailed information for a specific key (value, type, source URI, scope, and description).
- `search-paths`: List all directories and storage locations where mConfig looks for files.
- `propose-scheme`: Generate a draft `ConfigScheme` JSON from an existing configuration.
- `validate`: Validate a configuration against its scheme.
- `set`: Set or update a configuration entry. Requires `--value` and `--scope`.
- `monitor`: Monitor a configuration for real-time changes. Keeps running until interrupted.

### 4.1.2.1 `set` command options:
- `-V, --value=<value>`: The value to set.
- `-S, --scope=<scope>`: Target `ConfigScope` (`USER`, `HOST`, `SESSION`, `RUNTIME`, etc.).
- `-T, --type=<type>`: `ConfigEntryType` (optional, inferred from scheme if available).
- `-d, --dry-run`: Display what would be changed without performing the actual write.

### 4.1.2.2 `monitor` command options:
- `--dump`: Print the full effective configuration before starting the monitor loop.
- `-v, --verbose`: Provide more detail about changes (e.g., the scope of the change).

## 4.1.3 Global Options and Commands

Options can be placed both before and after the command (verb).

- `-c, --company=<name>`: Company name (default: `MCONFIG_COMPANY` env var). Use `""` (empty string) intentionally to omit the company segment in all paths/sources (e.g., `~/.config/myapp/`).
- `-a, --app=<name>`: Application name (default: `MCONFIG_APP` env var)
- `-C, --config=<name>`: Configuration name (default: `MCONFIG_CONFIG` env var)
- `-k, --key=<name>`: Specific configuration key
- `-o, --output=<format>`: Output format (`HUMAN`, `JSON`, `CSV`)
- `-v, --verbose`: Enable verbose output (shows source URIs and scopes)
- `-l, --lang=<lang>`: Preferred language for descriptions (default: `en`)

Do not underestimate the power of "-v" verbose mode! Try it and see which
extra information you can get out of it.

## 4.1.4 Shortened Syntax

For quick interactive use, the tool supports a positional argument in the format:
`[COMPANY:]APPLICATION[:CONFIGNAME[:KEY]]` where `COMPANY` can be empty `""` to omit the company segment in paths (use leading `:` e.g. `:myapp:settings`).

Examples:
- `mconfig mycompany:myapp:database show` (with company)
- `mconfig :myapp:database show` (app-only)
  Both equivalent to `mconfig --company [""""] --app myapp --config database show`.

## 4.1.5 Environment Variables

The tool automatically picks up the following environment variables if the corresponding options are not provided:
- `MCONFIG_COMPANY`
- `MCONFIG_APP`
- `MCONFIG_CONFIG`

## 4.1.6 Examples

1. List configurations:
   ```bash
   mconfig --company mycomp --app myapp list
   ```

2. Show effective configuration:
   ```bash
   mconfig mycomp:myapp:settings show
   # OR
   mconfig show --company mycomp --app myapp --config settings
   ```

3. Get value and metadata for a specific key:
   ```bash
   mconfig mycomp:myapp:db:password get
   ```

4. Change output to JSON for scripting:
   ```bash
   mconfig mycomp:myapp:settings show -o JSON
   ```

5. See where the values in your configuration are coming from:
   ```bash
   mconfig mycomp:myapp:settings show -v
   ```

6. Set a configuration value:
   ```bash
   mconfig mycomp:myapp:db:user set --value="dbuser" --scope=USER
   ```

7. Update a configuration value with dry-run and validation:
   ```bash
   mconfig mycomp:myapp:settings:timeout set -V 30 -S HOST -T NUMBER --dry-run
   ```

8. Monitor configuration changes:
  This one's for you, SRE and developers!
   ```bash
   mconfig mycomp:myapp:settings monitor --dump -v
   mconfig :myapp:settings monitor --dump -v  # app-only
   ```

9. App-only examples (omit company segment):
   ```bash
   mconfig -c "" myapp list
   mconfig :myapp:db get password
   ```
