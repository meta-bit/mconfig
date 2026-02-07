# Testing Configurations

This example shows how to enable Test Mode and point mConfig at test-specific directories.

## 1. Enable Test Mode in code
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setTestMode(true)
        .build())
    {
    Configuration cfg = factory.getConfiguration("network");
    // ... test using cfg ...
    }
```

## 2. Provide test-only config files
Place test configurations in your test resources:
- `src/test/resources/.config/ACME/ourApp/network.properties`

These take precedence in Test Mode, keeping production defaults intact.

## 3. JUnit example
```java
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

class NetworkConfigTest {

    @Test
    void loadsTestConfigDefaults() throws Exception {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp")
                .setTestMode(true)
                .build())
            {
            Configuration cfg = factory.getConfiguration("network");

            assertEquals("127.0.0.1", cfg.getString("server/host"));
            assertEquals(8080, cfg.getInteger("server/port"));
            }
    }
}
```

## 4. Override test directories explicitly (optional)
If you want to point to custom test directories, use `TESTMODE_DIRECTORIES`:
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp")
        .setTestMode(true)
        .setFeature(ConfigFeature.TESTMODE_DIRECTORIES,
            List.of("USER:/tmp/mconfig-tests/user", "APPLICATION:/tmp/mconfig-tests/app"))
        .build())
    {
    Configuration cfg = factory.getConfiguration("network");
    // ...
    }
```

## 5. Security note
Test Mode can be disabled globally with `ConfigFactoryBuilder.forbidTestMode()`.
If you do that, `TEST_MODE` and `setTestMode(true)` have no effect.

Related docs:
- [Test Mode](../15_test_mode.md)
- [Configuration Schemes](../23_configuration_schemes.md)
