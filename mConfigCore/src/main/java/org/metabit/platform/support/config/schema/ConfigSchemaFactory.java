package org.metabit.platform.support.config.schema;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigScope;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Factory for creating configuration schemes and entries.
 * This provides a clean API for defining configuration contracts.
 */
public interface ConfigSchemaFactory
{
    /**
     * @return a new ConfigSchemaFactory instance.
     */
    static ConfigSchemaFactory create()
        {
        java.util.ServiceLoader<ConfigSchemaFactory> loader = java.util.ServiceLoader.load(ConfigSchemaFactory.class);
        return loader.findFirst().orElseThrow(() -> new UnsupportedOperationException("ConfigSchemaFactory implementation not found. Please include mConfigSchema module."));
        }

    /**
     * Create a new, empty configuration scheme.
     * @return a new ConfigSchema instance.
     */
    ConfigSchema createSchema();

    /**
     * Create schemas from JSON.
     */
    default Map<String, ConfigSchema> createSchemasFromJSON(String json, org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx) throws org.metabit.platform.support.config.ConfigCheckedException
        { throw new UnsupportedOperationException(); }

    /**
     * Create a schema from entries.
     */
    default ConfigSchema createSchemaFromEntries(Set<ConfigSchemaEntry> entries) throws org.metabit.platform.support.config.ConfigCheckedException
        { throw new UnsupportedOperationException(); }

    /**
     * Create a new configuration scheme entry.
     * @param key hierarchical key
     * @param type entry type
     * @return a new ConfigSchemaEntry instance.
     */
    ConfigSchemaEntry createEntry(String key, ConfigEntryType type);

    /**
     * Create a new configuration scheme entry with full details.
     * @param key hierarchical key
     * @param type entry type
     * @param description description or resource key
     * @param defaultValue default value
     * @param validationPattern validation pattern (regex, range, etc.)
     * @param flags flags for the entry
     * @param scopes valid scopes for the entry
     * @return a new ConfigSchemaEntry instance.
     */
    ConfigSchemaEntry createEntry(String key, ConfigEntryType type, String description, Object defaultValue, String validationPattern, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes);

    /**
     * Test if a JSON formatted schema is valid.
     * @param json the schema in JSON format
     * @return true if valid, false otherwise
     */
    default boolean testSchemaJSON(String json) { throw new UnsupportedOperationException(); }

    /**
     * Fill an entry from its condensed string representation.
     */
    default void fillEntryFromCondensedForm(ConfigSchemaEntry entry, String condensedForm) throws org.metabit.platform.support.config.ConfigCheckedException
        { throw new UnsupportedOperationException(); }
}
