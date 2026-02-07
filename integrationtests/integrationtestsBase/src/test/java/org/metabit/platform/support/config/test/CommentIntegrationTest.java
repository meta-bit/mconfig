package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.impl.ext.ConfigSchemeImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommentIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testDescriptionOnCreate() throws Exception {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test");
        builder.setTestConfigPaths(ConfigScope.SESSION, List.of(tempDir.toAbsolutePath().toString()));
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true);
        builder.setFeature(ConfigFeature.WRITE_SYNC, true);
        builder.setFeature(ConfigFeature.COMMENTS_WRITING, true);
        builder.setFeature(ConfigFeature.COMMENTS_READING, true);
        builder.setFeature(ConfigFeature.DESCRIPTION_ON_CREATE, true);

        ConfigScheme scheme = new ConfigSchemeImpl();
        scheme.addSchemeEntry("mykey", ConfigEntryType.STRING, null, null, "This is a key description", null, null);

        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("testconfig", scheme);

        // This should trigger DESCRIPTION_ON_CREATE
        cfg.put("mykey", "myvalue", ConfigScope.SESSION);

        // Read back via API and verify the entry exists
        ConfigEntry entry = cfg.getConfigEntryFromFullKey("mykey", java.util.EnumSet.of(ConfigScope.SESSION));
        assertNotNull(entry, "Entry should be present after put");
        assertEquals("myvalue", entry.getValueAsString());

        // Additionally, verify on-disk content contains the description comment and key=value
        java.net.URI entryUri = entry.getURI();
        assertNotNull(entryUri, "Entry URI should not be null");
        java.nio.file.Path filePath = java.nio.file.Paths.get(entryUri);
        String fileContent = java.nio.file.Files.readString(filePath);
        // Debug aid on failure
        System.out.println("[DEBUG_LOG] Config file path: " + filePath);
        System.out.println("[DEBUG_LOG] Config file content:\n" + fileContent);
        assertTrue(fileContent.contains("This is a key description"), "File should contain the description as a comment");
        assertTrue(fileContent.contains("mykey=myvalue") || fileContent.contains("mykey = myvalue"), "File should contain the key-value pair");

        // Sanity: scheme must carry the same description
        org.metabit.platform.support.config.interfaces.ConfigEntrySpecification spec = cfg.getConfigScheme().getSpecification("mykey");
        assertNotNull(spec, "Spec should exist for mykey");
        String specDesc;
        try { specDesc = spec.getDescription(java.util.Locale.getDefault()); } catch (Throwable t) { specDesc = spec.getDescription(); }
        if (specDesc == null || specDesc.isEmpty()) specDesc = spec.getDescription();
        assertEquals("This is a key description", specDesc, "Scheme description should match");
    }

}
