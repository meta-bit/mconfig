package org.metabit.platform.support.config.impl.format.ini;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class INICommentTest
{
    @Test
    public void testReadComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();
        INIConfigLayer layer = new INIConfigLayer(settings, location, format, null);

        String ini = ""
                + "; leading comment\n"
                + "key=value # inline comment\n";

        layer.load(new BufferedReader(new StringReader(ini)));

        ConfigEntry entry = layer.getEntry("key");
        assertNotNull(entry);
        assertEquals("value", entry.getValueAsString());
        // INIConfigLayer joins leading with \n and appends inline with a space
        assertEquals("; leading comment # inline comment", entry.getComment());
        }

    @Test
    public void testWriteComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        settings.put(ConfigFeature.WRITE_SYNC, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();

        Path tempFile = Files.createTempFile("mConfigINICommentWriteTest", ".ini");
        try
            {
            INIConfigLayer layer = new INIConfigLayer(settings, location, format, tempFile);

            ConfigEntry newEntry = new StringConfigEntryLeaf("section/key", "value", new ConfigEntryMetadata(layer.getSource()));
            newEntry.setComment("programmatic comment");
            layer.writeEntry(newEntry);

            String content = Files.readString(tempFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("; programmatic comment"));
            assertTrue(content.contains("[section]"));
            assertTrue(content.contains("key=value"));
            }
        finally
            {
            Files.deleteIfExists(tempFile);
            }
        }

    @Test
    public void testMultiLineComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();
        INIConfigLayer layer = new INIConfigLayer(settings, location, format, null);

        String ini = ""
                + "; Line 1\n"
                + "; Line 2\n"
                + "key=value\n";

        layer.load(new BufferedReader(new StringReader(ini)));

        ConfigEntry entry = layer.getEntry("key");
        assertEquals("; Line 1\n; Line 2", entry.getComment());
        }

    @Test
    public void testGlobalHeaderComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();
        INIConfigLayer layer = new INIConfigLayer(settings, location, format, null);

        String ini = ""
                + "# Global Header\n"
                + "\n"
                + "; Entry Comment\n"
                + "key=value\n";

        layer.load(new BufferedReader(new StringReader(ini)));

        assertEquals(List.of("# Global Header"), layer.getGlobalHeaderComments());
        assertEquals("; Entry Comment", layer.getEntry("key").getComment());
        }

    @Test
    public void testAppendProgrammaticComment() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        settings.put(ConfigFeature.COMMENTS_READING, false);
        settings.put(ConfigFeature.WRITE_SYNC, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        INIFileFormat format = new INIFileFormat();

        Path tempFile = Files.createTempFile("mConfigINIAppendTest", ".ini");
        try
            {
            Files.writeString(tempFile, "; Existing\nkey=value\n");
            INIConfigLayer layer = new INIConfigLayer(settings, location, format, tempFile);
            layer.load(new BufferedReader(new StringReader("; Existing\nkey=value\n")));

            ConfigEntry entry = layer.getEntry("key");
            entry.setComment("New");
            layer.writeEntry(entry);

            String content = Files.readString(tempFile);
            assertTrue(content.contains("; Existing\n; New\nkey=value"));
            }
        finally
            {
            Files.deleteIfExists(tempFile);
            }
        }

    private static final class TestStorage implements ConfigStorageInterface
        {
        @Override public String getStorageName() { return "test"; }
        @Override public String getStorageID() { return "test"; }
        @Override public boolean test(ConfigFactorySettings settings, org.metabit.platform.support.config.interfaces.ConfigLoggingInterface logger) { return true; }
        @Override public boolean init(org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx) { return true; }
        @Override public void exit() { }
        @Override public boolean isGenerallyWriteable() { return true; }
        @Override public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment) { return URI.create("test://ini"); }
        @Override public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface layeredCfg) { }
        @Override public org.metabit.platform.support.config.interfaces.ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, org.metabit.platform.support.config.scheme.ConfigScheme configScheme, org.metabit.platform.support.config.impl.LayeredConfiguration layeredConfiguration) { return null; }
        @Override public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, org.metabit.platform.support.config.impl.BlobConfiguration blobConfig) { }
        @Override public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location) { return Collections.emptySet(); }
        @Override public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle) { return false; }
        @Override public void triggerChangeCheck(Object storageInstanceHandle) { }
        }
}
