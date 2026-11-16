package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.BootstrapConfigContext;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6 (MLIB-138), fix part: getConfig(name, schema) applies the requested schema even when an instance of that
 * name is cached; a cursor over a scope-restricted configuration sees only those scopes.
 */
class ConfigInstanceSchemaAndScopeTest
{
    private static ConfigFactory testFactory(String application)
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", application);
        builder.setTestMode(true);
        return builder.build();
        }

    /** core has no schema parser on its test classpath; a distinct, permissive schema instance is enough here. */
    private static ConfigSchema schema(ConfigFactory factory)
        {
        return new ConfigSchema()
            {
            @Override public boolean checkConfigEntryValidity(String fullKey, ConfigEntry entry) { return true; }
            @Override public void addSchemaEntry(String format1) { }
            @Override public void addSchemaEntry(String fullKey, ConfigEntryType type, String validitySchema, Object defaultValue, String description, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes) { }
            @Override public void addSchemaEntry(ConfigSchemaEntry entry) { }
            @Override public void init(ConfigFactoryInstanceContext ctx) { }
            @Override public void transferDefaults(DefaultLayer defaultLayer) { }
            @Override public Set<String> getEntryKeys() { return Collections.emptySet(); }
            @Override public ConfigEntrySpecification getSpecification(String fullKey) { return NullConfigEntrySpecification.INSTANCE; }
            };
        }

    @Test
    void sameSchemaReturnsTheCachedInstance() throws Exception
        {
        try (ConfigFactory factory = testFactory("M6_SAME_SCHEMA"))
            {
            ConfigSchema schema = schema(factory);
            Configuration first = factory.getConfig("network", schema);
            assertSame(first, factory.getConfig("network", schema));
            }
        }

    @Test
    void differentSchemaOnCacheHitIsApplied() throws Exception
        {
        try (ConfigFactory factory = testFactory("M6_OTHER_SCHEMA"))
            {
            Configuration plain = factory.getConfig("network");
            ConfigSchema schema = schema(factory);

            Configuration withSchema = factory.getConfig("network", schema);
            assertNotSame(plain, withSchema);
            assertSame(schema, withSchema.getConfigSchema());

            // the shared instance is unchanged and stays cached
            assertNotSame(schema, plain.getConfigSchema());
            assertSame(plain, factory.getConfig("network"));

            // closing the separate instance does not evict the cached one
            withSchema.close();
            assertSame(plain, factory.getConfig("network"));
            }
        }

    @Test
    void separateInstanceIsClosedWithTheFactory() throws Exception
        {
        Configuration withSchema;
        try (ConfigFactory factory = testFactory("M6_CLOSE"))
            {
            factory.getConfig("network");
            withSchema = factory.getConfig("network", schema(factory));
            }
        assertTrue(((LayeredConfiguration) withSchema).isClosed());
        }

    @Test
    void cursorOfRestrictedConfigSeesOnlyItsScopes() throws Exception
        {
        try (ConfigFactory factory = testFactory("M6_CURSOR"))
            {
            Configuration full = factory.getConfig("cursor");
            full.put("runtimeKey", "r", ConfigScope.RUNTIME);
            full.put("sessionKey", "s", ConfigScope.SESSION);

            Configuration restricted = ((BootstrapConfigContext) factory).getRestrictedConfig("cursor", EnumSet.of(ConfigScope.SESSION));
            assertEquals(Set.of("sessionKey"), topLevelKeys(restricted.getConfigCursor()));
            assertEquals(Set.of("runtimeKey", "sessionKey"), topLevelKeys(full.getConfigCursor()));
            }
        }

    private static Set<String> topLevelKeys(ConfigCursor cursor)
        {
        Set<String> keys = new HashSet<>();
        if (cursor.enter())
            {
            while (cursor.moveNext())
                { keys.add(cursor.getCurrentElement().getKey()); }
            }
        return keys;
        }
}
//___EOF___
