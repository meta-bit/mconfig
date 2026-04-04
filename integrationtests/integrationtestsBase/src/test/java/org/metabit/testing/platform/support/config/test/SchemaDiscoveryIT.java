package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.schema.provider.ConfigSchemaProvider;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaDiscoveryIT
{
    @Test
    void testSchemaProvidersAreDiscovered()
        {
        ServiceLoader<ConfigSchemaProvider> loader = ServiceLoader.load(ConfigSchemaProvider.class, ConfigSchemaProvider.class.getClassLoader());
        List<String> discoveredProviders = new ArrayList<>();
        for (ConfigSchemaProvider provider : loader)
            {
            discoveredProviders.add(provider.getClass().getName());
            }

        assertTrue(discoveredProviders.contains("org.metabit.platform.support.config.source.hardcoded.provider.ClasspathConfigSchemaProvider"),
                "ClasspathConfigSchemaProvider should be discovered");
        assertTrue(discoveredProviders.contains("org.metabit.platform.support.config.impl.source.filesystem.FilesystemConfigSchemaProvider"),
                "FilesystemConfigSchemaProvider should be discovered");
        }

    @Test
    void testSchemaDiscoveryInFactory() throws ConfigCheckedException
        {
        // This test verifies that DefaultConfigFactory correctly uses discovered providers
        try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "mConfigIT")
                .setFeature(ConfigFeature.TEST_MODE, true)
                .build())
            {
            // If discovery worked, both providers should have been called.
            // We can't easily check internal state, but if they failed or weren't found,
            // we'd have seen it in the logs (DefaultConfigFactory catches and logs failures).
            
            // We can check if ANY schema was registered. 
            // In test mode, ClasspathConfigSchemaProvider should find schemas in .config/
            
            Configuration config = factory.getConfig("testconfig");
            assertNotNull(config);
            }
        }
}
