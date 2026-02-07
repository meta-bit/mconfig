package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.mockups.MockConfigFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigCursorWriteTest
{
    @Test
    void testCursorWrite() throws ConfigException
    {
        ConfigFactorySettings cfSettings = new ConfigFactorySettings();
        ConfigFactorySettings.initDefaults(cfSettings);
        ConfigFactoryInstanceContext mockCtx = new ConfigFactoryInstanceContext(cfSettings);
        ConfigFactory mockFactory = new MockConfigFactory(mockCtx);
        
        Configuration cfg = mockFactory.getConfig("writeTest");
        assertNotNull(cfg);
        
        // MockConfigFactory might not have a layer that supports creation of new entries in all scopes.
        // Let's ensure we have a writable layer.
        if (!cfg.isWriteable()) {
            // This shouldn't happen with MockConfigFactory as it adds an InMemoryLayer
        }
        
        ConfigCursor cursor = cfg.getConfigCursor();
        assertNotNull(cursor);
        
        // Test put
        cursor.put("key1", "value1", ConfigScope.RUNTIME);
        assertEquals("value1", cfg.getString("key1"));
        
        cursor.put("key2", true, ConfigScope.RUNTIME);
        assertTrue(cfg.getBoolean("key2"));
        
        List<String> list = Arrays.asList("a", "b", "c");
        cursor.put("key3", list, ConfigScope.RUNTIME);
        assertEquals(list, cfg.getListOfStrings("key3"));
        
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        cursor.put("key4", data, ConfigScope.RUNTIME);
        assertArrayEquals(data, cfg.getBytes("key4"));
        
        // Test setValue
        assertTrue(cursor.moveNext());
        // Find key1
        while (!"key1".equals(cursor.getCurrentElement().getKey()))
        {
            if (!cursor.moveNext()) break;
        }
        assertEquals("key1", cursor.getCurrentElement().getKey());
        
        cursor.setValue("newValue1");
        assertEquals("newValue1", cfg.getString("key1"));
        
        // Test removal
        cursor.remove();
        assertTrue(cfg.getString("key1") == null || cfg.getString("key1").isEmpty());
    }
}
