package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FlatBinaryTest {

    @TempDir
    Path tempDir;

    @Test
    void testFlatBinaryNoKeyLeakage() throws Exception {
        byte[] data = {1, 2, 3, 4, 5};
        Path binFile = tempDir.resolve("mybinary.bin");
        Files.write(binFile, data);

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test");
        builder.setTestConfigPaths(ConfigScope.USER, java.util.List.of(tempDir.toAbsolutePath().toString()));
        builder.setTestMode(true);
        ConfigFactory factory = builder.build();

        // access as a blob configuration
        Configuration cfg = factory.getConfig("mybinary");
        
        // Root entry (empty key) should be our blob
        ConfigEntry rootEntry = cfg.getConfigEntryFromFullKey("", java.util.EnumSet.allOf(ConfigScope.class));
        assertNotNull(rootEntry);
        assertArrayEquals(data, rootEntry.getValueAsBytes());
        assertEquals("", rootEntry.getKey());

        // Accessing any other key should return null (no leakage)
        ConfigEntry leakedEntry = cfg.getConfigEntryFromFullKey("someKey", java.util.EnumSet.allOf(ConfigScope.class));
        assertNull(leakedEntry, "Should not be able to access nested keys in a flat binary file");
    }
}
