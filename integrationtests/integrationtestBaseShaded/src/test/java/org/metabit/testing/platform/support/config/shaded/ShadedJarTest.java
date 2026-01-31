package org.metabit.testing.platform.support.config.shaded;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShadedJarTest {

    @Test
    public void testConfigFactoryAvailable() {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "mConfigIT");
        assertNotNull(builder.build(), "ConfigFactory should be buildable");
    }

    @Test
    public void testExpectedStoragesAvailable() {
        ServiceLoader<ConfigStorageInterface> loader = ServiceLoader.load(ConfigStorageInterface.class);
        
        long count = StreamSupport.stream(loader.spliterator(), false).count();
        assertTrue(count > 0, "There should be at least one ConfigStorageInterface implementation");

        boolean hasFilesystem = StreamSupport.stream(loader.spliterator(), false)
                .anyMatch(s -> s.getClass().getName().contains("FileConfigStorage"));
        boolean hasJar = StreamSupport.stream(loader.spliterator(), false)
                .anyMatch(s -> s.getClass().getName().contains("JARConfigSource"));

        assertTrue(hasFilesystem, "FileConfigStorage should be available");
        assertTrue(hasJar, "JARConfigSource should be available");
    }
}
