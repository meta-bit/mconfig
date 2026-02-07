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

`ConfigFactoryBuilder.setTestMode(true);`

or with Environment Variables, e.g.
`MCONFIG_RUNTIME_TEST_MODE=true`

(requirements for Environment Variables to have effect: 
1. EnvVar source module present, 
2. ALLOW_RUNTIME_FLAG set to true in the ConfigFactoryBuilder.
)

#### Effects of Test Mode

this will

- disable lookup in the regular file paths
- instead, use "./src/test/config" and "./src/test/resources/config" paths
  relative to current working directory as base paths.
  Resource-based configurations in `src/test/resources/config/{SCOPE}` 
  take precedence over project-level test configurations in `src/test/config`.

  This is based on the standard maven test run setup,
  where the tests are run from one level above the src directory.
- disable lookup in default network environments, if network config sources are active
- enable lookup in test network environments, if available/configured.
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

In test mode, standard OS paths are bypassed. Use explicit paths via `ConfigFactoryBuilder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:/tmp/test-user"))` **or** fallbacks like `src/test/resources/.config/&lt;company&gt;/&lt;app&gt;/` and `src/test/resources/config/{SCOPE}` (resources > project configs).

| Scope        | Path                                                                      | Notes   |
|--------------|---------------------------------------------------------------------------|---------|
| POLICY       | TESTMODE_DIRECTORIES "POLICY:/path" or src/test/resources/config/POLICY   | Highest |
| RUNTIME      | src/test/resources/config/RUNTIME                                         |         |
| SESSION      | src/test/resources/config/SESSION                                         |         |
| USER         | src/test/resources/config/USER                                            |         |
| APPLICATION  | src/test/config/&lt;company&gt;/&lt;app&gt;/ or src/test/resources/config/APPLICATION |         |
| HOST         | src/test/resources/config/HOST                                            |         |
| CLUSTER      | src/test/resources/config/CLUSTER                                         |         |
| CLOUD        | src/test/resources/config/CLOUD                                           |         |
| ORGANIZATION | src/test/resources/config/ORGANIZATION                                    |         |
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

### Set your own paths (legacy API for some test environments)

ConfigFactoryBuilder.setTestConfigPaths() allows you to set multiple paths for 
each scope, to be used in test mode. Deprecated; use the custom test directories above.

```java

@BeforeAll
void setUpTestEnvironment()
    {
    List<String> paths = List.of("/my/local/fixed/config/environment");
    ConfigFactoryBuilder.setTestMode(true); // activate and set defaults
    ConfigFactoryBuilder.setTestFilePaths(paths); // replace
    ConfigFactoryBuilder.setTestJARURIs(null); // turn off
    }
```

**Deprecated legacy API**: Paths added at `RUNTIME` scope.

## Security Considerations

To prevent accidental or malicious activation of test mode in production environments,
mConfig employs a two-tier security gate system.

### 1.5.1 Global Static Gate (`forbidTestMode()` / `permitTestMode()`)

This is a JVM-wide master switch. Calling `ConfigFactoryBuilder.forbidTestMode()` 
ensures that no code within the JVM can activate `TEST_MODE`, 
regardless of other settings. This is intended for production environments 
to provide a hard security boundary.

### 1.5.2 Instance Dynamic Gate (`PERMIT_TEST_MODE` Feature)

This is a per-builder configuration flag. It acts as a final check 
before an instance activates test mode.

#### "Productive by Default"
To simplify development, the `PERMIT_TEST_MODE` feature defaults to `true`. 
This allows developers to activate `TEST_MODE` using a single flag 
(via environment variables, system properties, or `setFeature`) 
without needing to explicitly enable permissions in standard environments.

#### "Test Mode Proofing" (Lockdown)
Despite with the default being `true`, `PERMIT_TEST_MODE` remains an useful security tool
for some situations.
In a complex process where `TEST_MODE` might be enabled globally
(e.g., via `-DTEST_MODE=true`) to facilitate integration testing of some modules, a critical production-facing `ConfigFactory` can be locked down:

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

Test mode is only enabled if **both** the global gate and the instance gate allow it. The following logic is enforced during the `build()` process:

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

mConfig's self-configuration feature (loading `mconfig.properties` from the classpath) is another area where security can be tightened. If you don't want the library to automatically configure itself from classpath resources, you can disable it:

```java
ConfigFactoryBuilder.create("metabit", "APP")
    .setFeature(ConfigFeature.ENABLE_SELF_CONFIGURATION, false)
    .build();
```

This is particularly important in security-sensitive applications to ensure that no unexpected features (like `ALLOW_MCONFIG_RUNTIME_SETTINGS` or `TEST_MODE`) are enabled by a JAR file placed on the classpath.
