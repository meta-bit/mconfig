package org.metabit.platform.support.config.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.ConfigCheckedException;

import java.nio.file.Path;
import java.util.Collections;

/**
 * Example demonstrating the recommended way to handle configuration directories in TEST_MODE
 * using JUnit 5's @TempDir for automatic creation and cleanup.
 */
class JUnit5TestModeExample {
    @TempDir
    Path tempDir; // JUnit 5 automatically creates and cleans this up

    @Test
    void testWithAutoCreatedPaths() throws ConfigCheckedException {
        // Note: setTestMode() and setTestConfigPaths() return void, so they cannot be
        // chained. Only setFeature() returns the builder.
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
