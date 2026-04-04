package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that non-existent paths are kept in the search list and 
 * discovery works when they appear later.
 */
class NonExistentPathDiscoveryTest {

    private static final String COMPANY_NAME = "ACME";
    private static final String APPLICATION_NAME = "demoApp";
    private static final String CONFIG_NAME = "network";

    @TempDir
    Path tempDir;

    @Test
    void testDiscoveryInNewlyCreatedDirectory() throws Exception {
        // 1. Set up a non-existent directory path
        Path nonExistentDir = tempDir.resolve("missing_configs");
        // Ensure it doesn't exist
        assertFalse(Files.exists(nonExistentDir));

        // 2. Build ConfigFactory with this non-existent path
        ConfigFactoryBuilder.permitTestMode();
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + nonExistentDir.toAbsolutePath().toString()))
                .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100)
                .build()) {

            CountDownLatch latch = new CountDownLatch(1);
            // Trigger discovery once to register the watcher on the non-existent path
            Configuration cfgBefore = factory.getConfig(CONFIG_NAME);
            
            cfgBefore.subscribeToUpdates(location -> {
                System.out.println("[DEBUG_LOG] Received update for: " + location);
                latch.countDown();
            });

            // Trigger discovery of missing_configs by checking its presence in search list
            List<ConfigLocation> searchList = factory.getSearchList();
            boolean foundInSearchList = false;
            for (ConfigLocation loc : searchList) {
                if (loc.toString().contains("missing_configs")) {
                    foundInSearchList = true;
                }
            }
            assertTrue(foundInSearchList, "The non-existent path should be present in the search list");

            try {
                cfgBefore.getString("new_key");
            } catch (ConfigException e) {
                // Expected
            }

            // 4. Create the directory and the config file
            Files.createDirectories(nonExistentDir);
            Path configFile = nonExistentDir.resolve(CONFIG_NAME + ".properties");
            Files.write(configFile, "new_key=discovered_value".getBytes(StandardCharsets.UTF_8));

            // 5. Wait for the discovery to happen (WatchService may take a second)
            // We use a longer timeout and debug logging
            boolean updated = latch.await(10, TimeUnit.SECONDS);
            System.out.println("[DEBUG_LOG] Update received: " + updated);
            
            // Re-read configuration
            Configuration cfgAfter = factory.getConfig(CONFIG_NAME);
            String value = cfgAfter.getString("new_key");
            System.out.println("[DEBUG_LOG] Value after creation: " + value);
            
            assertEquals("discovered_value", value, "Config should be found in the newly created directory after re-reading");
        }
    }
    @Test
    void testUpdateInExistingFile() throws Exception {
        // 1. Set up a directory with a config file
        Path configDir = tempDir.resolve("existing_configs");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve(CONFIG_NAME + ".properties");
        Files.write(configFile, "initial_key=initial_value".getBytes(StandardCharsets.UTF_8));

        // 2. Build ConfigFactory
        ConfigFactoryBuilder.permitTestMode();
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + configDir.toAbsolutePath().toString()))
                .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100)
                .build()) {

            CountDownLatch latch = new CountDownLatch(1);
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("initial_value", cfg.getString("initial_key"));
            
            cfg.subscribeToUpdates(location -> {
                System.out.println("[DEBUG_LOG] Received update for: " + location);
                latch.countDown();
            });

            // 3. Update the file
            Files.write(configFile, "initial_key=updated_value".getBytes(StandardCharsets.UTF_8));

            // 4. Wait for the discovery to happen
            boolean updated = latch.await(10, TimeUnit.SECONDS);
            assertTrue(updated, "Update notification should be received");
            
            // 5. Verify the value is updated in the SAME configuration object
            assertEquals("updated_value", cfg.getString("initial_key"), "Config should be updated in-place");
        }
    }
}
