package org.metabit.platform.support.config.scheme;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;

/**
 * A permissive implementation of ConfigEntrySpecification that allows any entry.
 * Implemented as a singleton to minimize memory usage.
 * (NullObject pattern)
 */
public final class NullConfigEntrySpecification implements ConfigEntrySpecification
{
    public static final NullConfigEntrySpecification INSTANCE = new NullConfigEntrySpecification();

    private NullConfigEntrySpecification() { }

    @Override
    public String getKey()
    {
        return "";
    }

    @Override
    public ConfigEntryType getType()
    {
        return ConfigEntryType.STRING;
    }

    @Override
    public String getValueLimitations()
    {
        return null;
    }

    @Override
    public ConfigEntry getDefaultEntry()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public boolean isMandatory()
    {
        return false;
    }

    @Override
    public boolean isSecret()
    {
        return false;
    }

    @Override
    public boolean validateEntry(ConfigEntry entry)
    {
        return true;
    }

    /**
     * Checks whether the configuration entry is marked as hidden from
     * automatic documentation (e.g. commandline --help features)
     *
     * @return true if the configuration entry is hidden, false otherwise.
     */
    @Override
    public boolean isHidden()
        { return false; }
}
