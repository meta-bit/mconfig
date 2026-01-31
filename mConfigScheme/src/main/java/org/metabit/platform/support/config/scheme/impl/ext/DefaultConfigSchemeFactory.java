package org.metabit.platform.support.config.scheme.impl.ext;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;
import org.metabit.platform.support.config.scheme.ConfigSchemeFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Default implementation of ConfigSchemeFactory.
 */
public class DefaultConfigSchemeFactory implements ConfigSchemeFactory
{
    @Override
    public ConfigScheme createScheme()
        {
        return new ConfigSchemeImpl();
        }

    @Override
    public Map<String, ConfigScheme> createSchemesFromJSON(String json, org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext ctx) throws ConfigCheckedException
        {
        return ConfigSchemeImpl.fromJSON(json, ctx);
        }

    @Override
    public ConfigScheme createSchemeFromEntries(Set<ConfigSchemeEntry> entries) throws ConfigCheckedException
        {
        return ConfigSchemeImpl.fromEntries(entries);
        }

    @Override
    public ConfigSchemeEntry createEntry(String key, ConfigEntryType type)
        {
        return new ConfigSchemeEntry(key, type);
        }

    @Override
    public ConfigSchemeEntry createEntry(String key, ConfigEntryType type, String description, Object defaultValue, String validationPattern, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes)
        {
        ConfigSchemeEntry entry = new ConfigSchemeEntry(key, type);
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
    public boolean testSchemeJSON(String json)
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
    public void fillEntryFromCondensedForm(ConfigSchemeEntry entry, String condensedForm) throws ConfigCheckedException
        {
        throw new UnsupportedOperationException("Condensed form parsing removed. Use JSON.");
        }
}
