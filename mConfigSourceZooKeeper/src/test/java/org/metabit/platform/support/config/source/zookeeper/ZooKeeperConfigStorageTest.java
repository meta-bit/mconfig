package org.metabit.platform.support.config.source.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.*;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ZooKeeperConfigStorageTest
{
    private TestingServer zkServer;
    private CuratorFramework client;

    @BeforeEach
    void setUp() throws Exception
    {
        zkServer = new TestingServer();
        zkServer.start();

        client = CuratorFrameworkFactory.newClient(zkServer.getConnectString(), new ExponentialBackoffRetry(1000, 3));
        client.start();

        // Prepare test data in ZK
        String content = "{\n  \"app\": {\n    \"name\": \"ZKTestApp\",\n    \"version\": \"1.0\"\n  }\n}";
        client.create().creatingParentsIfNeeded().forPath("/mconfig-test/cluster/test-config", content.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws Exception
    {
        if (client != null) client.close();
        if (zkServer != null) zkServer.stop();
    }

    @Test
    void testZooKeeperSource() throws Exception
    {
        // Create a temporary zookeeper.json in a SCOPED directory
        Path tempBaseDir = Files.createTempDirectory("mconfig-zk-test");
        Path userConfigDir = tempBaseDir.resolve("USER");
        Files.createDirectories(userConfigDir);
        
        String zookeeperJson = "{\n" +
                "  \"zookeeper\": {\n" +
                "    \"connectString\": \"" + zkServer.getConnectString() + "\",\n" +
                "    \"rootPath\": \"/mconfig-test\"\n" +
                "  }\n" +
                "}";
        Files.write(userConfigDir.resolve("zookeeper.json"), zookeeperJson.getBytes(StandardCharsets.UTF_8));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test-app");
        builder.setTestMode(true);
        builder.setFeature(org.metabit.platform.support.config.ConfigFeature.EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND, false);
        builder.setTestConfigPaths(org.metabit.platform.support.config.ConfigScope.USER, java.util.Collections.singletonList(userConfigDir.toString()));
        
        try (ConfigFactory factory = builder.build())
        {
            Configuration config = factory.getConfig("test-config");
            assertNotNull(config);
            assertEquals("ZKTestApp", config.getString("app/name"));
            assertEquals("1.0", config.getString("app/version"));
        }
    }

    @Test
    void testZooKeeperRetryConfig() throws Exception
    {
        // Create a temporary zookeeper.json with custom retry settings
        Path tempBaseDir = Files.createTempDirectory("mconfig-zk-retry-test");
        Path userConfigDir = tempBaseDir.resolve("USER");
        Files.createDirectories(userConfigDir);

        String zookeeperJson = "{\n" +
                "  \"zookeeper\": {\n" +
                "    \"connectString\": \"" + zkServer.getConnectString() + "\",\n" +
                "    \"rootPath\": \"/mconfig-test\",\n" +
                "    \"retryBaseSleepMs\": 500,\n" +
                "    \"retryMaxRetries\": 5\n" +
                "  }\n" +
                "}";
        Files.write(userConfigDir.resolve("zookeeper.json"), zookeeperJson.getBytes(StandardCharsets.UTF_8));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test-app");
        builder.setTestMode(true);
        builder.setTestConfigPaths(org.metabit.platform.support.config.ConfigScope.USER, java.util.Collections.singletonList(userConfigDir.toString()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration config = factory.getConfig("test-config");
            assertNotNull(config);
            assertEquals("ZKTestApp", config.getString("app/name"));
        }
    }

    @Test
    void testZooKeeperRetryConfigViaFeatures() throws Exception
    {
        // No bootstrap config, set via builder features
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test-app");
        builder.setTestMode(true);
        builder.setFeature(org.metabit.platform.support.config.ConfigFeature.ZOOKEEPER_CONNECT_STRING, zkServer.getConnectString());
        builder.setFeature(org.metabit.platform.support.config.ConfigFeature.ZOOKEEPER_ROOT_PATH, "/mconfig-test");
        builder.setFeature(org.metabit.platform.support.config.ConfigFeature.ZOOKEEPER_RETRY_BASE_SLEEP_MS, 500);
        builder.setFeature(org.metabit.platform.support.config.ConfigFeature.ZOOKEEPER_RETRY_MAX_RETRIES, 5);

        try (ConfigFactory factory = builder.build())
        {
            Configuration config = factory.getConfig("test-config");
            assertNotNull(config);
            assertEquals("ZKTestApp", config.getString("app/name"));
        }
    }

    @Test
    void testZeroConfigStartup() throws Exception
    {
        // No zookeeper.json provided, no ZK features set.
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test-app");
        builder.setTestMode(true);
        // We ensure no test directories are set to simulate empty environment
        builder.setTestConfigPaths(org.metabit.platform.support.config.ConfigScope.USER, java.util.Collections.emptyList());

        try (ConfigFactory factory = builder.build())
        {
            // Factory should start fine
            assertNotNull(factory);
            
            // Getting a config should still work (it will just be empty or have JAR defaults)
            Configuration config = factory.getConfig("any-config");
            assertNotNull(config);
            assertTrue(config.isEmpty());
        }
    }

    /*
    @Test
    void testZooKeeperUpdatePropagation() throws Exception
    {
        // Create a temporary zookeeper.json in a SCOPED directory
        Path tempBaseDir = Files.createTempDirectory("mconfig-zk-update-test");
        Path userConfigDir = tempBaseDir.resolve("USER");
        Files.createDirectories(userConfigDir);

        String zookeeperJson = "{\n" +
                "  \"zookeeper\": {\n" +
                "    \"connectString\": \"" + zkServer.getConnectString() + "\",\n" +
                "    \"rootPath\": \"/mconfig-test\"\n" +
                "  }\n" +
                "}";
        Files.write(userConfigDir.resolve("zookeeper.json"), zookeeperJson.getBytes(StandardCharsets.UTF_8));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test-app");
        builder.setTestMode(true);
        // Set update check frequency to a low value for faster test
        builder.setFeature(org.metabit.platform.support.config.ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100);
        builder.setTestConfigPaths(org.metabit.platform.support.config.ConfigScope.USER, java.util.Collections.singletonList(userConfigDir.toString()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration config = factory.getConfig("test-config");
            assertNotNull(config);
            assertEquals("ZKTestApp", config.getString("app/name"));

            // Update in ZK
            String newContent = "{\n  \"app\": {\n    \"name\": \"ZKTestAppUpdated\",\n    \"version\": \"1.1\"\n  }\n}";
            long startTime = System.currentTimeMillis();
            client.setData().forPath("/mconfig-test/cluster/test-config", newContent.getBytes(StandardCharsets.UTF_8));

            // Wait for propagation
            boolean updated = false;
            long propagationTime = -1;
            for (int i = 0; i < 80; i++)
            {
                String currentName = config.getString("app/name");
                System.out.println("[DEBUG_LOG] Iteration " + i + ": app/name = " + currentName);
                if ("ZKTestAppUpdated".equals(currentName))
                {
                    propagationTime = System.currentTimeMillis() - startTime;
                    updated = true;
                    break;
                }
                Thread.sleep(100);
            }

            assertTrue(updated, "Configuration should have been updated");
            System.out.println("ZooKeeper propagation took " + propagationTime + " ms");
        }
    }

     */
}
