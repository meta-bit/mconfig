# 1.5 Test Mode

mConfig has a test mode. This is to support software using mConfig
in performing automated tests.

Test mode supports:
- manual tests by individual developers,
- unit and integration testing,
- the CI/CD pipeline test environment.

One of the problems with tests is to replace the real, runtime environment
with the test environment – preferably with as little residue in the actual
code as possible.

One of the solution approaches is to supply the code with different configurations,
depending on whether it is run in test mode, or regular runtime mode.

## Goals

We want to allow testing configurations without modifying the actual code.

The simple, straightforward way would be to provide `ConfigFactoryBuilder` instances
to your code, set up differently depending on whether testing or not. But that's
breaking encapsulation, and cumbersome.

Instead, we provide a way to change the `ConfigFactoryBuilder` behaviour for the
entire VM - "static"-like. This way, all instances in the respective JVM
are turned to test mode or production mode, including the paths and settings
you also specify.

This way, your test code can set the test environment in a @BeforeAll/@BeforeEach
function before the tests start, while tests and **code stay free of test-specific
modifications** in regard to mConfig.

## Use

### Activate Test Mode

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("company", "app");
builder.setTestMode(true);
```

or from outside the program, without changing its code: environment variable
`MCONFIG_RUNTIME_TEST_MODE=true`, or system property `-Dmconfig.runtime.test_mode=true`.
This switches an unchanged binary into test mode, e.g. in a CI/CD pipeline.

Requirements for this to have effect:
1. the application set `ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS` to true in code
   (it delegates the decision to whoever starts the process; it cannot be enabled from outside);
2. test mode is permitted: `PERMIT_TEST_MODE` is not false, and `ConfigFactoryBuilder.forbidTestMode()` was not called.

A component can also switch on its own test mode in its self-configuration file
`.config/<company>/<application>/mconfig.properties` (`TEST_MODE=true`), typically in `src/test/resources`.
This is logged as a WARNING with the file's location, so a file accidentally shipped in `src/main/resources`
is visible at once. It is subject to the same gates. The deprecated global file
`.config/metabit/mConfig/mconfig.properties` cannot set test mode.
See [Who may set a feature](25_config_features.md#who-may-set-a-feature).

#### Effects of Test Mode

this will

- disable lookup in the regular file paths
- instead, use "./src/test/config" and "./src/test/resources/config" paths
  relative to current working directory as base paths.
  Resource-based configurations in `src/test/resources/config/{SCOPE}` 
  take precedence over project-level test configurations in `src/test/config`.

  This is based on the standard maven test run setup,
  where the tests are run from one level above the src directory.
- disable lookup in default network environments, if network config sources are active. For example, `mConfigSourceKubernetes` will automatically disable its API client when `TEST_MODE` is active to prevent accidental cluster communication.
- enable lookup in test network environments, if available/configured (e.g., using `BYPASS_TEST` and mock endpoints). 
  
  **Testing Networked Modules:**
  Modules that rely on external services (Kubernetes, Vault, AWS) can be tested by providing mock endpoints via the `mConfig` hierarchy. Instead of hardcoded values, these modules use `BootstrapConfigContext` to resolve their own settings (like `API_ENDPOINT`) from the standard 10-tier hierarchy.
  
  The module reads its settings from the application's configuration named `kubernetes`. It is an
  ordinary mConfig configuration of the application, resolved through the usual layers and storages within
  the scopes the module reads: bundled defaults, host- or user-wide files, other storages, and values the
  application stores there itself. It is *not* read from mConfig's internal `.config/metabit/mConfig/`
  namespace. In tests, one convenient option is a test resource,
  e.g. `src/test/resources/.config/<company>/<application>/kubernetes.properties`:
  
  ```properties
  # Mock API server for tests
  KUBERNETES_API_ENDPOINT=http://localhost:8080
  # send the ServiceAccount token to the http mock server
  KUBERNETES_ALLOW_INSECURE_TRANSPORT=true
  ```
  
  To let the module run in `TEST_MODE` at all, set the feature `KUBERNETES_BYPASS_TEST=true`, in code or in
  the component's `src/test/resources/.config/<company>/<application>/mconfig.properties`.
  
  Alternatively, use environment variables (see [Runtime Overrides](#runtime-overrides) below).

- switch JAR lookup from regular resources folder to test resources.
  Instead of `src/main/resources/config` from source,
  in test mode `src/test/resources/config` becomes the starting point
  for JAR-file resolution.

- NB: JARs rarely compile in test resources! The JAR aspect takes effect 
  only if you are building test JARs in addition to the regular ones.

#### Paths for Test Mode    

Below the src/test/resources/config/ directory, we check for subdirectories with
the scope names in all-caps, e.g. PRODUCT, APPLICATION, USER, to allow
for placement of respective contents.

In the other case, we go test/.config/*COMPANY*/*PRODUCTNAME*, to mirror such entries
for production.

So, activating test mode results in these directories searched for entries:

##### Test mode search paths

In test mode, standard OS paths are bypassed. Use explicit paths via `ConfigFactoryBuilder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Collections.singletonList("USER:/tmp/test-user"))` **or** fallbacks like `src/test/resources/.config/&lt;company&gt;/&lt;app&gt;/` and `src/test/resources/config/{SCOPE}` (resources > project configs).

| Scope        | Path                                                                                  | Notes   |
|--------------|---------------------------------------------------------------------------------------|---------|
| POLICY       | TESTMODE_DIRECTORIES "POLICY:/path" or src/test/resources/config/POLICY               | Highest |
| RUNTIME      | src/test/resources/config/RUNTIME                                                     |         |
| SESSION      | src/test/resources/config/SESSION                                                     |         |
| USER         | src/test/resources/config/USER                                                        |         |
| APPLICATION  | src/test/config/&lt;company&gt;/&lt;app&gt;/ or src/test/resources/config/APPLICATION |         |
| HOST         | src/test/resources/config/HOST                                                        |         |
| CLUSTER      | src/test/resources/config/CLUSTER                                                     |         |
| CLOUD        | src/test/resources/config/CLOUD                                                       |         |
| ORGANIZATION | src/test/resources/config/ORGANIZATION                                                |         |
| PRODUCT      | src/main/resources/.config/&lt;company&gt;/&lt;app&gt;/                               | Lowest  |

NB: "COMPANY" and "APP" placeholders replaced by code values. Scope dirs verbatim UPPERCASE. Resources override project configs; scopes follow enum priority (higher scopes override lower).

### Custom test directories

You can provide a list of directories for `TESTMODE_DIRECTORIES` with their respective scope.

Format: `SCOPENAME ":" PATH`
e.g. `USER:/~developer/mylocaltest` or `APPLICATION:/tmp/generatedautomatedtestingdir`.

Multiple entries per scope are possible, and used in order.
If the scope name is omitted or invalid, it defaults to `RUNTIME` scope.

**Note**:
In `TEST_MODE`, `ADDITIONAL_RUNTIME_DIRECTORIES` and `ADDITIONAL_USER_DIRECTORIES` 
are intentionally ignored to prevent tests from accidentally interacting with real production data.
Only `TESTMODE_DIRECTORIES` and the default test resource locations are used.

### Set your own paths

The `ConfigFactoryBuilder.setTestConfigPaths()` method allows you to set multiple paths for 
each scope to be used in test mode.

```java
@BeforeAll
static void setUpTestEnvironment() throws ConfigCheckedException
    {
    ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("myCompany", "myApp");
    builder.setTestMode(true);
    
    // Set specific test paths for USER scope
    builder.setTestConfigPaths(ConfigScope.USER, Collections.singletonList("/my/local/fixed/config/environment"));
    
    // The resulting factory will use these paths
    ConfigFactory factory = builder.build();
    }
```


## Automated Test Directory Management

The recommended way to handle configuration directories in tests is to use
**JUnit 5's `@TempDir`** annotation (or the corresponding feature of other test frameworks)
combined with the `CREATE_MISSING_PATHS` feature. This ensures that:
1. Tests run in isolation.
2. Directories are created only for the duration of the test.
3. The entire directory tree is automatically deleted by the test runner after the test completes.

When `CREATE_MISSING_PATHS` is active, the library will automatically include designated test directories in the search list even if they do not exist yet, and create them upon the first write.

### Example Implementation:
```java
class MyConfigTest {
    @TempDir
    Path tempDir; // JUnit 5 automatically creates and cleans this up

    @Test
    void testWithAutoCreatedPaths() throws ConfigCheckedException {
        // NB: setTestMode() and setTestConfigPaths() return void and cannot be chained;
        // only setFeature() returns the builder.
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("myCompany", "myApp");
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.CREATE_MISSING_PATHS, true);
        builder.setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true);
        // Point a specific scope to our temp directory
        builder.setTestConfigPaths(ConfigScope.USER, Collections.singletonList(tempDir.toString()));

        try (ConfigFactory configFactory = builder.build()) {
            Configuration cfg = configFactory.getConfig("app-settings");
            // This will create the file AND any missing parent directories inside tempDir
            cfg.put("key", "value", ConfigScope.USER);
            cfg.flush();
        }

        // No manual cleanup required; @TempDir handles it.
    }
}
```

## Security Considerations

To prevent accidental or malicious activation of test mode in production environments,
mConfig employs a two-tier security gate system.

### 1.5.1 Global Static Gate (`forbidTestMode()` / `permitTestMode()`)

This is a JVM-wide master switch. Calling `ConfigFactoryBuilder.forbidTestMode()` 
ensures that no code within the JVM can activate `TEST_MODE`, 
regardless of other settings. This is intended for production environments 
to provide a hard security boundary. It is meant for the **main application**: each component using
mConfig is responsible for its own test mode, and the main application switches all of them off at once.

### 1.5.2 Instance Dynamic Gate (`PERMIT_TEST_MODE` Feature)

This is a per-builder configuration flag. It acts as a final check 
before an instance activates test mode.

#### "Productive by Default"
To simplify development, the `PERMIT_TEST_MODE` feature defaults to `true`. 
This allows developers to activate `TEST_MODE` using a single flag 
(via `setFeature`, via runtime settings if the application enabled them, or in the component's own
self-configuration file) without needing to explicitly enable permissions in standard environments.
Code wins over files, so a `PERMIT_TEST_MODE=false` set in code cannot be undone by a file.

#### "Test Mode Proofing" (Lockdown)
Despite with the default being `true`, `PERMIT_TEST_MODE` remains an useful security tool
for some situations.
In a complex process where `TEST_MODE` might be enabled globally
(e.g., via `-Dmconfig.runtime.test_mode=true`) to facilitate integration testing of some modules, a critical production-facing `ConfigFactory` can be locked down:

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "CRITICAL_APP")
    .setFeature(ConfigFeature.PERMIT_TEST_MODE, false) // Lockdown this instance
    .build())
    {
    // ...
    }
```
Even if `TEST_MODE` is requested externally, this specific instance will remain in production mode, ensuring it never reads from `src/test/resources` or other untrusted test locations,
like some dynamically generated test directories and -files.

### Activation Logic

Test mode is only enabled if **both** the global gate and the instance gate allow it. Self-configuration
files are read before this check, so the gates cover file-set test mode too. The following logic is enforced
during the `build()` process:

```java
// Test mode is only enabled if BOTH the global gate AND the instance gate allow it.
if (testModePermitted && configFactorySettings.getBoolean(PERMIT_TEST_MODE)) 
{
    // Activation occurs if either the global trigger or instance trigger is set.
    if (testModeActive || configFactorySettings.getBoolean(TEST_MODE)) 
    {
        configFactorySettings.setBoolean(TEST_MODE, true);
    }
}
else 
{
    // If either permission gate is closed, test mode is forced to false.
    configFactorySettings.setBoolean(TEST_MODE, false);
}
```

### 1.5.3 Self-Configuration Security

mConfig's self-configuration feature (loading `mconfig.properties` from the classpath) is another area where security can be tightened. If you don't want the library to automatically configure itself from classpath resources, you can disable that:

```java
ConfigFactoryBuilder.create("metabit", "APP")
    .setFeature(ConfigFeature.ENABLE_SELF_CONFIGURATION, false)
    .build();
```

Self-configuration is per component: `.config/<company>/<application>/mconfig.properties` applies only to
factories for that company and application, and settings made in code always win over it. Features such as
`ALLOW_MCONFIG_RUNTIME_SETTINGS` or `ALLOW_APPLICATION_POLICY_LOCATIONS` can only be set in code; the
deprecated global file `.config/metabit/mConfig/mconfig.properties` cannot set any test-mode feature either.
See [Who may set a feature](25_config_features.md#who-may-set-a-feature).
Disabling self-configuration keeps all classpath settings out, which is useful if the classpath is not "closed".

## Runtime Overrides

mConfig supports several library-wide and module-specific settings via environment variables
(and the equivalent `-Dmconfig.runtime.*` system properties). They are processed early during the
`ConfigFactoryBuilder.build()` phase, and **only if the application set
`ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS` to true in code**. That switch cannot be set from outside,
neither by an environment variable nor by a classpath resource.

### Core Overrides

| Environment Variable | Description |
| :--- | :--- |
| `MCONFIG_RUNTIME_TEST_MODE` | Activates [Test Mode](#activate-test-mode) (if permitted). |
| `MCONFIG_RUNTIME_DEBUG_LEVEL` | Sets the internal library logging level (`off`, `error`, `warn`, `info`, `debug`, `trace`, or a number). |
| `MCONFIG_RUNTIME_DEBUG_OUTPUT` | Redirects internal logging (`stderr`, `stdout`, `quiet`, `file:<path>`). |
| `MCONFIG_RUNTIME_DISABLE_MODULES` | Comma-separated module IDs to disable. |

`MCONFIG_RUNTIME_MODULES` (`ADDITIONAL_MODULE_PATHS`) is refused since 0.8.15: it loads code, so only code may set it.

### Module Overrides (Kubernetes)

These overrides are specifically useful for configuring the `mConfigSourceKubernetes` and `mConfigSourceKubernetesFabric8` modules without hardcoded values.

| Environment Variable | Description |
| :--- | :--- |
| `MCONFIG_RUNTIME_KUBERNETES_API_ENDPOINT` | The URL of the Kubernetes API server. |
| `MCONFIG_RUNTIME_KUBERNETES_CONFIG_DIR` | Colon-separated list of paths for ConfigMap mounts. |
| `MCONFIG_RUNTIME_KUBERNETES_SECRETS_DIR` | Colon-separated list of paths for Secret mounts. |
| `MCONFIG_RUNTIME_KUBERNETES_SA_TOKEN_PATH` | Path to the ServiceAccount token file. |
| `MCONFIG_RUNTIME_KUBERNETES_SA_NAMESPACE_PATH` | Path to the ServiceAccount namespace file. |
| `MCONFIG_RUNTIME_KUBERNETES_SA_CA_CERT_PATH` | Path to the CA certificate for API validation. |
