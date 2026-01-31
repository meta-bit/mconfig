package org.metabit.platform.support.config.util;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeFactory;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ConfigurationViewsTest {

    @Test
    public void testOverridingConfiguration() {
        Properties props = new Properties();
        props.setProperty("app.host", "localhost");
        props.setProperty("app.port", "8080");
        Configuration parent = ConfigUtil.fromProperties(props);

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("app.host", "override.com");
        overrides.put("app.port", 9090);
        overrides.put("app.debug", true);

        Configuration view = ConfigUtil.withOverrides(parent, overrides);

        assertEquals("override.com", view.getString("app.host"));
        assertEquals(9090, view.getInteger("app.port"));
        assertTrue(view.getBoolean("app.debug"));
        assertNull(view.getString("missing")); // parent null, override null -> null

        Set<String> keys = view.getAllConfigurationKeysFlattened(ConfigUtil.ALL_SCOPES);
        assertTrue(keys.contains("app.host"));
        assertTrue(keys.contains("app.debug"));
    }

    @Test
    public void testRemappedConfiguration() {
        Properties props = new Properties();
        props.setProperty("mysql.host", "localhost");
        props.setProperty("mysql.port", "3306");
        props.setProperty("other.key", "value");
        Configuration source = ConfigUtil.fromProperties(props);

        Configuration view = ConfigUtil.remapped(source, "mysql.", "db.");

        assertEquals("localhost", view.getString("db.host"));
        assertEquals(3306, view.getInteger("db.port"));
        assertEquals("value", view.getString("other.key")); // passthru

        Set<String> keys = view.getAllConfigurationKeysFlattened(ConfigUtil.ALL_SCOPES);
        assertTrue(keys.contains("db.host"));
        assertTrue(keys.contains("other.key"));
        assertFalse(keys.contains("mysql.host")); // projected
    }

    @Test
    public void testPropertiesConfiguration() {
        Properties props = new Properties();
        props.setProperty("str", "hello");
        props.setProperty("int", "42");
        props.setProperty("bool", "true");
        props.setProperty("invalid.int", "abc");
        props.setProperty("list", "a,b , c");

        Configuration view = ConfigUtil.fromProperties(props);

        assertEquals("hello", view.getString("str"));
        assertEquals(42, view.getInteger("int"));
        assertTrue(view.getBoolean("bool"));
        assertNull(view.getInteger("invalid.int")); // graceful
        assertEquals(Arrays.asList("a", "b", "c"), view.getListOfStrings("list"));

        Set<String> keys = view.getAllConfigurationKeysFlattened(ConfigUtil.ALL_SCOPES);
        assertEquals(props.stringPropertyNames(), keys);
    }

    @Test
    public void testChaining() {
        Properties props = new Properties();
        props.setProperty("mysql.host", "db1");
        Configuration source = ConfigUtil.fromProperties(props);

        Configuration remapped = ConfigUtil.remapped(source, "mysql.", "db.");
        Map<String, Object> overrides = Map.of("db.host", "db2");

        Configuration chained = ConfigUtil.withOverrides(remapped, overrides);

        assertEquals("db2", chained.getString("db.host")); // override > remap > source
    }

    // TODO: scheme validation test (requires scheme setup)
}