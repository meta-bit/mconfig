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
     * @param source the config source
     * @return the created entry
     */
    ConfigEntry createSecretEntry(String key, SecretValue value, ConfigSource source);
}
