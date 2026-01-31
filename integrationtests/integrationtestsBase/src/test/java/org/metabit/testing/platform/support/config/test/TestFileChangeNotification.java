package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TestFileChangeNotification
{
    public static final String COMPANY_NAME = "metabit";
    public static final String APPLICATION_NAME = "mConfigNotificationTest";
    private static final String CONFIG_NAME = "notiftest";
    private static Path tempDir;
    private static Path tempCfgFile;

    @BeforeAll
    static void init() throws IOException
    {
        tempDir = Files.createTempDirectory("mConfigNotifTest_");
        tempCfgFile = tempDir.resolve(CONFIG_NAME + ".properties");
        Files.write(tempCfgFile, "key=initial".getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    static void tearDown() throws IOException
    {
        if (tempCfgFile != null) Files.deleteIfExists(tempCfgFile);
        if (tempDir != null) Files.deleteIfExists(tempDir);
    }

    @Test
    void testFileChangeTriggersNotification() throws Exception
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, List.of(tempDir.toAbsolutePath().toString()))
                .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100) // fast check
                .build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("initial", cfg.getString("key"));

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<ConfigLocation> changedLocation = new AtomicReference<>();

            cfg.subscribeToUpdates(location ->
                {
                System.out.println("[DEBUG_LOG] Received notification for location: " + location);
                changedLocation.set(location);
                latch.countDown();
                });

            // Trigger change
            Thread.sleep(200); // Wait a bit to ensure watcher is started
            Files.write(tempCfgFile, "key=updated".getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG_LOG] Updated file: " + tempCfgFile);

            boolean received = latch.await(5, TimeUnit.SECONDS);
            assertTrue(received, "Notification not received within timeout");
            assertNotNull(changedLocation.get());
            
            // Should be able to read updated value (might need a bit of time for re-reading if not automatic, but here we just check notification)
            // Note: mConfig might need some time to re-read or we might need to clear cache if it doesn't do it automatically on notification.
            // But the primary goal is to verify coordination between FileChangeWatcher and SourceChangeNotifier.
        }
    }
}
