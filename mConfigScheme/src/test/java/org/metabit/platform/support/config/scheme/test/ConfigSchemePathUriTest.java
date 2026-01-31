package org.metabit.platform.support.config.scheme.test;
import org.metabit.platform.support.config.scheme.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemePathUriTest {

    @Test
    public void testUriValidation() throws ConfigCheckedException {
        String json = "[" +
                "  {" +
                "    \"KEY\": \"myUri\"," +
                "    \"TYPE\": \"URI\"" +
                "  }" +
                "]";

        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(new ConfigFactorySettings());
        Map<String, ConfigScheme> schemes = org.metabit.platform.support.config.scheme.impl.ext.JsonConfigSchemeParser.parseJSON(json, ctx);
        ConfigScheme scheme = schemes.get("");
        ConfigEntrySpecification spec = scheme.getSpecification("myUri");

        ConfigEntry validEntry = new org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf(
                "myUri", URI.create("https://example.com"), ConfigEntryType.URI, null);
        assertTrue(spec.validateEntry(validEntry));

        ConfigEntry invalidEntry = new org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf(
                "myUri", "not a uri space", null);
        assertFalse(spec.validateEntry(invalidEntry));
    }

//    @Test
    public void testFilePathValidation(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "hello".getBytes());
        Path testDir = tempDir.resolve("subdir");
        Files.createDirectory(testDir);

        String json = "[" +
                "  {" +
                "    \"KEY\": \"myFile\"," +
                "    \"TYPE\": \"FILEPATH\"," +
                "    \"MANDATORY\": { \"EXISTS\": true, \"IS_FILE\": true }" +
                "  }," +
                "  {" +
                "    \"KEY\": \"myDir\"," +
                "    \"TYPE\": \"FILEPATH\"," +
                "    \"MANDATORY\": { \"EXISTS\": true, \"IS_DIRECTORY\": true }" +
                "  }" +
                "]";

        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(new ConfigFactorySettings());
        Map<String, ConfigScheme> schemes = org.metabit.platform.support.config.scheme.impl.ext.JsonConfigSchemeParser.parseJSON(json, ctx);
        ConfigScheme scheme = schemes.get("");

        // Test File
        ConfigEntrySpecification fileSpec = scheme.getSpecification("myFile");
        ConfigEntry fileEntry = new org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf(
                "myFile", testFile, ConfigEntryType.FILEPATH, null);
        assertTrue(fileSpec.validateEntry(fileEntry), "Should be valid file");

        ConfigEntry dirAsFileEntry = new org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf(
                "myFile", testDir, ConfigEntryType.FILEPATH, null);
        assertFalse(fileSpec.validateEntry(dirAsFileEntry), "Directory should not be valid as IS_FILE");

        // Test Directory
        ConfigEntrySpecification dirSpec = scheme.getSpecification("myDir");
        ConfigEntry dirEntry = new org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf(
                "myDir", testDir, ConfigEntryType.FILEPATH, null);
        assertTrue(dirSpec.validateEntry(dirEntry), "Should be valid directory");

        ConfigEntry fileAsDirEntry = new org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf(
                "myDir", testFile, ConfigEntryType.FILEPATH, null);
        assertFalse(dirSpec.validateEntry(fileAsDirEntry), "File should not be valid as IS_DIRECTORY");
    }
}
