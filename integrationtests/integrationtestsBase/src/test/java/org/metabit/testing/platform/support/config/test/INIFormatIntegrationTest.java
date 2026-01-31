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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class INIFormatIntegrationTest
{
    private static final String COMPANY_NAME = "metabit";
    private static final String APPLICATION_NAME = "INIIntegrationTest";
    private static final String CONFIG_NAME = "initestconfig";

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException
    {
        tempDir = Files.createTempDirectory("mConfigINITest");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        ConfigIOUtil.deleteDirectoryWithContents(tempDir);
    }

    @Test
    void testINIFormatIntegration() throws Exception
    {
        Path configFile = tempDir.resolve(CONFIG_NAME + ".ini");
        Files.write(configFile, List.of(
            "global=value1",
            "[section1]",
            "key1=value2",
            "[section2]",
            "key1=value3"
        ));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);

            assertEquals("value1", cfg.getString("global"));
            assertEquals("value2", cfg.getString("section1/key1"));
            assertEquals("value3", cfg.getString("section2/key1"));
        }
    }

    @Test
    void testINIHierarchyNormalization() throws Exception
    {
        Path configFile = tempDir.resolve(CONFIG_NAME + ".ini");
        Files.write(configFile, List.of(
            "[a/b]",
            "c=d",
            "[x]",
            "y/z=w"
        ));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);

            assertEquals("d", cfg.getString("a/b/c"));
            assertEquals("w", cfg.getString("x/y/z"));
        }
    }

    @Test
    void testINIWritingIntegration() throws Exception
    {
        // Pre-create the file so it is discovered as a writeable layer
        Path configFile = tempDir.resolve(CONFIG_NAME + ".ini");
        Files.createFile(configFile);

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));
        // Ensure INI is in writing priorities for the test
        builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, List.of("INI"));

        try (ConfigFactory factory = builder.build())
        {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            cfg.put("newGlobal", "val1", ConfigScope.USER);
            cfg.put("newSection/key2", "val2", ConfigScope.USER);
            cfg.flush();
        }

        assertTrue(Files.exists(configFile), "Config file should exist");
        List<String> lines = Files.readAllLines(configFile);
        
        // Basic check of file content
        boolean foundGlobal = false;
        boolean foundSection = false;
        boolean foundKey = false;
        for (String line : lines) {
            if (line.contains("newGlobal=val1")) foundGlobal = true;
            if (line.contains("[newSection]")) foundSection = true;
            if (line.contains("key2=val2")) foundKey = true;
        }
        
        assertTrue(foundGlobal, "Should find global key");
        assertTrue(foundSection, "Should find section");
        assertTrue(foundKey, "Should find key in section");
    }
}
