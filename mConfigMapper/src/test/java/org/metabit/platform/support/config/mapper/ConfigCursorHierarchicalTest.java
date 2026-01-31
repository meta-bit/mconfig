package org.metabit.platform.support.config.mapper;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigCursorHierarchicalTest {

    @Test
    public void testHierarchicalNavigation() throws ConfigException, ConfigCheckedException {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApp")
                .setFeature(ConfigFeature.TEST_MODE, true)
                .build()) {
            Configuration cfg = factory.getConfig("myHierarchicalConfig");
            ConfigCursor cursor = cfg.getConfigCursor();

            // At root
            assertTrue(cursor.isOnMap(), "Should be on map at root");
            assertFalse(cursor.isOnLeaf(), "Should not be on leaf at root");
            assertTrue(cursor.canEnter(), "Should be able to enter at root");

            assertTrue(cursor.enter(), "Should be able to enter root map");
            
            boolean foundInputDatabase = false;
            while (cursor.moveNext()) {
                ConfigEntry entry = cursor.getCurrentElement();
                if ("inputDatabase".equals(entry.getKey())) {
                    foundInputDatabase = true;
                    assertTrue(cursor.isOnMap(), "inputDatabase should be a map");
                    assertFalse(cursor.isOnLeaf(), "inputDatabase should not be a leaf");
                    assertTrue(cursor.canEnter(), "Should be able to enter inputDatabase");
                    
                    assertTrue(cursor.enter(), "Should be able to enter inputDatabase map");
                    boolean foundPort = false;
                    while (cursor.moveNext()) {
                        ConfigEntry innerEntry = cursor.getCurrentElement();
                        if ("port".equals(innerEntry.getKey())) {
                            foundPort = true;
                            assertTrue(cursor.isOnLeaf(), "port should be a leaf");
                            assertEquals(1234, innerEntry.getValueAsInteger());
                        }
                    }
                    assertTrue(foundPort, "Should have found port inside inputDatabase");
                    assertTrue(cursor.leave(), "Should be able to leave inputDatabase");
                    assertEquals("inputDatabase", cursor.getCurrentElement().getKey(), "After leave, should be back on inputDatabase");
                }
            }
            assertTrue(foundInputDatabase, "Should have found inputDatabase");
        }
    }
}
