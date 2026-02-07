package org.metabit.platform.support.config.test;

import org.metabit.platform.support.config.mapper.ConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.LayeredConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TOMLIntegrationTest
{
    @TempDir
    Path tempDir;

    private ConfigFactoryBuilder builder;

    @BeforeEach
    void setUp()
        {
        builder = ConfigFactoryBuilder.create("myCompany", "myApp");
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        // Force native TOML priority
        builder.setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, Collections.singletonList("TOML"));
        }

    @Test
    @DisplayName("Convert JSON config to TOML and verify content and types")
    void testJsonToTomlConversion()
            throws ConfigCheckedException, IOException
        {
        // 1. Prepare a JSON config file
        Path jsonDir = tempDir.resolve("json_input");
        Files.createDirectories(jsonDir);
        Path jsonFile = jsonDir.resolve("myTestConfig.json");
        Files.write(jsonFile,
                ("{" +
                        "\"server\":{\"port\":8080,\"pi\":3.14,\"enabled\":true}," +
                        "\"database\":{\"url\":\"jdbc:mysql://localhost/db\"}," +
                        "\"owner\":{\"name\":\"Tom\"}" +
                "}").getBytes());

        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Collections.singletonList(jsonDir.toString()));
        ConfigFactory factory = builder.build();
        Configuration jsonCfg = factory.getConfig("myTestConfig");

        // 2. Prepare a temporary directory for the TOML output
        Path tomlOutDir = tempDir.resolve("toml_output");
        Files.createDirectories(tomlOutDir);
        Path tomlFile = tomlOutDir.resolve("myTestConfig.toml");

        // 3. Write the configuration out as TOML using the Mapper
        ConfigMapper mapper = ConfigMapper.create();
        mapper.saveTo(jsonCfg, tomlFile);

        assertTrue(Files.exists(tomlFile), "TOML file should have been created");
        String tomlContent = Files.readString(tomlFile);
        System.out.println("TOML content:\n" + tomlContent);

        // 4. Create a new factory pointed at the temporary TOML directory
        ConfigFactoryBuilder tomlBuilder = ConfigFactoryBuilder.create("myCompany", "myApp");
        tomlBuilder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        tomlBuilder.setFeature(ConfigFeature.TEST_MODE, true);
        tomlBuilder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Collections.singletonList(tomlOutDir.toString()));

        ConfigFactory tomlFactory = tomlBuilder.build();
        Configuration tomlCfg = tomlFactory.getConfig("myTestConfig");

        // 5. Compare key values and types
        assertEquals(8080, tomlCfg.getInteger("server/port"));
        assertEquals(3.14, tomlCfg.getDouble("server/pi"), 0.0001);
        assertEquals(true, tomlCfg.getBoolean("server/enabled"));
        assertEquals("jdbc:mysql://localhost/db", tomlCfg.getString("database/url"));
        assertEquals("Tom", tomlCfg.getString("owner/name"));

        assertEntryType(tomlCfg, "server/port", ConfigEntryType.NUMBER);
        assertEntryType(tomlCfg, "server/pi", ConfigEntryType.NUMBER);
        assertEntryType(tomlCfg, "server/enabled", ConfigEntryType.BOOLEAN);
        assertEntryType(tomlCfg, "database/url", ConfigEntryType.STRING);
        assertEntryType(tomlCfg, "owner/name", ConfigEntryType.STRING);
        }

    @Test
    @DisplayName("Convert TOML with comments to Properties and back to TOML, verifying comment preservation")
    void testTomlCommentPreservation()
            throws ConfigCheckedException, IOException
        {
        // 1. Prepare a TOML config file with comments
        Path tomlDir = tempDir.resolve("toml_input");
        Files.createDirectories(tomlDir);
        Path tomlFile = tomlDir.resolve("commentTest.toml");
        String originalToml = "# Global Header\n" +
                              "\n" +
                              "# Port leading\n" +
                              "port = 8080 # Port inline\n";
        Files.writeString(tomlFile, originalToml);

        // 2. Load it
        ConfigFactoryBuilder tomlBuilder = ConfigFactoryBuilder.create("myCompany", "myApp");
        tomlBuilder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        tomlBuilder.setFeature(ConfigFeature.TEST_MODE, true);
        // Ensure we don't use Jackson
        tomlBuilder.setFeature(ConfigFeature.DISABLED_MODULE_IDS, Collections.singletonList("TOMLwithJackson"));
        // Force native TOML priority
        tomlBuilder.setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, Collections.singletonList("TOML"));
        
        tomlBuilder.setTestConfigPaths(ConfigScope.RUNTIME, Collections.singletonList(tomlDir.toString()));
        tomlBuilder.setFeature(ConfigFeature.COMMENTS_READING, true);
        tomlBuilder.setFeature(ConfigFeature.COMMENTS_WRITING, true);

        ConfigFactory factory = tomlBuilder.build();
        Configuration tomlCfg = factory.getConfig("commentTest");

        // 3. Verify comments were read
        ConfigEntry portEntry = tomlCfg.getConfigEntryFromFullKey("port", EnumSet.allOf(ConfigScope.class));
        assertNotNull(portEntry, "Port entry should not be null");
        assertNotNull(portEntry.getComment(), "Port comment should not be null");
        assertTrue(portEntry.getComment().contains("# Port leading"));
        assertTrue(portEntry.getComment().contains("# Port inline"));

        // 4. Save to Properties file via Mapper (Properties supports comments)
        Path propsOutDir = tempDir.resolve("props_output");
        Files.createDirectories(propsOutDir);
        Path propsFile = propsOutDir.resolve("commentTest.properties");

        ConfigMapper mapper = ConfigMapper.create();
        mapper.saveTo(tomlCfg, propsFile);

        String propsContent = Files.readString(propsFile);
        System.out.println("Intermediate Properties content:\n" + propsContent);
        // Properties uses # or ! for comments. Mapper/Layer should have preserved them.
        assertTrue(propsContent.contains("# Port leading") || propsContent.contains("! Port leading"));

        // 5. Load from Properties and save back to TOML
        ConfigFactoryBuilder propsBuilder = ConfigFactoryBuilder.create("myCompany", "myApp");
        propsBuilder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        propsBuilder.setFeature(ConfigFeature.TEST_MODE, true);
        propsBuilder.setTestConfigPaths(ConfigScope.RUNTIME, Collections.singletonList(propsOutDir.toString()));
        propsBuilder.setFeature(ConfigFeature.COMMENTS_READING, true);
        propsBuilder.setFeature(ConfigFeature.COMMENTS_WRITING, true);

        ConfigFactory propsFactory = propsBuilder.build();
        Configuration propsCfg = propsFactory.getConfig("commentTest");

        Path finalTomlFile = tempDir.resolve("final.toml");
        mapper.saveTo(propsCfg, finalTomlFile);

        // 6. Verify preserved comments in final TOML output
        String outputContent = Files.readString(finalTomlFile);
        System.out.println("Final Output TOML with comments:\n" + outputContent);
        
        // Note: Global headers might not be preserved across format conversion via Mapper 
        // if Mapper doesn't support them, but entry comments should.
        assertTrue(outputContent.contains("# Port leading"), "Should preserve leading comment");
        assertTrue(outputContent.contains("# Port inline"), "Should preserve inline comment");
        }

    private void assertEntryType(Configuration cfg, String key, ConfigEntryType expectedType)
        {
        assertTrue(cfg instanceof LayeredConfiguration, "Expected LayeredConfiguration");
        LayeredConfiguration layered = (LayeredConfiguration) cfg;
        ConfigEntry entry = layered.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
        assertNotNull(entry, "Missing entry for key: " + key);
        assertEquals(expectedType, entry.getType(), "Type mismatch for key: " + key);
        }
}
