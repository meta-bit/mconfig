package org.metabit.platform.support.config.mapper;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * test the mapping with a test config (placed in the resources folder)
 */
public class ConfigCursorMappingTest
{

    @Test
    public void testConfigCursorMapping()
    {
    // 1. Build the factory
    try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApp")
            .setFeature(ConfigFeature.TEST_MODE, true)
            .build())
        {
        // 2. Get the configuration
        Configuration cfg = factory.getConfig("myConfig");

        // 3. Obtain the cursor
        ConfigCursor cursor = cfg.getConfigCursor();

        // 4. Use the cursor (e.g., for POJO mapping)
        TestPojo pojo = new TestPojo();
        int mapped = cursor.copyMapToObject(pojo, "set", "");

        assertEquals(3, mapped);
        assertEquals("John Doe", pojo.getName());
        assertEquals(30, pojo.getAge());
        assertTrue(pojo.isActive());
        }
    }
}
