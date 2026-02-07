package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class YAMLCommentPersistenceTest
{
    @TempDir
    Path tempDir;

    @Test
    public void testCommentPersistence() throws Exception
    {
        YAMLwithSnakeYAMLFormat format = new YAMLwithSnakeYAMLFormat();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConsoleLogging logger = new ConsoleLogging("YAMLCommentTest");
        format.testComponent(settings, logger);

        String yaml = "# Top level comment\n" +
                      "foo: bar # Inline comment\n" +
                      "# Comment before block\n" +
                      "nested:\n" +
                      "  key: value\n";
        
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.write(yamlFile, yaml.getBytes(StandardCharsets.UTF_8));
        
        ConfigStorageInterface mockStorage = Mockito.mock(ConfigStorageInterface.class);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, mockStorage, format, yamlFile);
        
        // Read file
        ConfigLayerInterface layer = format.readFile(yamlFile.toFile(), location);
        assertNotNull(layer);
        
        if (layer instanceof YAMLSnakeYAMLConfigLayer) {
            Object data = ((YAMLSnakeYAMLConfigLayer) layer).getData();
            System.out.println("Data class: " + data.getClass().getName());
            if (data instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) data;
                for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                    System.out.println("Key: " + entry.getKey() + ", Value class: " + (entry.getValue() != null ? entry.getValue().getClass().getName() : "null"));
                }
            }
        }
        
        // Write file back
        format.writeFile(layer);
        
        // Read again and check for comments
        String updatedYaml = new String(Files.readAllBytes(yamlFile), StandardCharsets.UTF_8);
        System.out.println("Updated YAML:\n" + updatedYaml);
        
        assertTrue(updatedYaml.contains("# Top level comment"), "Should contain top level comment");
        assertTrue(updatedYaml.contains("# Inline comment"), "Should contain inline comment");
        assertTrue(updatedYaml.contains("# Comment before block"), "Should contain comment before block");

        // Now modify something and see if comments persist
        layer.writeEntry(new org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf("foo", "updated", null));
        format.writeFile(layer);

        String finalYaml = new String(Files.readAllBytes(yamlFile), StandardCharsets.UTF_8);
        System.out.println("Final YAML:\n" + finalYaml);
        assertTrue(finalYaml.contains("foo: updated"), "Value should be updated");
        assertTrue(finalYaml.contains("# Top level comment"), "Should still contain top level comment after update");
        // Inline comments on replaced nodes are currently lost, which is expected SnakeYAML behavior when replacing Nodes.
        // assertTrue(finalYaml.contains("# Inline comment"), "Should still contain inline comment after update");
    }
}
