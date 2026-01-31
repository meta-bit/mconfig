package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the prioritized writing logic.
 */
class WritePriorityIntegrationTest
{
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Priority 1: Update existing entry in scope")
    void testPriority1UpdateExisting() throws IOException, ConfigCheckedException
    {
        Path dir = tempDir.resolve("priority1");
        Files.createDirectories(dir);
        Path configFile = dir.resolve("config.properties");
        Files.writeString(configFile, "key1=original\nkey2=other");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "WRITE_TEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, 
            Arrays.asList("USER:" + dir.toString()));
        
        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("config");

        assertEquals("original", cfg.getString("key1"));

        // Write to USER scope
        cfg.put("key1", "updated", ConfigScope.USER);
        cfg.flush();

        // Verify file content
        String content = Files.readString(configFile);
        assertTrue(content.contains("key1=updated"));
        assertTrue(content.contains("key2=other"));
        assertEquals("updated", cfg.getString("key1"));
    }

    @Test
    @DisplayName("Priority 2: Add to existing writeable layer in scope")
    void testPriority2AddToExistingLayer() throws IOException, ConfigCheckedException
    {
        Path dir = tempDir.resolve("priority2");
        Files.createDirectories(dir);
        Path configFile = dir.resolve("config.properties");
        Files.writeString(configFile, "key1=existing");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "WRITE_TEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, 
            Arrays.asList("USER:" + dir.toString()));
        
        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("config");

        // key2 does not exist
        cfg.put("key2", "new_value", ConfigScope.USER);
        cfg.flush();

        // Verify it was added to the existing file in the scope
        String content = Files.readString(configFile);
        assertTrue(content.contains("key1=existing"));
        assertTrue(content.contains("key2=new_value"));
        assertEquals("new_value", cfg.getString("key2"));
    }

    @Test
    @DisplayName("Priority 3: Create new layer in scope")
    void testPriority3CreateNewLayer() throws IOException, ConfigCheckedException
    {
        Path dir = tempDir.resolve("priority3");
        Files.createDirectories(dir);
        // Note: We don't create the config file initially

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "WRITE_TEST");
        // We set the search path, but the file doesn't exist yet.
        // Filesystem storage should be able to create it.
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, 
            Arrays.asList("USER:" + dir.toString()));
        
        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("newconfig");

        cfg.put("new_key", "created_value", ConfigScope.USER);
        cfg.flush();

        Path createdFile = dir.resolve("newconfig.properties");
        assertTrue(Files.exists(createdFile));
        String content = Files.readString(createdFile);
        assertTrue(content.contains("new_key=created_value"));
    }

    @Test
    @DisplayName("Multi-scope write: Preferred scope order (specific to generic)")
    void testMultiScopeOrdering() throws IOException, ConfigCheckedException
    {
        Path userDir = tempDir.resolve("user");
        Path appDir = tempDir.resolve("app");
        Files.createDirectories(userDir);
        Files.createDirectories(appDir);
        
        Files.writeString(userDir.resolve("multi.properties"), "key=user_val");
        Files.writeString(appDir.resolve("multi.properties"), "key=app_val");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "MULTI_WRITE");
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, 
            Arrays.asList("USER:" + userDir.toString(), "APPLICATION:" + appDir.toString()));
        
        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("multi");

        // Write to both USER and APPLICATION
        // It should prefer USER (more specific)
        cfg.put("key", "new_val", EnumSet.of(ConfigScope.USER, ConfigScope.APPLICATION));
        cfg.flush();

        // Verify USER was updated, APPLICATION remains same
        String userContent = Files.readString(userDir.resolve("multi.properties"));
        assertTrue(userContent.contains("key=new_val"), "USER file should contain updated value. Got: " + userContent);
        
        String appContent = Files.readString(appDir.resolve("multi.properties"));
        assertTrue(appContent.contains("key=app_val"), "APPLICATION file should remain unchanged. Got: " + appContent);
    }
}
