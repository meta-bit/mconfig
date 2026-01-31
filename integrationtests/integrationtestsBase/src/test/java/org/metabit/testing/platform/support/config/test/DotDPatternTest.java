package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.util.ConfigIOUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DotDPatternTest
{
    private static final String COMPANY_NAME = "metabit";
    private static final String APPLICATION_NAME = "DotDTest";
    private static final String CONFIG_NAME = "network";

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException
    {
        tempDir = Files.createTempDirectory("mConfigDotDTest");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        ConfigIOUtil.deleteDirectoryWithContents(tempDir);
    }

    @Test
    void testDotDPatternOverrides() throws IOException, ConfigCheckedException
    {
        // Create main config file
        Path mainFile = tempDir.resolve(CONFIG_NAME + ".properties");
        Files.write(mainFile, List.of("key1=main", "key2=main"));

        // Create .d directory
        Path dotDDir = tempDir.resolve(CONFIG_NAME + ".d");
        Files.createDirectory(dotDDir);

        // Create fragments
        Files.write(dotDDir.resolve("00-default.properties"), List.of("key2=default", "key3=default"));
        Files.write(dotDDir.resolve("01-custom.properties"), List.of("key3=custom", "key4=custom"));
        Files.write(dotDDir.resolve("99-override.properties"), List.of("key1=override", "key4=override"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);

            // key1: main (lowest) -> 99-override (highest) => override
            assertEquals("override", cfg.getString("key1"));
            // key2: main -> 00-default => default
            assertEquals("default", cfg.getString("key2"));
            // key3: 00-default -> 01-custom => custom
            assertEquals("custom", cfg.getString("key3"));
            // key4: 01-custom -> 99-override => override
            assertEquals("override", cfg.getString("key4"));
        }
    }

    @Test
    void testOnlyDotD() throws IOException, ConfigCheckedException
    {
        // Create .d directory
        Path dotDDir = tempDir.resolve(CONFIG_NAME + ".d");
        Files.createDirectory(dotDDir);

        // Create fragments
        Files.write(dotDDir.resolve("01-custom.properties"), List.of("key1=custom"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("custom", cfg.getString("key1"));
        }
    }

    @Test
    void testMultipleFormatsInDotD() throws IOException, ConfigCheckedException
    {
        // Create .d directory
        Path dotDDir = tempDir.resolve(CONFIG_NAME + ".d");
        Files.createDirectory(dotDDir);

        // 01-first.properties: key1=first
        Files.write(dotDDir.resolve("01-first.properties"), List.of("key1=first"));
        
        // 02-second.txt: treated as Raw Text if format available.
        // If Jackson YAML is available, we could test .yaml.
        // Let's see what formats we have.
        Files.write(dotDDir.resolve("02-second.properties"), List.of("key1=second"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("second", cfg.getString("key1"));
        }
    }

    @Test
    void testDiscoveryWithDotD() throws IOException
    {
        // Create .d directory
        Path dotDDir = tempDir.resolve(CONFIG_NAME + ".d");
        Files.createDirectory(dotDDir);

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            java.util.Set<ConfigDiscoveryInfo> configs = factory.listAvailableConfigurations();
            boolean found = false;
            for (ConfigDiscoveryInfo info : configs)
            {
                if (CONFIG_NAME.equals(info.getConfigName()))
                {
                    found = true;
                    break;
                }
            }
            // "network" should be discovered because of "network.d"
            assertEquals(true, found, "Config " + CONFIG_NAME + " should be discovered via .d directory");
        }
    }
}
