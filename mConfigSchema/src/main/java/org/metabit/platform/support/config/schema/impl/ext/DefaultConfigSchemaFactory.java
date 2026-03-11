package org.metabit.platform.support.config.schema.impl.ext;

import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.schema.ConfigSchemaFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of ConfigSchemaFactory.
 */
public class DefaultConfigSchemaFactory implements ConfigSchemaFactory
{
    @Override
    public ConfigSchema createSchema()
        {
        return new ConfigSchemaImpl();
        }

    @Override
    public Map<String, ConfigSchema> createSchemasFromJSON(String json, org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx) throws ConfigCheckedException
        {
        return ConfigSchemaImpl.fromJSON(json, ctx);
        }

    @Override
    public ConfigSchema createSchemaFromEntries(Set<ConfigSchemaEntry> entries) throws ConfigCheckedException
        {
        return ConfigSchemaImpl.fromEntries(entries);
        }

    @Override
    public ConfigSchemaEntry createEntry(String key, ConfigEntryType type)
        {
        return new ConfigSchemaEntry(key, type);
        }

    @Override
    public ConfigSchemaEntry createEntry(String key, ConfigEntryType type, String description, Object defaultValue, String validationPattern, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes)
        {
        ConfigSchemaEntry entry = new ConfigSchemaEntry(key, type);
        if (description != null) entry.setDescription(description);
        if (flags != null) entry.setFlags(flags);
        if (scopes != null) entry.setScopes(scopes);
        if (validationPattern != null)
            {
            // We use the same Unicode-enabled regex by default as planned
            entry.setValidationPattern(validationPattern);
            }
        if (defaultValue != null)
            {
            try
                {
                entry.setDefault(String.valueOf(defaultValue));
                }
            catch (ConfigCheckedException e)
                {
                throw new ConfigException(e);
                }
            }
        return entry;
        }

    @Override
    public boolean testSchemaJSON(String json)
        {
        org.metabit.library.format.json.JsonStreamParser jsp = new org.metabit.library.format.json.JsonStreamParser();
        org.metabit.library.format.json.DummyJsonStreamConsumer duh = new org.metabit.library.format.json.DummyJsonStreamConsumer();
        try
            {
            jsp.parse(json, duh);
            return true;
            }
        catch (Exception e)
            {
            return false;
            }
        }

    @Override
    public void fillEntryFromCondensedForm(ConfigSchemaEntry entry, String condensedForm) throws ConfigCheckedException
        {
        throw new UnsupportedOperationException("Condensed form parsing removed. Use JSON.");
        }
}
