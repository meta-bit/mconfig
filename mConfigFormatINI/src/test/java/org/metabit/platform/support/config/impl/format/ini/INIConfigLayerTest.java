package org.metabit.platform.support.config.impl.format.ini;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class INIConfigLayerTest
{
    @Test
    public void testLoadAndGetEntry() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.TRIM_TEXTVALUE_SPACES, Boolean.TRUE);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
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
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.TRIM_TEXTVALUE_SPACES, Boolean.TRUE);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
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
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.TRIM_TEXTVALUE_SPACES, Boolean.TRUE);
        settings.put(ConfigFeature.WRITE_SYNC, Boolean.TRUE);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("mConfigTest", ".ini");
        try
            {
            INIConfigLayer layer = new INIConfigLayer(settings, location, format, tempFile);

            ConfigEntry entry = new StringConfigEntryLeaf("s1/k1", "v1", new ConfigEntryMetadata(layer.getSource()));
            layer.writeEntry(entry);

            Map<String, Map<String, String>> data = layer.getData();
            assertTrue(data.containsKey("s1"));
            assertEquals("v1", data.get("s1").get("k1"));

            List<String> lines = java.nio.file.Files.readAllLines(tempFile);
            assertTrue(lines.contains("[s1]"));
            assertTrue(lines.contains("k1=v1"));
            }
        finally
            {
            java.nio.file.Files.deleteIfExists(tempFile);
            }
        }

    @Test
    public void testCommentsRoundTrip() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.TRIM_TEXTVALUE_SPACES, Boolean.TRUE);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("mConfigTest", ".ini");
        try
            {
            INIConfigLayer layer = new INIConfigLayer(settings, location, format, tempFile);
            String ini = ""
                    + "# top comment\n"
                    + "root=value # inline root\n"
                    + "; section comment\n"
                    + "[section] ; inline section\n"
                    + "; key comment\n"
                    + "key=value ; inline key\n"
                    + "arr=1;notacomment\n";
            layer.load(new BufferedReader(new StringReader(ini)));
            format.writeFile(layer);

            List<String> lines = java.nio.file.Files.readAllLines(tempFile);
            String joined = String.join("\n", lines);
            assertTrue(joined.contains("# top comment"));
            assertTrue(joined.contains("root=value # inline root"));
            assertTrue(joined.contains("; section comment"));
            assertTrue(joined.contains("[section] ; inline section"));
            assertTrue(joined.contains("; key comment"));
            assertTrue(joined.contains("key=value ; inline key"));
            assertTrue(joined.contains("arr=1;notacomment"));
            }
        finally
            {
            java.nio.file.Files.deleteIfExists(tempFile);
            }
        }

    private static final class TestStorage implements ConfigStorageInterface
        {
        @Override
        public String getStorageName()
            {
            return "test";
            }

        @Override
        public String getStorageID()
            {
            return "test";
            }

        @Override
        public boolean test(ConfigFactorySettings settings, org.metabit.platform.support.config.interfaces.ConfigLoggingInterface logger)
            {
            return true;
            }

        @Override
        public boolean init(org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx)
            {
            return true;
            }

        @Override
        public void exit()
            {
            }

        @Override
        public boolean isGenerallyWriteable()
            {
            return true;
            }

        @Override
        public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
            {
            return URI.create("test://ini");
            }

        @Override
        public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface layeredCfg)
            {
            }

        @Override
        public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, org.metabit.platform.support.config.scheme.ConfigScheme configScheme, org.metabit.platform.support.config.impl.LayeredConfiguration layeredConfiguration)
            {
            return null;
            }

        @Override
        public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, org.metabit.platform.support.config.impl.BlobConfiguration blobConfig)
            {
            }

        @Override
        public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
            {
            return Collections.emptySet();
            }

        @Override
        public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
            {
            return false;
            }

        @Override
        public void triggerChangeCheck(Object storageInstanceHandle)
            {
            }
        }
}
