package org.metabit.platform.support.config.schema;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.util.Collections;
import java.util.Set;

/**
 * A permissive implementation of ConfigSchema that allows any entry.
 * Implemented as a singleton to minimize memory usage.
 */
public final class NullConfigSchema implements ConfigSchema
{
    public static final NullConfigSchema INSTANCE = new NullConfigSchema();

    private NullConfigSchema() { }

    @Override
    public boolean checkConfigEntryValidity(String fullKey, ConfigEntry entry)
    {
        return true;
    }

    @Override
    public void addSchemaEntry(String format1)
    {
        // No-op for permissive scheme
    }

    @Override
    public void addSchemaEntry(String fullKey, org.metabit.platform.support.config.ConfigEntryType type, String validitySchema, Object defaultValue, String description, java.util.EnumSet<org.metabit.platform.support.config.ConfigEntry.ConfigEntryFlags> flags, java.util.EnumSet<org.metabit.platform.support.config.ConfigScope> scopes)
    {
        // No-op for permissive scheme
    }

    @Override
    public void addSchemaEntry(ConfigSchemaEntry entry)
    {
        // No-op for permissive scheme
    }

    @Override
    public void init(ConfigFactoryInstanceContext ctx)
    {
        // No-op for permissive scheme
    }

    @Override
    public void transferDefaults(DefaultLayer defaultLayer)
    {
        // No-op for permissive scheme
    }

    @Override
    public Set<String> getEntryKeys()
    {
        return Collections.emptySet();
    }

    @Override
    public ConfigEntrySpecification getSpecification(String fullKey)
    {
        return NullConfigEntrySpecification.INSTANCE;
    }

    @Override
    public boolean isNullSchema()
    {
        return true;
    }
}
