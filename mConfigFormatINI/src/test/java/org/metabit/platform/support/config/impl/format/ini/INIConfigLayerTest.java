package org.metabit.platform.support.config.impl.format.ini;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class INIConfigLayerTest
{
    @Test
    public void testLoadAndGetEntry() throws Exception
        {
        ConfigFactorySettings settings = Mockito.mock(ConfigFactorySettings.class);
        Mockito.when(settings.getBoolean(Mockito.any())).thenReturn(true);
        ConfigLocation location = Mockito.mock(ConfigLocation.class);
        INIFileFormat format = new INIFileFormat();
        INIConfigLayer layer = new INIConfigLayer(settings, location, format, null);

        String ini = "top=value1\n" +
                     "[section1]\n" +
                     "key1=value2\n" +
                     "key2 = value3 \n" +
                     "; comment\n" +
                     "# another comment\n" +
                     "[section2]\n" +
                     "key1=value4\n";

        layer.load(new BufferedReader(new StringReader(ini)));

        ConfigEntry entryTop = layer.getEntry("top");
        assertNotNull(entryTop);
        assertEquals("value1", entryTop.getValueAsString());

        ConfigEntry entryS1K1 = layer.getEntry("section1/key1");
        assertNotNull(entryS1K1);
        assertEquals("value2", entryS1K1.getValueAsString());

        ConfigEntry entryS1K2 = layer.getEntry("section1/key2");
        assertNotNull(entryS1K2);
        assertEquals("value3", entryS1K2.getValueAsString());

        ConfigEntry entryS2K1 = layer.getEntry("section2/key1");
        assertNotNull(entryS2K1);
        assertEquals("value4", entryS2K1.getValueAsString());

        assertNull(layer.getEntry("nonexistent"));
        assertNull(layer.getEntry("section1/nonexistent"));
        }

    @Test
    public void testGetKeyIterator() throws IOException
        {
        ConfigFactorySettings settings = Mockito.mock(ConfigFactorySettings.class);
        Mockito.when(settings.getBoolean(Mockito.any())).thenReturn(true);
        ConfigLocation location = Mockito.mock(ConfigLocation.class);
        INIFileFormat format = new INIFileFormat();
        INIConfigLayer layer = new INIConfigLayer(settings, location, format, null);

        String ini = "top=value1\n" +
                     "[section1]\n" +
                     "key1=value2\n";

        layer.load(new BufferedReader(new StringReader(ini)));

        Iterator<String> it = layer.tryToGetKeyIterator();
        assertTrue(it.hasNext());
        assertEquals("top", it.next());
        assertTrue(it.hasNext());
        assertEquals("section1/key1", it.next());
        assertFalse(it.hasNext());
        }

    @Test
    public void testWriteEntry() throws Exception
        {
        ConfigFactorySettings settings = Mockito.mock(ConfigFactorySettings.class);
        Mockito.when(settings.getBoolean(ConfigFeature.TRIM_TEXTVALUE_SPACES)).thenReturn(true);
        Mockito.when(settings.getBoolean(ConfigFeature.WRITE_SYNC)).thenReturn(true);
        ConfigLocation location = Mockito.mock(ConfigLocation.class);
        INIFileFormat format = Mockito.spy(new INIFileFormat());
        
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("mConfigTest", ".ini");
        try {
            INIConfigLayer layer = new INIConfigLayer(settings, location, format, tempFile);
            
            ConfigEntry entry = Mockito.mock(ConfigEntry.class);
            Mockito.when(entry.getKey()).thenReturn("s1/k1");
            Mockito.when(entry.getValueAsString()).thenReturn("v1");
            
            layer.writeEntry(entry);
            
            Map<String, Map<String, String>> data = layer.getData();
            assertTrue(data.containsKey("s1"));
            assertEquals("v1", data.get("s1").get("k1"));
            
            // Verify format.writeINI was called because WRITE_SYNC is true
            Mockito.verify(format, Mockito.atLeastOnce()).writeINI(Mockito.any(), Mockito.any());
            
            List<String> lines = java.nio.file.Files.readAllLines(tempFile);
            assertTrue(lines.contains("[s1]"));
            assertTrue(lines.contains("k1=v1"));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
        }
}
