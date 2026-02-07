package org.metabit.platform.support.config.impl.format.javaproperties;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.core.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaPropertiesCommentTest
{
    @Test
    public void testReadComments() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        format.testComponent(settings, NullLogging.getSingletonInstance());

        String properties = ""
                + "# leading comment line 1\n"
                + "# leading comment line 2\n"
                + "foo=bar\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, null);

        ConfigLayerInterface layer = format.readStream(inputStream, location);
        ConfigEntry entry = layer.getEntry("foo");
        assertNotNull(entry);
        assertEquals("bar", entry.getValueAsString());
        assertEquals("# leading comment line 1\n# leading comment line 2", entry.getComment());
        }

    @Test
    public void testWriteComments() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        format.testComponent(settings, NullLogging.getSingletonInstance());

        Path tempFile = Files.createTempFile("mConfigPropsCommentWriteTest", ".properties");
        try
            {
            ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, tempFile);
            JavaPropertiesConfigLayer layer = new JavaPropertiesConfigLayer(settings, location, format, tempFile);

            ConfigEntry entry = layer.getEntry("newKey");
            assertNull(entry);

            // Create entry with comment
            ConfigEntry newEntry = new org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf("newKey", "newValue", new org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata((ConfigSource)location));
            newEntry.setComment("this is a programmatic comment");
            layer.writeEntry(newEntry);
            layer.flush();

            String content = Files.readString(tempFile, StandardCharsets.UTF_8);
            assertTrue(content.contains("# this is a programmatic comment"), "Content should contain the comment");
            assertTrue(content.contains("newKey=newValue"), "Content should contain the key-value pair");
            }
        finally
            {
            Files.deleteIfExists(tempFile);
            }
        }

    @Test
    public void testMultiLineComments() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        format.testComponent(settings, NullLogging.getSingletonInstance());

        String properties = ""
                + "# Line 1\n"
                + "# Line 2\n"
                + "foo=bar\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, null);

        ConfigLayerInterface layer = format.readStream(inputStream, location);
        ConfigEntry entry = layer.getEntry("foo");
        assertEquals("# Line 1\n# Line 2", entry.getComment());
        }

    @Test
    public void testGlobalHeaderComments() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_READING, true);
        format.testComponent(settings, NullLogging.getSingletonInstance());

        String properties = ""
                + "# Global Header\n"
                + "\n"
                + "# Entry Comment\n"
                + "foo=bar\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, null);

        JavaPropertiesConfigLayer layer = (JavaPropertiesConfigLayer) format.readStream(inputStream, location);
        assertEquals(List.of("# Global Header"), layer.internalGetGlobalHeaderComments());
        assertEquals("# Entry Comment", layer.getEntry("foo").getComment());
        }

    @Test
    public void testAppendProgrammaticComment() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        settings.put(ConfigFeature.COMMENTS_READING, false); // Scenario B: Preserve-Only
        format.testComponent(settings, NullLogging.getSingletonInstance());

        Path tempFile = Files.createTempFile("mConfigPropsAppendTest", ".properties");
        try
            {
            String initial = "# Existing Comment\nkey=value\n";
            Files.writeString(tempFile, initial);

            ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, tempFile);
            JavaPropertiesConfigLayer layer = (JavaPropertiesConfigLayer) format.readFile(tempFile.toFile(), location);

            ConfigEntry entry = layer.getEntry("key");
            entry.setComment("New Comment");
            layer.writeEntry(entry);
            layer.flush();

            String content = Files.readString(tempFile);
            assertTrue(content.contains("# Existing Comment\n# New Comment\nkey=value"), "Should append new comment after existing one");
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
        @Override public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment) { return URI.create("test://properties"); }
        @Override public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface layeredCfg) { }
        @Override public org.metabit.platform.support.config.interfaces.ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, org.metabit.platform.support.config.scheme.ConfigScheme configScheme, org.metabit.platform.support.config.impl.LayeredConfiguration layeredConfiguration) { return null; }
        @Override public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, org.metabit.platform.support.config.impl.BlobConfiguration blobConfig) { }
        @Override public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location) { return Collections.emptySet(); }
        @Override public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle) { return false; }
        @Override public void triggerChangeCheck(Object storageInstanceHandle) { }
        }
}
