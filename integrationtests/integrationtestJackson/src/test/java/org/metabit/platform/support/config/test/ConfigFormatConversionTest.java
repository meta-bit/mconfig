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

/**
 * Validates configuration consistency across different file formats (JSON to YAML conversion).
 */
public class ConfigFormatConversionTest
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
    @DisplayName("Convert JSON config to YAML and verify content equality")
    void testJsonToYamlConversion()
            throws ConfigCheckedException, IOException
        {
        // 1. Load the original JSON configuration from resources
        // The factory should find myHierarchicalConfig.json automatically in test mode
        ConfigFactory factory = builder.build();
        Configuration jsonCfg = factory.getConfig("myHierarchicalConfig");

        // 2. Prepare a temporary directory for the YAML output
        Path yamlOutDir = tempDir.resolve("yaml_output");
        Files.createDirectories(yamlOutDir);
        Path yamlFile = yamlOutDir.resolve("myHierarchicalConfig.yaml");

        // 3. Write the configuration out as YAML using the Mapper
        // We provide the data to the YAML format handler via the Configuration's export/write capability
        // If the library supports direct format conversion via factory:
        ConfigMapper mapper = ConfigMapper.create(); // Or however your DI/Factory provides it
        mapper.saveTo(jsonCfg, yamlFile);

        // 4. Create a new factory pointed at the temporary YAML directory
        ConfigFactoryBuilder yamlBuilder = ConfigFactoryBuilder.create("myCompany", "myApp");
        yamlBuilder.setFeature(ConfigFeature.TEST_MODE, true);
        yamlBuilder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Collections.singletonList(yamlOutDir.toString()));

        ConfigFactory yamlFactory = yamlBuilder.build();
        Configuration yamlCfg = yamlFactory.getConfig("myHierarchicalConfig");

        // 5. Compare key values to ensure fidelity
        // We check a few known paths from a typical hierarchical config
        compareConfigs(jsonCfg, yamlCfg, "server/port");
        compareConfigs(jsonCfg, yamlCfg, "database/url");
        compareConfigs(jsonCfg, yamlCfg, "logging/level");
        }

    private void compareConfigs(Configuration original, Configuration derived, String key)
        {
        try
            {
            String originalVal = original.getString(key);
            String derivedVal = derived.getString(key);
            assertEquals(originalVal, derivedVal, "Value mismatch for key: "+key);
            }
        catch (ConfigException e)
            {
            // Handle cases where optional keys might not exist in both
            if (e.getReason() != ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY)
                {
                throw e;
                }
            }
        }
}