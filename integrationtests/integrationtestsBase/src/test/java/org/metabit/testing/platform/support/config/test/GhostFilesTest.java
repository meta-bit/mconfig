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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class GhostFilesTest
{
    private static final String COMPANY_NAME = "metabit";
    private static final String APPLICATION_NAME = "GhostFilesTest";
    private static final String CONFIG_NAME = "network";

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException
    {
        tempDir = Files.createTempDirectory("mConfigGhostFilesTest");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        ConfigIOUtil.deleteDirectoryWithContents(tempDir);
    }

    @Test
    void testIgnoreGhostFilesDiscovery() throws IOException
    {
        // Create valid config
        Files.write(tempDir.resolve("valid.properties"), List.of("key=value"));
        
        // Create ghost files
        Files.write(tempDir.resolve("ghost1.properties~"), List.of("key=ghost"));
        Files.write(tempDir.resolve("ghost2.properties.bak"), List.of("key=ghost"));
        Files.write(tempDir.resolve("ghost3.properties.swp"), List.of("key=ghost"));
        Files.write(tempDir.resolve("ghost4.properties.tmp"), List.of("key=ghost"));
        Files.write(tempDir.resolve("Thumbs.db"), List.of("garbage"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Set<ConfigDiscoveryInfo> configs = factory.listAvailableConfigurations();
            Set<String> names = configs.stream().map(ConfigDiscoveryInfo::getConfigName).collect(Collectors.toSet());
            
            assertTrue(names.contains("valid"), "Should contain valid config");
            assertFalse(names.contains("ghost1.properties"), "Should ignore ~ files");
            assertFalse(names.contains("ghost2.properties"), "Should ignore .bak files");
            assertFalse(names.contains("ghost3.properties"), "Should ignore .swp files");
            assertFalse(names.contains("ghost4.properties"), "Should ignore .tmp files");
        }
    }

    @Test
    void testIgnoreGhostFilesDotD() throws IOException, ConfigCheckedException
    {
        // Create main config file
        Path mainFile = tempDir.resolve(CONFIG_NAME + ".properties");
        Files.write(mainFile, List.of("key=main"));

        // Create .d directory
        Path dotDDir = tempDir.resolve(CONFIG_NAME + ".d");
        Files.createDirectory(dotDDir);

        // Create valid fragment
        Files.write(dotDDir.resolve("01-valid.properties"), List.of("key=valid"));
        
        // Create ghost fragments
        Files.write(dotDDir.resolve("02-ghost.properties~"), List.of("key=ghost"));
        Files.write(dotDDir.resolve("03-ghost.properties.bak"), List.of("key=ghost"));
        Files.write(dotDDir.resolve("Thumbs.db"), List.of("garbage"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("valid", cfg.getString("key"), "Ghost files in .d should be ignored and not override valid fragments");
        }
    }
}
