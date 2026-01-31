package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for ConfigScopeHierarchy functionality.
 * As an integration test, it tests with real (temporary) directories,
 * not TEST_MODE directories.
 */
class HierarchyIntegrationTest
{

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Ensure APPLICATION scope takes precedence over PRODUCT scope")
    void testApplicationOverProduct()
            throws IOException, ConfigCheckedException
        {
        // 1. Create a "Product" default file
        Path productDir = tempDir.resolve("product");
        Path appDir = tempDir.resolve("app");
        Files.createDirectories(productDir);
        Files.createDirectories(appDir);

        Files.writeString(productDir.resolve("config.properties"), "scopeTest=PRODUCT\nonlyProduct=true");
        Files.writeString(appDir.resolve("config.properties"), "scopeTest=APPLICATION");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "TEST");
        // Forcing a specific search order to test if scope logic overrides index order
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES,
                Arrays.asList("APPLICATION:" + appDir.toString(), "PRODUCT:" + productDir.toString()));

        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("config");

        // Even though productDir might be added last or first,
        // if the system correctly identifies APPLICATION scope, it should win.
        // (Note: This depends on how the factory assigns scopes to ADDITIONAL_RUNTIME_DIRECTORIES)
        assertNotNull(cfg.getString("scopeTest"));

        // If the hierarchy works:
        // 'target' should be 'APPLICATION' because it's higher than 'PRODUCT'
        // 'onlyProduct' should be visible (fallback)

        // Note: The DefaultConfigFactory assigns scopes based on the ConfigLocation.
        // In this test environment, we'll verify the result of the merge.
        String result = cfg.getString("scopeTest");

        // This validates the precedence logic inside ConfigFacadeImpl/LayeredConfiguration
        // where higher ordinal Scopes (APPLICATION > PRODUCT) win.
        assertEquals("APPLICATION", cfg.getString("scopeTest"),
                "APPLICATION scope should obscure PRODUCT scope");
        assertEquals("true", cfg.getString("onlyProduct"));
        }


    @Test
    @DisplayName("Precedence: Newest entry in same scope wins")
    void testNewestEntrySameScopeWins()
            throws IOException, ConfigCheckedException
        {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        // Files with the same name but different values
        Files.writeString(dir1.resolve("multi.properties"), "val=old");
        Files.writeString(dir2.resolve("multi.properties"), "val=new");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "PRIORITY_TEST");
        // Use production-time features instead of TEST_MODE for integration test
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES,
                Arrays.asList("RUNTIME:" + dir1.toString(), "RUNTIME:" + dir2.toString()));

        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("multi");

        assertEquals("new", cfg.getString("val"), "The later addition in the search list should obscure the earlier one.");
        }
}