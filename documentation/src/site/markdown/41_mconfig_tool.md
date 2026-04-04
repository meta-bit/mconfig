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
- `propose-schema`: Generate a draft `ConfigSchema` JSON from an existing configuration.
- `validate`: Validate a configuration against its schema.
- `set`: Set or update a configuration entry. Supports `key=value` pairs or `-K/--key` with `-V/--value`. Requires a target `--scope` unless implied by schema.
- `monitor`: Monitor a configuration for real-time changes. Keeps running until interrupted.

### 4.1.2.1 `set` command options:
- `-V, --value=<value>`: The value to set. Shorthand `-V` is intentionally used for value (not for version).
- `-K, --key=<key>`: The key to set (optional when using `key=value` positional form).
- `-m, --comment=<text>`: Comment to associate with the entry (format support varies; shown with `get -v`).
- `-S, --scope=<scope>`: Target `ConfigScope` (`USER`, `HOST`, `SESSION`, `RUNTIME`, etc.).
- `-T, --type=<type>`: `ConfigEntryType` (optional, inferred from schema if available).
- `-F, --file-format=<fmt>`: Preferred file format when creating a new file (e.g., `PROPERTIES`, `YAML`, `TOML`).
- `-d, --dry-run`: Display what would be changed without performing the actual write.
- `--create`: Create missing parent directories and configuration files if they do not exist yet.

You can also pass one or more `key=value` pairs as positional arguments after the vector, e.g. `mconfig set acme:app:conf db.port=5432`.

N.B.: Some file formats may lose comments on write access. TOML, .INI, and .PROPERTIES are known to be safe.

### 4.1.2.2 `monitor` command options:
- `--dump`: Print the full effective configuration before starting the monitor loop.
- `-v, --verbose`: Provide more detail about changes (e.g., the scope and source of the change).

## 4.1.3 Global Options and Commands (verb/vector order)

The tool accepts both orders:
- `mconfig <verb> <vector> [...options]`
- `mconfig <vector> <verb> [...options]`

For example, `mconfig acme:app:conf show` is equivalent to `mconfig show acme:app:conf`.

Options can be placed both before and after the command (verb).

- `-c, --company=<name>`: Company name (default: `MCONFIG_COMPANY` env var). Use `""` (empty string) intentionally to omit the company segment in all paths/sources (e.g., `~/.config/myapp/`).
- `-a, --app=<name>`: Application name (default: `MCONFIG_APP` env var)
- `-C, --config=<name>`: Configuration name (default: `MCONFIG_CONFIG` env var)
- `-K, --key=<name>`: Specific configuration key
- `-o, --output=<format>`: Output format (`HUMAN`, `JSON`, `CSV`)
- `-v, --verbose`: Enable verbose output (shows source URIs and scopes)
- `--debug`: Enable debug logging for the CLI and underlying library
- `-l, --lang=<lang>`: Preferred language for descriptions (default: `en`)

Do not underestimate the power of "-v" verbose mode! Try it and see which
extra information you can get out of it.

## 4.1.4 Shortened Syntax

For quick interactive use, the tool supports a positional vector in the format:
`[COMPANY:]APPLICATION[:SUBDIR...]:CONFIGNAME[@KEY]`

Notes:
- `COMPANY` is optional. Use a leading `:` to omit it: `:myapp:settings`.
- Optional `SUBDIR` components map to subdirectories under the application root (per Resource Configuration Layout).
- The optional key is introduced with `@` to avoid ambiguity: `...:CONFIGNAME@KEY`.
- Legacy `:CONFIGNAME:KEY` syntax is no longer supported.

Examples:
- `mconfig mycompany:myapp:database show` (with company)
- `mconfig :myapp:database show` (app-only)
  Both equivalent to `mconfig --company [""""] --app myapp --config database show`.

## 4.1.5 Environment Variables

The tool automatically picks up the following environment variables if the corresponding options are not provided:
- `MCONFIG_COMPANY`
- `MCONFIG_APP`
- `MCONFIG_CONFIG`

Test/CI note: The CLI does not enable Test Mode by default. For isolated tests, you can direct discovery to temporary directories via the JVM system property `TESTMODE_DIRECTORIES` (e.g., `-DTESTMODE_DIRECTORIES=USER:/tmp/testdir`).

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
