package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YAMLwithSnakeYAMLFormatTest
{
    @Test
    public void testReadYAMLFromResources() throws Exception
        {
        // Use ConfigFactoryBuilder in test mode to discover the YAML file in resources
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "yamltest");
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        
        try (ConfigFactory factory = builder.build())
            {
            Configuration config = factory.getConfig("testConfig");
            assertNotNull(config);
            
            // Check basic entry retrieval
            assertEquals("YAML Test App", config.getString("name"));
            assertEquals("1.2.3", config.getString("version"));
            assertEquals("true", config.getString("enabled"));
            assertEquals("42", config.getString("count"));
            assertEquals("value", config.getString("nested/key"));

            // Use ConfigCursor and POJO mapping
            ConfigCursor cursor = config.getConfigCursor();
            assertNotNull(cursor);
            
            YAMLTestPojo pojo = new YAMLTestPojo();
            int mapped = cursor.copyMapToObject(pojo, "set", "");
            
            assertTrue(mapped >= 4, "Should have mapped at least 4 entries (name, version, enabled, count)");
            assertEquals("YAML Test App", pojo.getName());
            assertEquals("1.2.3", pojo.getVersion());
            assertTrue(pojo.isEnabled());
            assertEquals(42, pojo.getCount());
            }
        }

    @Test
    public void testReadYAML() throws Exception
        {
        YAMLwithSnakeYAMLFormat format = new YAMLwithSnakeYAMLFormat();
        org.metabit.platform.support.config.impl.ConfigFactorySettings settings = new org.metabit.platform.support.config.impl.ConfigFactorySettings();
        ConsoleLogging logger = new ConsoleLogging("YAMLFormatTest");
        logger.setLevel("DEBUG");
        format.testComponent(settings, logger);

        String yaml = "foo: bar\n" +
                      "nested:\n" +
                      "  key: value\n" +
                      "number: 42\n" +
                      "bool: true";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ConfigStorageInterface mockStorage = Mockito.mock(ConfigStorageInterface.class);
        Mockito.when(mockStorage.getStorageName()).thenReturn("mock");
        Mockito.when(mockStorage.getStorageID()).thenReturn("mock");

        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, mockStorage, format, null);
        
        ConfigLayerInterface layer = format.readStream(inputStream, location);
        
        assertNotNull(layer);
        assertEquals("bar", layer.getEntry("foo").getValueAsString());
        assertEquals("value", layer.getEntry("nested/key").getValueAsString());
        assertEquals("42", layer.getEntry("number").getValueAsString());
        assertEquals("true", layer.getEntry("bool").getValueAsString());
        }

    @Test
    public void testGetFilenameExtensions()
        {
        YAMLwithSnakeYAMLFormat format = new YAMLwithSnakeYAMLFormat();
        assertTrue(format.getFilenameExtensions().contains(".yaml"));
        assertTrue(format.getFilenameExtensions().contains(".yml"));
        }

    @Test
    public void testGetFormatID()
        {
        YAMLwithSnakeYAMLFormat format = new YAMLwithSnakeYAMLFormat();
        assertEquals("YAMLwithSnakeYAML", format.getFormatID());
        }
}
