package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.mapper.ConfigMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigTypePreservationTest
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
    void testTypePreservationInYaml() throws ConfigCheckedException, IOException
        {
        ConfigFactory factory = builder.build();
        Configuration jsonCfg = factory.getConfig("myHierarchicalConfig");

        Path yamlFile = tempDir.resolve("output.yaml");
        ConfigMapper mapper = ConfigMapper.create();
        mapper.saveTo(jsonCfg, yamlFile);

        String yamlContent = Files.readString(yamlFile);
        
        // In YAML, unquoted true is boolean, unquoted number is number.
        // We want to make sure they are NOT quoted.
        
        // Note: Different YAML libraries might format differently, 
        // but typically "enabled: true" (unquoted) is desired for boolean.
        assertTrue(yamlContent.contains("enabled: true"), "Boolean should be preserved as unquoted in YAML. Content: " + yamlContent);
        assertTrue(yamlContent.contains("port: 1234"), "Number should be preserved as unquoted in YAML. Content: " + yamlContent);
        }

    @Test
    void testTypePreservationInJson() throws ConfigCheckedException, IOException
        {
        ConfigFactory factory = builder.build();
        Configuration jsonCfg = factory.getConfig("myHierarchicalConfig");

        Path jsonOutputFile = tempDir.resolve("output.json");
        ConfigMapper mapper = ConfigMapper.create();
        mapper.saveTo(jsonCfg, jsonOutputFile);

        String jsonContent = Files.readString(jsonOutputFile);
        
        // In JSON, "port": 1234 is number, "enabled": true is boolean.
        assertTrue(jsonContent.contains("\"enabled\": true") || jsonContent.contains("\"enabled\" : true"), "Boolean should be preserved in JSON. Content: " + jsonContent);
        assertTrue(jsonContent.contains("\"port\": 1234") || jsonContent.contains("\"port\" : 1234"), "Number should be preserved in JSON. Content: " + jsonContent);
        }
}
