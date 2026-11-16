package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.interfaces.SecretValue;

/**
 * Interface for components that can create secret configuration entries.
 */
public interface SecretEntryFactory
{
    /**
     * Create a secret configuration entry.
     * @param key the entry key
     * @param value the secret value
     * @param meta the entry metadata
     * @return the created entry
     */
    default ConfigEntry createSecretEntry(String key, SecretValue value, ConfigEntryMetadata meta)
    {
        return createSecretEntry(key, value, meta.getSource());
    }

    /**
     * Create a secret configuration entry.
     * @param key the entry key
     * @param value the secret value
     * @param source the config source
     * @return the created entry
     * @deprecated use {@link #createSecretEntry(String, SecretValue, ConfigEntryMetadata)}
     */
    @Deprecated
    ConfigEntry createSecretEntry(String key, SecretValue value, ConfigSource source);
}
//___EOF___
