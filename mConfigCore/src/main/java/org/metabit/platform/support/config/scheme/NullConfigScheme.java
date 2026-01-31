package org.metabit.platform.support.config.scheme;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.util.Collections;
import java.util.Set;

/**
 * A permissive implementation of ConfigScheme that allows any entry.
 * Implemented as a singleton to minimize memory usage.
 */
public final class NullConfigScheme implements ConfigScheme
{
    public static final NullConfigScheme INSTANCE = new NullConfigScheme();

    private NullConfigScheme() { }

    @Override
    public boolean checkConfigEntryValidity(String fullKey, ConfigEntry entry)
    {
        return true;
    }

    @Override
    public void addSchemeEntry(String format1)
    {
        // No-op for permissive scheme
    }

    @Override
    public void addSchemeEntry(String fullKey, org.metabit.platform.support.config.ConfigEntryType type, String validityScheme, Object defaultValue, String description, java.util.EnumSet<org.metabit.platform.support.config.ConfigEntry.ConfigEntryFlags> flags, java.util.EnumSet<org.metabit.platform.support.config.ConfigScope> scopes)
    {
        // No-op for permissive scheme
    }

    @Override
    public void addSchemeEntry(ConfigSchemeEntry entry)
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
    public boolean isNullScheme()
    {
        return true;
    }
}
