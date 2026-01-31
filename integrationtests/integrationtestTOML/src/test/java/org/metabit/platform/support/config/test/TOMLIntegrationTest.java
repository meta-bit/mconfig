package org.metabit.platform.support.config.test;

import org.metabit.platform.support.config.mapper.ConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        }

    @Test
    @DisplayName("Convert JSON config to TOML and verify content equality")
    void testJsonToTomlConversion()
            throws ConfigCheckedException, IOException
        {
        // 1. Prepare a JSON config file
        Path jsonDir = tempDir.resolve("json_input");
        Files.createDirectories(jsonDir);
        Path jsonFile = jsonDir.resolve("myTestConfig.json");
        Files.write(jsonFile, "{\"server\":{\"port\":8080},\"database\":{\"url\":\"jdbc:mysql://localhost/db\"}}".getBytes());

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

        // 5. Compare key values
        assertEquals("8080", tomlCfg.getString("server/port"), "Port mismatch. TOML content was:\n" + tomlContent);
        assertEquals("jdbc:mysql://localhost/db", tomlCfg.getString("database/url"));
        }
}
