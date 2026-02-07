package org.metabit.platform.support.config.impl.format.toml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TOMLCommentTest
{
    @TempDir
    Path tempDir;

    @Test
    public void testReadComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(settings, org.metabit.platform.support.config.impl.core.NullLogging.getSingletonInstance());
        
        String toml = "# leading comment\n" +
                      "key = \"value\" # inline comment\n";
        
        TOMLConfigLayer layer = (TOMLConfigLayer) format.readStream(new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8)), location);
        
        ConfigEntry entry = layer.getEntry("key");
        assertNotNull(entry);
        assertEquals("value", entry.getValueAsString());
        assertEquals("# leading comment # inline comment", entry.getComment());
        }

    @Test
    public void testWriteComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        settings.put(ConfigFeature.WRITE_SYNC, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(settings, org.metabit.platform.support.config.impl.core.NullLogging.getSingletonInstance());

        Path tempFile = tempDir.resolve("test.toml");
        TOMLConfigLayer layer = (TOMLConfigLayer) format.createFile(tempFile, location);

        ConfigEntry newEntry = new TypedConfigEntryLeaf("section/key", "value", ConfigEntryType.STRING, new ConfigEntryMetadata(layer.getSource()));
        newEntry.setComment("programmatic comment");
        layer.writeEntry(newEntry);

        String content = Files.readString(tempFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("# programmatic comment"));
        assertTrue(content.contains("[section]"));
        assertTrue(content.contains("key = \"value\""));
        }

    @Test
    public void testMultiLineComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(settings, org.metabit.platform.support.config.impl.core.NullLogging.getSingletonInstance());

        String toml = "# Line 1\n" +
                      "# Line 2\n" +
                      "key = \"value\"\n";

        TOMLConfigLayer layer = (TOMLConfigLayer) format.readStream(new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8)), location);

        ConfigEntry entry = layer.getEntry("key");
        assertEquals("# Line 1\n# Line 2", entry.getComment());
        }

    @Test
    public void testGlobalHeaderComments() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(settings, org.metabit.platform.support.config.impl.core.NullLogging.getSingletonInstance());

        String toml = "# Global Header\n" +
                      "\n" +
                      "# Entry Comment\n" +
                      "key = \"value\"\n";

        TOMLConfigLayer layer = (TOMLConfigLayer) format.readStream(new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8)), location);

        assertEquals(List.of("# Global Header"), layer.getGlobalHeaderComments());
        assertEquals("# Entry Comment", layer.getEntry("key").getComment());
        }

    @Test
    public void testAppendProgrammaticComment() throws Exception
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        settings.put(ConfigFeature.COMMENTS_READING, false);
        settings.put(ConfigFeature.WRITE_SYNC, true);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), null, null);
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(settings, org.metabit.platform.support.config.impl.core.NullLogging.getSingletonInstance());

        Path tempFile = tempDir.resolve("append.toml");
        Files.writeString(tempFile, "# Existing\nkey = \"value\"\n");
        
        TOMLConfigLayer layer = (TOMLConfigLayer) format.readFile(tempFile.toFile(), location);

        ConfigEntry entry = layer.getEntry("key");
        entry.setComment("New");
        layer.writeEntry(entry);

        String content = Files.readString(tempFile);
        // Should contain both
        assertTrue(content.contains("# Existing"));
        assertTrue(content.contains("# New"));
        }

    private static final class TestStorage implements ConfigStorageInterface
        {
        @Override public String getStorageName() { return "test"; }
        @Override public String getStorageID() { return "test"; }
        @Override public boolean test(ConfigFactorySettings settings, org.metabit.platform.support.config.interfaces.ConfigLoggingInterface logger) { return true; }
        @Override public boolean init(org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx) { return true; }
        @Override public void exit() { }
        @Override public boolean isGenerallyWriteable() { return true; }
        @Override public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment) { return URI.create("test://toml"); }
        @Override public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface layeredCfg) { }
        @Override public org.metabit.platform.support.config.interfaces.ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, org.metabit.platform.support.config.scheme.ConfigScheme configScheme, org.metabit.platform.support.config.impl.LayeredConfiguration layeredConfiguration) { return null; }
        @Override public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, org.metabit.platform.support.config.impl.BlobConfiguration blobConfig) { }
        @Override public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location) { return Collections.emptySet(); }
        @Override public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle) { return false; }
        @Override public void triggerChangeCheck(Object storageInstanceHandle) { }
        }
}
