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
please consider using a ConfigSchema (in addition, or instead).

Also, have a look at other methods in the ConfigUtil class, for similar use cases.

### I want to know where my configuration files are located.

Use the mConfigTool provided.
It can tell you the general search paths (potential locations),
as well as the actual locations of the files it found,
and where the values in individual entries come from.

Programmatically, mConfig allows you to trace the origin of each individual configuration value you get.
If your interest focused on filesystem only, ConfigUtil.whereToPutMyFiles() may be what you are looking for.

## 4.3.2 Why is...

### Why can't I state my defaults right at the place I'm asking for the value?

This approach sounds simpler - `getInteger("weightInTons",5)` looks good at first sight.
Until you realize what happens when you ask for this value a second time, or a third...
Also, this prevents you from generating a proper documentation of your configuration parameters.

ConfigSchemas should not be hard to use. Try them, please.


But if you insist on using the old way, 
there is the mConfigCheese module - for those who like their code a bit pungent.

### Coming from Spring: why are there no `${...}` placeholders, profiles or conditional values?

Because mConfig answers the same needs by *where* a value is stored, not by rules inside the
configuration. For every entry you can ask which scope and which location it came from
(`getScope()`, `getLocation()`, or `mconfig get <key> -v`), and the answer is one place.
Placeholders, profiles and conditions each break that: the value you read is then computed from
several places, or depends on a switch set somewhere else.

**No `${...}` placeholders.** A value is what its source says, character by character.

* A value assembled from other keys has no single origin; "where does this come from?" has no
  clear answer any more.
* A reference to another key can pull a secret into a value that is not marked as one, and from
  there into logs and exports.
* A `$` in a password or a path needs no escaping.

If two settings share a part, keep the part as its own entry and combine in code, or store the
complete value at the place that differs.

**No profiles.** There is no `application-prod.properties` and no switch that selects it. What
differs between deployments is stored where the deployment is:

| In Spring                            | In mConfig                                                                                                                       |
|:-------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------|
| defaults in `application.properties` | defaults in the JAR (`PRODUCT` scope), or `DEFAULT` in the schema                                                                |
| `application-prod.properties`        | a file on the production host (`HOST` scope, e.g. below `/etc`), or values from the cluster or cloud source (`CLUSTER`, `CLOUD`) |
| a developer's local overrides        | the user's own file (`USER` scope)                                                                                               |
| overrides per container or start     | environment variables (`SESSION` scope)                                                                                          |
| the `test` profile                   | [test mode](15_test_mode.md) with `src/test/resources/.config/`                                                                  |

The JAR is the same everywhere and carries no production values. See
[Priorities and Hierarchies](22_priorities_and_hierarchies.md) for the order in which the scopes apply.

**No conditional values.** There is no "if the host is called `prod-*`" and no `@ConditionalOnProperty`
for values. Which value applies follows from the fixed order of the scopes alone, so the same
question gives the same answer, and the tool can show why.

mConfig itself runs fine inside a Spring application; see "Spring Boot wiring" in the
[snippets](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md#11-spring-boot-wiring).

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

4. **Use mConfigTool CLI:** `mconfig myCompany:myApplication get network.peer -v` (requires building mConfigTools; see [mConfigTools README](https://github.com/meta-bit/mconfig/blob/master/mConfigTools/README.md)).

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

* [mconfigbasic](https://github.com/meta-bit/mconfig/blob/master/mConfigBasic/README.md) (JPMS `requires` section)
* [mconfigstandard](https://github.com/meta-bit/mconfig/blob/master/mConfigStandard/README.md) (JPMS `requires` section)
* [mconfigbasicbinary](https://github.com/meta-bit/mconfig/blob/master/mConfigBasicBinary/README.md) (JPMS `requires` section)

See also [JPMS introduction](examples/java-modules.md).

### Config values always returning defaults or null

**Symptoms:** Calls like `cfg.getString("key")` return `null` or schema defaults; no exceptions or warnings.

**Common causes:**
- Mismatched `company`, `application`, or `configName` (case-sensitive).
- No config files in standard locations (classpath `src/main/resources/.config/<company>/<app>/<configName>.properties` or filesystem equivalents).
- File extension not matching available formats (e.g., `.yaml` without YAML format module).
- Permissions issues on directories/files (check logs).
- Application packed into a single JAR without merging `META-INF/services` files; see
  [Faulty single-JAR packaging](#faulty-single-jar-packaging) below.

**Fix steps:**
1. Use `factory.listAvailableConfigurations()` to list discovered configs and their sources.
2. Run [mConfigTool](https://github.com/meta-bit/mconfig/blob/master/mConfigTools/README.md) CLI for full diagnostics: paths searched, files found, entry origins.
3. Verify placements per [Getting Started](13_getting_started.md).
4. Add missing format modules (e.g., `mConfigFormatYAMLwithJackson` for YAML).

### Faulty single-JAR packaging

**Symptoms:** the application works when started from the IDE or with `mvn exec`, but not as a single
("fat") JAR. Either `build()` fails with
```
JAVA_SERVICE_CONFIGURATION_ERROR: mConfig did not find all of its components, likely due to faulty JAR packaging, ...
```
or values from bundled defaults or from files are missing (`NO_MATCHING_ENTRY`), without any warning.
The log line `mConfig initialised, ... storages [...]` lacks storages you expect, e.g. `JAR` or `files`.

**Cause:** the JAR was built by unpacking all dependency JARs into one, without merging their
`META-INF/services` files, e.g. with the `jar-with-dependencies` descriptor of the Maven assembly plugin.
Only one file per service interface survives; which one depends on dependency order.

**Fix:** merge the service files when packaging; see
[Packaging as a single JAR](13_getting_started.md#138-packaging-as-a-single-jar) for the Maven assembly,
Maven shade and Gradle configurations. Then test the packaged JAR itself, not only the classes.

This is not specific to mConfig: every library that uses `ServiceLoader` (e.g. SLF4J 2, Log4j 2, JDBC drivers,
Jackson modules) is affected. mConfig detects it when its core registrations are among the lost ones and then
fails `build()` with the message above; in other cases nothing reports it, for mConfig or for the other libraries.

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
2. Use naming schema: `<application>_<configName>_<key>`, **UPPER CASE**, e.g. `MYAPP_NETWORK_PEER`.
   Within the key, `/` becomes `_` and a literal `_` becomes `__`.
   This is the rule of the Elasticsearch (`ES_SETTING_...`) and Confluent Platform (Kafka) Docker images, too.
   It is close to MicroProfile Config / Quarkus and Grafana: these also separate with `_`, but do not
   double a literal `_`, so the names are the same only for keys without an underscore.
   It is the reverse of the .NET and Airflow custom, though, where `__` separates the levels:
   here `MYAPP_NETWORK_DB__HOST` is the key `db_host`, and `MYAPP_NETWORK_DB_HOST` is `db/host`.
   Up to 0.8.16 the underscore was not doubled; a WARNING names a variable still in the old naming.
   On Linux and macOS env var names are case-sensitive, so a lowercase name is not found.
3. Scope: `SESSION` (overrides USER).
4. Verify via `mConfigTool` or enable logging.
See [mConfigSourceEnvVar README](https://github.com/meta-bit/mconfig/blob/master/mConfigSourceEnvVar/README.md).
