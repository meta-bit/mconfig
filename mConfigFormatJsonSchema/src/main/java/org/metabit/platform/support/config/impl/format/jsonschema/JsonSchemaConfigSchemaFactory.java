package org.metabit.platform.support.config.impl.format.jsonschema;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.ConfigSchemaFactory;

import java.util.EnumSet;

public class JsonSchemaConfigSchemaFactory implements ConfigSchemaFactory
{
    @Override
    public ConfigSchema createSchema()
        {
        return null; // Not applicable for standard JSON schema
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
        if (defaultValue != null)
            {
            try { entry.setDefault(String.valueOf(defaultValue)); } catch (Exception ignored) { }
            }
        if (validationPattern != null) entry.setValidationPattern(validationPattern);
        if (flags != null) entry.setFlags(flags);
        if (scopes != null) entry.setScopes(scopes);
        return entry;
        }
}
