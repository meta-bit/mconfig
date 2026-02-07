# 4.3 FAQ - Frequently Asked Questions
## 4.3.1 I want to...

### I want to extract a subconfiguration to a Properties object.

If your configuration contains a subset of entries, say, for an existing Java
component which expects them that way, and you just want to get an pass them
on,

then 
`ConfigUtil.copyPrefixedEntriesToJavaProperties()` is what should help you.

You specify the prefix, including possible "." from the hierarchy,
and get a java.util.Properties instance with all the contents from the Configuration
which start with the prefix. The prefix is removed automatically.

Caveat: This will skip configuration layers where the source does not allow
enumeration of its contents. So the result of this call may yield results
different from what you'd get if you were using the Configuration itself!

Recommendation: If that subconfig can be limited in what it may and should contain,
please consider using a ConfigScheme (in addition, or instead).

Also, have a look at other methods in the ConfigUtil class, for similar use cases.

### I want to know where my configuration files are located.

Use the mConfigTool provided.
It can tell you the general search paths (potential locations),
as well as the actual locations of the files it found,
and where the values in individual entries come from.


## 4.3.2 Why is...

### Why can't I state my defaults right at the place I'm asking for the value?

This approach sounds simpler - `getInteger("weightInTons",5)` looks good at first sight.
Until you realize what happens when you ask for this value a second time, or a third...
Also, this prevents you from generating a proper documentation of your configuration parameters.

ConfigSchemes should not be hard to use. Try them, please.


But if you insist on using the old way, 
there is the mConfigCheese module - for those who like their code a bit pungent.

## 4.3.3 How can I fix...

### Quick Verification Checklist
If it's not working yet:

1. **Place a test config file:**
   Create `~/.config/myCompany/myApplication/network.properties` (Linux/Mac) or `%APPDATA%\myCompany\myApplication\network.properties` (Windows):
   ```
   peer = localhost
   port = 8080
   probability = 0.5
   ```

2. **List discovered configs:**
   ```java
   try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication").build()) {
       factory.listAvailableConfigurations().forEach(System.out::println);
   }
   ```

3. **Enable logging:** Add `mConfigLoggingSlf4j` dep + SLF4J config (see [No mConfig logging output](#no-mconfig-logging-output) below).

4. **Use mConfigTool CLI:** `mconfig myCompany:myApplication get network.peer -v` (requires building mConfigTools; see [mConfigTools README](../../../../mConfigTools/README.md)).

5. **See detailed troubleshooting sections below.**

### Updates to config files are not detected
1. You are not buffering the results in some local variables, are you?
<br/>
Your own variables are not updated by mConfig.
Ask the Configuration object for the value instead whenever you need it.
2. Are the directories and files accessible for the account the program runs as?
mConfig needs to be able to list directory contents and read config files;
inaccessible dirs and files cause debug messages at most, but no error.
3. The cache setting doesn't match with your expected reaction time.
For efficiency, files are not checked and re-read on every config value access.
There is a delay between checks. If the default value (2 seconds) is too long
for your purposes, you can change it. @TODO name the config settings key.
Keep in mind a higher update frequency increases overhead.
4. Is the format supported?
You can choose and combine mconfig Format modules; if the file extension pattern
of the config file does not match with a file format present in your
current build, it won't be read.



 

### JPMS Module Access Errors

If your application uses JPMS (named `module-info.java`) but omits `requires` for mConfig modules, you will see access errors.

**Compile-time (javac):**
```
cannot access metabit.mconfig.core.ConfigFactory
  module your.module does not read module metabit.mconfig.core
```

**Runtime (java):**
```
java.lang.IllegalAccessError: class your.package.YourClass cannot access class metabit.mconfig.core.ConfigFactory (in module metabit.mconfig.core) because module your.module does not read module metabit.mconfig.core
```

**Fix:**
Add the required `requires` directives to your `module-info.java`.

The list depends on the mConfig variant:

* [mConfigFull](../../../../mConfigFull/README.md) (JPMS `requires` section)
* [mConfigStandard](../../../../mConfigStandard/README.md) (JPMS `requires` section)

See also [JPMS introduction](examples/java-modules.md).

### Config values always returning defaults or null

**Symptoms:** Calls like `cfg.getString("key")` return `null` or scheme defaults; no exceptions or warnings.

**Common causes:**
- Mismatched `company`, `application`, or `configName` (case-sensitive).
- No config files in standard locations (classpath `src/main/resources/.config/<company>/<app>/<configName>.properties` or filesystem equivalents).
- File extension not matching available formats (e.g., `.yaml` without YAML format module).
- Permissions issues on directories/files (check logs).

**Fix steps:**
1. Use `factory.listAvailableConfigurations()` to list discovered configs and their sources.
2. Run [mConfigTool](../../../../mConfigTools/README.md) CLI for full diagnostics: paths searched, files found, entry origins.
3. Verify placements per [Getting Started](13_getting_started.md).
4. Add missing format modules (e.g., `mConfigFormatYAMLwithJackson` for YAML).

### Tests not loading expected configurations

**Symptoms:** Unit/integration tests use production defaults or empty configs despite `setTestMode(true)`.

**Causes:**
- Missing `ConfigFactoryBuilder.setTestMode(true)` before `build()`.
- Test files not in correct locations.
- `forbidTestMode()` called or `PERMIT_TEST_MODE=false`.

**Fix:**
1. Add `@BeforeAll static void setup() { ConfigFactoryBuilder.setTestMode(true); }`
2. Place files in:
   - `src/test/resources/.config/<company>/<app>/<configName>.ext` (preferred)
   - `src/test/resources/config/<SCOPE>` (e.g., `USER`)
3. Custom: `ConfigFactoryBuilder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:src/test/configs"));`
4. See full details in [Test Mode](15_test_mode.md).

### No mConfig logging output

**Symptoms:** Silent on config discovery, errors, or debug info.

**Fix:**
1. Add dependency: `mConfigLoggingSlf4j`.
2. Configure SLF4J backend (e.g., Logback `logback.xml`):
   ```xml
   <logger name="metabit.config" level="DEBUG"/>
   ```
3. Ensure SLF4J implementation (logback-classic, log4j-slf4j) on classpath.
See [Logging](42_logging.md).

### Environment variables don't provide config values

**Symptoms:** Env vars like `myapp-network-peer=localhost` ignored.

**Fix:**
1. Add `mConfigSourceEnvVar` dependency.
2. Use naming scheme: `<application>_<configName>_<key>` (lowercase, e.g., `myapp_network_peer`).
3. Scope: `SESSION` (overrides USER).
4. Verify via `mConfigTool` or enable logging.
See [mConfigSourceEnvVar README](../../../../mConfigSourceEnvVar/README.md).
