package org.metabit.platform.support.config.impl.format.javaproperties;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
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
        format.testComponent(settings, NullLogging.getSingletonInstance());

        String properties = "foo=bar\n" +
                           "nested/key=value\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, null);

        ConfigLayerInterface layer = format.readStream(inputStream, location);

        assertNotNull(layer);
        assertEquals("bar", layer.getEntry("foo").getValueAsString());
        assertEquals("value", layer.getEntry("nested/key").getValueAsString());
        }

    @Test
    public void testCommentsRoundTrip() throws Exception
        {
        FileJavaPropertiesFormat format = new FileJavaPropertiesFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        format.testComponent(settings, NullLogging.getSingletonInstance());

        String properties = ""
                + "# top comment\n"
                + "foo=bar\n"
                + "! mid comment\n"
                + "key\\ with\\ spaces: value\n";

        Path tempFile = Files.createTempFile("mConfigPropsTest", ".properties");
        try
            {
            Files.writeString(tempFile, properties, StandardCharsets.UTF_8);
            ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new TestStorage(), format, tempFile);
            ConfigLayerInterface layer = format.readFile(tempFile.toFile(), location);
            assertNotNull(layer);
            format.writeFile(layer);

            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
            String joined = String.join("\n", lines);
            assertTrue(joined.contains("# top comment"));
            assertTrue(joined.contains("foo=bar"));
            assertTrue(joined.contains("! mid comment"));
            assertTrue(joined.contains("key\\ with\\ spaces=value"));
            }
        finally
            {
            Files.deleteIfExists(tempFile);
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
            return URI.create("test://properties");
            }

        @Override
        public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface layeredCfg)
            {
            }

        @Override
        public org.metabit.platform.support.config.interfaces.ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, org.metabit.platform.support.config.scheme.ConfigScheme configScheme, org.metabit.platform.support.config.impl.LayeredConfiguration layeredConfiguration)
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
