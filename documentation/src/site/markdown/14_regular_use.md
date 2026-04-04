# 1.4 Regular Use

## 1.4.1 Providing a default configuration (PRODUCT scope)
The lowest-priority layer is `PRODUCT` scope. Defaults come from two places:
- **ConfigSchema defaults** (recommended for typed, documented defaults)
- **JAR resources** (optional: ship default config files inside the JAR)

For JAR resources, mConfig searches these paths (in order):
- `.config/[<company>/]<application>/<configName>.<ext>`
- `.config/<application>/<configName>.<ext>`
- `.config/<configName>.<ext>`

In a Maven/Gradle project, place them here:
- `src/main/resources/.config/[<company>/]<application>/<configName>.<ext>` (production)
- `src/test/resources/.config/[<company>/]<application>/<configName>.<ext>` (tests)

> **Note**: `<company>` optional; omitted if blank/null/whitespace. 
Normally, one should supply both company and application names.
(company, organization, vendor, etc - we use "company" here for brevity.)
While it is possible to omit the company name, it is not recommended.
Some environments may use no company name by convention, but developing multi-platform,


ConfigSchema defaults are always part of the PRODUCT scope default layer. If you want schemas
to *replace* existing defaults rather than merge them, use `SCHEME_RESETS_DEFAULTS`.

**Note:** JAR defaults are active when running from a JAR, but during development
the JAR source still reads resources from the classpath, so the same paths work in IDE runs.

## 1.4.2 Configuration Schemas Discovery
Config Schemas (`*.mconfig-schema.json`) are automatically discovered from multiple locations:

1.  **Classpath:** Scanned within `.config/` directories (e.g., `src/main/resources/.config/`).
2.  **Filesystem (system-wide and user):** Scanned in prioritized OS-specific locations. Schemas are stored under an app-scoped subdirectory:
    - `[<company>/]<application>/<configName>.mconfig-schema.json`
    - If company is blank/null/whitespace, the company segment is omitted (app-only paths).
    - **Windows:** `%APPDATA%\\mconfig\\schemas`, `%PROGRAMDATA%\\mconfig\\schemas`.
    - **Unix/Linux/Mac:** `~/.config/mconfig/schemas`, `/etc/mconfig/schemas`, `/usr/local/share/mconfig/schemas`.
    You can override this with `ConfigFeature.LOCAL_SCHEMA_DIRECTORY`.
3.  **Custom providers:** Only classpath and filesystem providers are built-in. If you need network-backed discovery, implement a `ConfigSchemaProvider` and register it via `ServiceLoader`.

- **Development Time:** Both `src/main/resources/.config/` and `src/test/resources/.config/` are searched on the classpath.
- **Precedence:** By default, schemas found in the **filesystem** take precedence over those found on the **classpath**. Within the same location, the one found last takes precedence.
- **Hardening:** Use `ConfigFeature.ALLOW_LOCAL_SCHEMA_OVERRIDE = false` to ignore filesystem schemas and ensure the application only uses its bundled contract.
- **Production vs Test:** Unlike configuration data, schemas are not bypassed in `TEST_MODE`. Production schemas are always loaded to ensure the configuration contract is maintained.

Recommended: Place in the main resources only.
Schemas are a "contract" for the configurations,
so they should be valid both for testing and production.


Most IDE don't do the testing with JAR files, they run class files from the
directories instead. 

The JAR config source won't just take any relative path for
its default values; either the files are within the JAR, or it refuses to use
them. That is intentional behaviour for security and safety purposes.

You can activate [Test Mode](15_test_mode.md), however: In Test Mode, the relative paths are 
accepted (along with all the other directories test mode uses).

## 1.4.3 Use a config schema

### 1.4.3.1 Reasons
There are some benefits to declaring the contents of your configurations
separately from the code.
- better maintenance (all in one place, instead of scattered)
- enables automated documentation (both runtime and compile-time), which is always up-to-date with the code, without extra work.
- type safety for your configuration parameters
- automatic parameter validation before your code gets the values.

Ideally, for a configuration there is just theme ConfigSchema you declared,
and the key names your code uses. Once the mConfig Configuration is instantiated,
the sole connection between the code and the values should be the keys it asks for.

(In the API, we stick with String keys for ease of use.
 If you want to be strict in your development, you can declare an enum and use
 its names as keys.)

### 1.4.3.2 Normal use

Normally, you'll place a JSON file in a subfolder of your resource directory, 
and that's it. 

The file name is the name of the Configuration you want to use,
plus the suffix ".mconfig-schema.json". It is a JSON(-style) file, following a specific
format (see [Configuration Schemas](23_configuration_schemas.md))

You declare all the configuration parameters your code can or will use.
mConfig will automatically detect and use these files.

Example: If your code uses a Configuration named "MyConfig" for
Company "MyCompany" and Application "MyApplication",
you would place the corresponding schema file in
`src/main/resources/.config/MyCompany/MyApplication/MyConfig.mconfig-schema.json`


### 1.4.3.3 Explicit setting

If, for reasons whatsoever, you want to provide the configuration schema yourself,
prepare the JSON string according to the format.
Convert it to a ConfigSchema
`ConfigSchema testSchema = ConfigSchema.statics.fromJSON(yourConfigSchemaString);`
and apply it to the respective Configuration
`cfg.setConfigSchema(testSchema);`.

Note: default values in your schema will replace potentially existing defaults.

## 1.4.4 Runtime Dynamic Configurations
Mutable Puts: you can change the values of existing entries at runtime.
Example:
```java
configFactory.put("voting.database.name", "dynamicDB", ConfigScope.RUNTIME);
```
The `RUNTIME` scope is at the highest priority ¹, so it overrides all others.
See **[Scopes](20_scopes.md)** for the full list.

So dynamically changing the configuration at runtime is quite possible.

¹: the highest priority of all regular scopes, that is. the `POLICY` scope
can enforce its settings over everything else; that's for security reasons.

## 1.4.5 Put and write

Configurations can be written to; the put operation is the same for all scopes.
The puts are typed.
You can pick either a single scope, or an enum set of scopes.
When writing, mConfig follows the "principle of least surprise" when writing to a scope:
- If an entry for the key already exists in a writeable layer within the scope,
its contents are changed, and written.
- If no entry for the key exists in the scope yet, it is created and written,
if there is a writeable layer available within the scope or scopes specified.
- If no writeable layer is available in the scope, the put fails with an exception. No silent data loss.
