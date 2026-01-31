package org.metabit.platform.support.config.impl.format.javaproperties;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FileJavaPropertiesFormatTest
{
    @Test
    public void testGetFormatID()
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        assertEquals("properties", format.getFormatID());
        }

    @Test
    public void testGetFilenameExtensions()
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        assertTrue(format.getFilenameExtensions().contains(".properties"));
        }

    @Test
    public void testReadStream() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigLoggingInterface logger = Mockito.mock(ConfigLoggingInterface.class);
        format.testComponent(settings, logger);

        String properties = "foo=bar\n" +
                           "nested/key=value\n";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        ConfigStorageInterface mockStorage = Mockito.mock(ConfigStorageInterface.class);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, mockStorage, format, null);
        
        ConfigLayerInterface layer = format.readStream(inputStream, location);
        
        assertNotNull(layer);
        assertEquals("bar", layer.getEntry("foo").getValueAsString());
        assertEquals("value", layer.getEntry("nested/key").getValueAsString());
        }
}
