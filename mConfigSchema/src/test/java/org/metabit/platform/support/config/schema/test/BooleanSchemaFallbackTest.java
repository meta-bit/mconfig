package org.metabit.platform.support.config.schema.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * strict Boolean conversion (0.8.17) under a BOOLEAN schema: the fallback chain, not an exception.
 * Here rather than in core: core has no schema implementation on its test class path.
 */
class BooleanSchemaFallbackTest
{
    private static ConfigFactory factory(String application)
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", application);
        builder.setTestMode(true);
        return builder.build();
        }

    /** with a BOOLEAN schema, an invalid value is skipped; the schema default applies. */
    @Test
    void invalidValueUnderABooleanSchemaFallsBackToTheDefault() throws Exception
        {
        try (ConfigFactory factory = factory("BOOL_SCHEMA"))
            {
            ConfigSchemaEntry entry = new ConfigSchemaEntry("enabled", ConfigEntryType.BOOLEAN).setDefault("on");
            ConfigSchema schema = ConfigSchema.fromSchemaEntries(Collections.singleton(entry));
            Configuration cfg = factory.getConfig("flags", schema);
            assertEquals(Boolean.TRUE, cfg.getBoolean("enabled"), "default \"on\" is a Boolean literal");

            cfg.put("enabled", "off", ConfigScope.RUNTIME);
            assertEquals(Boolean.FALSE, cfg.getBoolean("enabled"));

            ConfigException rejected = assertThrows(ConfigException.class, ()->cfg.put("enabled", "maybe", ConfigScope.RUNTIME), "writes are validated");
            assertEquals(Boolean.FALSE, cfg.getBoolean("enabled"), "the rejected write changed nothing");

            // a value the schema did not see on writing, e.g. from a file: skipped when read, the default applies
            Configuration unchecked = factory.getConfig("flags");
            unchecked.put("enabled", "maybe", ConfigScope.RUNTIME);
            assertEquals("maybe", unchecked.getString("enabled"), "precondition: the instances share the RUNTIME layer");
            assertEquals(Boolean.TRUE, cfg.getBoolean("enabled"), "skipped; the default applies");
            }
        }

}
//___EOF___
