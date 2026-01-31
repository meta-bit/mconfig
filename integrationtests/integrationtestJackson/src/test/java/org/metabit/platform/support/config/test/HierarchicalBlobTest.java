package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HierarchicalBlobTest {

    @TempDir
    Path tempDir;

    @Test
    void testJsonWithBlob() throws Exception {
        byte[] data = {0, 1, 2, 3, 4, 5};
        String base64Data = Base64.getEncoder().encodeToString(data);
        
        Path configDir = tempDir;
        Files.createDirectories(configDir);
        Path jsonFile = configDir.resolve("myconfig.json");
        Files.writeString(jsonFile, "{ \"binaryData\": \"" + base64Data + "\", \"other\": \"value\" }");

        // We need a scheme to tell mConfig that "binaryData" is BYTES
        ConfigSchemeEntry schemeEntry = new ConfigSchemeEntry("binaryData", ConfigEntryType.BYTES);
        Set<ConfigSchemeEntry> entries = new HashSet<>();
        entries.add(schemeEntry);
        ConfigScheme scheme = ConfigScheme.fromSchemeEntries(entries);

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test");
        builder.setTestConfigPaths(ConfigScope.USER, java.util.List.of(tempDir.toAbsolutePath().toString()));
        builder.setTestMode(true);
        ConfigFactory factory = builder.build();

        Configuration cfg = factory.getConfig("myconfig", scheme);
        
        ConfigEntry entry = cfg.getConfigEntryFromFullKey("binaryData", java.util.EnumSet.allOf(ConfigScope.class));
        assertNotNull(entry, "Entry 'binaryData' should exist");
        assertEquals(ConfigEntryType.BYTES, entry.getType());
        assertArrayEquals(data, entry.getValueAsBytes());
        assertEquals("binaryData", entry.getKey());
    }

    @Test
    void testYamlWithBlob() throws Exception {
        byte[] data = {10, 20, 30, 40};
        String base64Data = Base64.getEncoder().encodeToString(data);
        
        Path configDir = tempDir;
        Files.createDirectories(configDir);
        Path yamlFile = configDir.resolve("myconfig.yaml");
        Files.writeString(yamlFile, "binaryData: !!binary " + base64Data + "\nother: value");

        // YAML with Jackson (or SnakeYAML) supports !!binary tag
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test");
        builder.setTestConfigPaths(ConfigScope.USER, java.util.List.of(tempDir.toAbsolutePath().toString()));
        builder.setTestMode(true);
        ConfigFactory factory = builder.build();

        Configuration cfg = factory.getConfig("myconfig");
        
        ConfigEntry entry = cfg.getConfigEntryFromFullKey("binaryData", java.util.EnumSet.allOf(ConfigScope.class));
        assertNotNull(entry, "Entry 'binaryData' should exist");
        assertEquals(ConfigEntryType.BYTES, entry.getType());
        assertArrayEquals(data, entry.getValueAsBytes());
        assertEquals("binaryData", entry.getKey());
    }
}
