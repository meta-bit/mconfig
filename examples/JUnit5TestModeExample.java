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
        ConfigFactory configFactory = ConfigFactoryBuilder.create("myCompany", "myApp")
            .setTestMode(true)
            .setFeature(ConfigFeature.CREATE_MISSING_PATHS, true)
            // Point a specific scope to our temp directory
            .setTestConfigPaths(ConfigScope.USER, Collections.singletonList(tempDir.toString()))
            .build();

        Configuration cfg = configFactory.getConfig("app-settings");
        // This will create the file AND any missing parent directories inside tempDir
        cfg.set("key", "value");

        // No manual cleanup required; @TempDir handles it.
    }
}
