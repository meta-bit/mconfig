package org.metabit.platform.support.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.EnumSet;

// tests MLIB-65 implementation
public class LimitScopesTest {
    @TempDir
    Path tempBase;

    @Test
    public void testLimitScopes() throws Exception {
        ConfigFactoryBuilder.permitTestMode();
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "limit-test");
        builder.setTestMode(true);
        try (ConfigFactory factory = builder.build()) {
            Configuration config = factory.getConfig("limit-scopes");

            config.put("test.key", "session-value", ConfigScope.SESSION);

            // Default all scopes
            assertEquals("session-value", config.getString("test.key"));

            // Limit to SESSION
            config.limitScopes(EnumSet.of(ConfigScope.SESSION));
            assertEquals("session-value", config.getString("test.key"));

            // No scopes (throws due to exceptionOnNullFlag)
            config.limitScopes(EnumSet.noneOf(ConfigScope.class));
            assertThrows(ConfigException.class, () -> config.getString("test.key"));
        }
    }
}