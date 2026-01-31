package org.metabit.platform.support.config.impl.test;
import org.metabit.platform.support.config.scheme.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.scheme.impl.ext.*;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.impl.ext.JsonConfigSchemeParser;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCursorListTest {

    @Test
    public void testListNavigation() throws ConfigCheckedException {
        ConfigFactory factory = ConfigFactoryBuilder.create("company", "app")
                .setFeature(ConfigFeature.PERMIT_TEST_MODE, true)
                .build();
        LayeredConfiguration cfg = (LayeredConfiguration) factory.getConfig("test");
        ConfigFactoryInstanceContext ctx = ((DefaultConfigFactory) factory).ctx;
        
        // Ensure a writeable layer exists
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.RUNTIME, new org.metabit.platform.support.config.source.core.InMemoryLayerSource(), null, null);
        cfg.add(new org.metabit.platform.support.config.source.core.InMemoryLayer(ctx, location, ConfigScope.RUNTIME), location);
        
        // Add a list entry
        cfg.put("servers", Arrays.asList("srv1", "srv2", "srv3"), ConfigScope.RUNTIME);
        
        ConfigCursor cursor = cfg.getConfigCursor();
        assertTrue(cursor.moveTo("servers"));
        assertTrue(cursor.isOnList());
        // A list entry is still a leaf node in the main tree
        assertTrue(cursor.isOnLeaf());
        
        assertTrue(cursor.enter());
        
        // Items in list
        assertTrue(cursor.moveNext());
        assertEquals("0", cursor.getCurrentElement().getKey());
        assertEquals("srv1", cursor.getCurrentElement().getValueAsString());
        
        assertTrue(cursor.moveNext());
        assertEquals("1", cursor.getCurrentElement().getKey());
        assertEquals("srv2", cursor.getCurrentElement().getValueAsString());
        
        assertTrue(cursor.moveNext());
        assertEquals("2", cursor.getCurrentElement().getKey());
        assertEquals("srv3", cursor.getCurrentElement().getValueAsString());
        
        assertFalse(cursor.moveNext());
        
        assertTrue(cursor.leave());
        assertEquals("servers", cursor.getCurrentElement().getKey());
        assertTrue(cursor.isOnList());
    }

    @Test
    public void testEnumSetNavigation() throws ConfigCheckedException {
        String schemeJson = "{" +
                "  \"KEY\": \"modes\"," +
                "  \"TYPE\": \"ENUM_SET\"," +
                "  \"PATTERN\": \"A|B|C\"" +
                "}";
        
        ConfigFactory factory = ConfigFactoryBuilder.create("company", "app")
                .setFeature(ConfigFeature.PERMIT_TEST_MODE, true)
                .build();
        factory.addConfigScheme("test", schemeJson);
        LayeredConfiguration cfg = (LayeredConfiguration) factory.getConfig("test");
        ConfigFactoryInstanceContext ctx = ((DefaultConfigFactory) factory).ctx;

        // Ensure a writeable layer exists
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.RUNTIME, new org.metabit.platform.support.config.source.core.InMemoryLayerSource(), null, null);
        cfg.add(new org.metabit.platform.support.config.source.core.InMemoryLayer(ctx, location, ConfigScope.RUNTIME), location);

        cfg.put("modes", Arrays.asList("A", "C"), ConfigScope.RUNTIME);
        
        ConfigCursor cursor = cfg.getConfigCursor();
        assertTrue(cursor.moveTo("modes"));
        assertTrue(cursor.isOnList());
        
        assertTrue(cursor.enter());
        
        assertTrue(cursor.moveNext());
        assertEquals(ConfigEntryType.ENUM, cursor.getCurrentElement().getType());
        assertEquals("A", cursor.getCurrentElement().getValueAsString());
        
        assertTrue(cursor.moveNext());
        assertEquals("C", cursor.getCurrentElement().getValueAsString());
        
        assertTrue(cursor.leave());
    }
}
