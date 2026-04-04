package org.metabit.platform.support.config.impl.format.toml;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TOMLWriteOrderTest
{
    @Test
    void testNewEntryAddedAtTopLevel() throws Exception
        {
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());

        String toml = "[section]\n" +
                "key = \"value\"\n";

        InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new TestConfigLocation();

        TOMLConfigLayer layer = (TOMLConfigLayer) format.readStream(is, location);
        assertNotNull(layer);

        // Add a new entry that should be at the top level
        ConfigEntry newEntry = new GenericConfigEntryLeaf("newKey", "newValue", org.metabit.platform.support.config.ConfigEntryType.STRING, null);
        layer.writeEntry(newEntry);

        TomlWriter writer = new TomlWriter();
        String output = writer.write(layer.getRoot());

        // The issue is that newKey might be placed AFTER [section]
        // In TOML, anything after [section] belongs to [section]
        
        int sectionIndex = output.indexOf("[section]");
        int newKeyIndex = output.indexOf("newKey = \"newValue\"");
        
        assertTrue(newKeyIndex < sectionIndex, "New top-level key should be written before any sections, but was:\n" + output);
        }

    private static final class TestConfigLocation implements ConfigLocation
        {
        @Override
        public String toLocationString() { return "test://toml"; }
        @Override
        public URI getURI(String key, String optionalFragment) { return URI.create("test://toml"); }
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
