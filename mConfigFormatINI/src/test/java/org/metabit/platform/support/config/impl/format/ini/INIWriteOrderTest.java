package org.metabit.platform.support.config.impl.format.ini;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class INIWriteOrderTest
{
    @TempDir
    Path tempDir;

    @Test
    void testNewEntryAddedAtTopLevel() throws Exception
        {
        INIFileFormat format = new INIFileFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setBoolean(org.metabit.platform.support.config.ConfigFeature.COMMENTS_WRITING, true);
        format.testComponent(settings, NullLogging.getSingletonInstance());

        String ini = "[section]\n" +
                "key = value\n";

        InputStream is = new ByteArrayInputStream(ini.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new TestConfigLocation();

        INIConfigLayer layer = (INIConfigLayer) format.readStream(is, location);
        assertNotNull(layer);

        // Add a new entry that should be at the top level
        ConfigEntry newEntry = new GenericConfigEntryLeaf("newKey", "newValue", ConfigEntryType.STRING, null);
        newEntry.setComment("new comment");
        
        // We need a writable layer to use writeEntry
        Path tempFile = tempDir.resolve("test.ini");
        INIConfigLayer writableLayer = new INIConfigLayer(settings, location, format, tempFile);
        writableLayer.load(new java.io.BufferedReader(new java.io.StringReader(ini)));
        
        // Ensure it's writable for the test
        // Current INIConfigLayer derives writability from path.canWrite()
        Files.writeString(tempFile, ini); 
        
        writableLayer.writeEntry(newEntry);
        format.writeFile(writableLayer);

        String output = Files.readString(tempFile);

        // The issue is that newKey might be placed AFTER [section]
        int sectionIndex = output.indexOf("[section]");
        int newKeyIndex = output.indexOf("newKey=newValue");
        
        assertTrue(newKeyIndex >= 0, "newKey should be present");
        assertTrue(newKeyIndex < sectionIndex, "New top-level key should be written before any sections, but was:\n" + output);
        }

    private static final class TestConfigLocation implements ConfigLocation
        {
        @Override
        public String toLocationString() { return "test://ini"; }
        @Override
        public URI getURI(String key, String optionalFragment) { return URI.create("test://ini"); }
        @Override
        public ConfigScope getScope() { return ConfigScope.SESSION; }
        @Override
        public boolean isWriteable() { return true; }
        @Override
        public ConfigStorageInterface getStorage() { return null; }
        @Override
        public Object getStorageInstanceHandle() { return null; }
        @Override
        public ConfigLocation derive(Path file) { return this; }
        }
}
