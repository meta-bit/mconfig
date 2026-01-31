# 2.2 Regular Use

## 2.2.1 Providing a default configuration (PRODUCT scope)
The lowest-priority layer is `PRODUCT` scope. Defaults come from two places:
- **ConfigScheme defaults** (recommended for typed, documented defaults)
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


ConfigScheme defaults are always part of the PRODUCT scope default layer. If you want schemes
to *replace* existing defaults rather than merge them, use `SCHEME_RESETS_DEFAULTS`.

**Note:** JAR defaults are active when running from a JAR, but during development
the JAR source still reads resources from the classpath, so the same paths work in IDE runs.

## 2.2.2 Configuration Schemes Discovery
Config Schemes (`*.scheme.json`) are automatically discovered from the `.config/` directory on the classpath.

- **Development Time:** Both `src/main/resources/.config/` and `src/test/resources/.config/` are searched.
- **Precedence:** If a scheme for the same configuration name exists in both locations, the one found last by the `ClassLoader` takes precedence.
- **Production vs Test:** Unlike configuration data, schemes are not bypassed in `TEST_MODE`. Production schemes are always loaded from the classpath to ensure the configuration contract is maintained.

Recommended: Place in the main resources only.
Schemes are a "contract" for the configurations,
so they should be valid both for testing and production.


Most IDE don't do the testing with JAR files, they run class files from the
directories instead. 

The JAR config source won't just take any relative path for
its default values; either the files are within the JAR, or it refuses to use
them. That is intentional behaviour for security and safety purposes.

You can activate <a href="2_3_test_mode.md">Test Mode</a>, however; in Test Mode, the relative paths are 
accepted (along with all the other directories test mode uses).

## 2.2.3 Use a config scheme

### 2.2.3.1 Reasons
There are some benefits to declaring the contents of your configurations
separately from the code.
- better maintenance (all in one place, instead of scattered)
- enables automated documentation (both runtime and compile-time), which is always up-to-date with the code, without extra work.
- type safety for your configuration parameters
- automatic parameter validation before your code gets the values.

Ideally, for a configuration there is just theme ConfigScheme you declared,
and the key names your code uses. Once the mConfig Configuration is instantiated,
the sole connection between the code and the values should be the keys it asks for.

(In the API, we stick with String keys for ease of use.
 If you want to be strict in your development, you can declare an enum and use
 its names as keys.)

### 2.2.3.2 Normal use

Normally, you'll place a JSON file in a subfolder of your resource directory, 
and that's it. 

The file name is the name of the Configuration you want to use,
plus the suffix ".scheme.json". It is a JSON(-style) file, following a specific
format (@TODO link to format section).

You declare all the configuration parameters your code can or will use.
mConfig will automatically detect and use these files.

Example: If your code uses a Configuration named "MyConfig" for
Company "MyCompany" and Application "MyApplication",
you would place the corresponding scheme file in
src/main/resources/.config/MyCompany/MyApplication/MyConfig.scheme.json


### 2.2.3.3 Explicit setting

If, for reasons whatsoever, you want to provide the configuration scheme yourself,
prepare the JSON string according to the format.
Convert it to a ConfigScheme
`ConfigScheme testScheme = ConfigScheme.statics.fromJSON(yourConfigSchemeString);`
and apply it to the respective Configuration
`cfg.setConfigScheme(testScheme);`.

Note: default values in your scheme will replace potentially existing defaults.

## 2.2.4 Runtime Dynamic Configurations
Mutable Puts: you can change the values of existing entries at runtime.
Example:
```java
configFactory.put("voting.database.name", "dynamicDB", ConfigScope.RUNTIME);
```
The RUNTIME scope is at the highest priority ¹, so it overrides all others.

So dynamically changing the configuration at runtime is quite possible.

¹: the highest priority of all regular scopes, that is. the POLICY scope
can enforce its settings over everything else; that's for security reasons.

## 2.2.5 Put and write

Configurations can be written to; the put operation is the same for all scopes.
The puts are typed.
You can pick either a single scope, or an enum set of scopes.
When writing, mConfig follows the "principle of least surprise" when writing to a scope:
- If an entry for the key already exists in a writeable layer within the scope,
its contents are changed, and written.
- If no entry for the key exists in the scope yet, it is created and written,
if there is a writeable layer available within the scope or scopes specified.
- If no writeable layer is available in the scope, the put fails with an exception. No silent data loss.
