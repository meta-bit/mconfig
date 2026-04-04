package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;

/**
 * Shared validation logic for adapter classes.
 */
class AdapterValidator
{
    static boolean validate(ConfigSchema scheme, String key, String strVal)
        {
        if (scheme == null) return true;
        ConfigEntrySpecification spec = scheme.getSpecification(key);
        if (spec == null) return true;
        try
            {
            ConfigEntryMetadata dummyMeta = new ConfigEntryMetadata((ConfigSource) null);
            ConfigEntry tempEntry = new GenericConfigEntryLeaf(key, strVal != null ? strVal : "", ConfigEntryType.STRING, dummyMeta);
            return scheme.checkConfigEntryValidity(key, tempEntry);
            }
        catch (Exception e)
            {
            throw new IllegalArgumentException("Configuration validation failed for key '"+key+"': "+strVal, e);
            }
        }
}