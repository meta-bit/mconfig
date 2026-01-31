package org.metabit.platform.support.config.scheme;

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
public interface ConfigSchemeFactory
{
    /**
     * @return a new ConfigSchemeFactory instance.
     */
    static ConfigSchemeFactory create()
        {
        java.util.ServiceLoader<ConfigSchemeFactory> loader = java.util.ServiceLoader.load(ConfigSchemeFactory.class);
        return loader.findFirst().orElseThrow(() -> new UnsupportedOperationException("ConfigSchemeFactory implementation not found. Please include mConfigScheme module."));
        }

    /**
     * Create a new, empty configuration scheme.
     * @return a new ConfigScheme instance.
     */
    ConfigScheme createScheme();

    /**
     * Create schemes from JSON.
     */
    default Map<String, ConfigScheme> createSchemesFromJSON(String json, org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx) throws org.metabit.platform.support.config.ConfigCheckedException
        { throw new UnsupportedOperationException(); }

    /**
     * Create a scheme from entries.
     */
    default ConfigScheme createSchemeFromEntries(Set<ConfigSchemeEntry> entries) throws org.metabit.platform.support.config.ConfigCheckedException
        { throw new UnsupportedOperationException(); }

    /**
     * Create a new configuration scheme entry.
     * @param key hierarchical key
     * @param type entry type
     * @return a new ConfigSchemeEntry instance.
     */
    ConfigSchemeEntry createEntry(String key, ConfigEntryType type);

    /**
     * Create a new configuration scheme entry with full details.
     * @param key hierarchical key
     * @param type entry type
     * @param description description or resource key
     * @param defaultValue default value
     * @param validationPattern validation pattern (regex, range, etc.)
     * @param flags flags for the entry
     * @param scopes valid scopes for the entry
     * @return a new ConfigSchemeEntry instance.
     */
    ConfigSchemeEntry createEntry(String key, ConfigEntryType type, String description, Object defaultValue, String validationPattern, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes);

    /**
     * Test if a JSON formatted scheme is valid.
     * @param json the scheme in JSON format
     * @return true if valid, false otherwise
     */
    default boolean testSchemeJSON(String json) { throw new UnsupportedOperationException(); }

    /**
     * Fill an entry from its condensed string representation.
     */
    default void fillEntryFromCondensedForm(ConfigSchemeEntry entry, String condensedForm) throws org.metabit.platform.support.config.ConfigCheckedException
        { throw new UnsupportedOperationException(); }
}
