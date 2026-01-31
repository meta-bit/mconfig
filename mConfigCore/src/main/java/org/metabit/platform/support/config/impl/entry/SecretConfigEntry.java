package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.interfaces.SecretValue;

/**
 * Interface for secret-aware configuration entries.
 */
public interface SecretConfigEntry extends org.metabit.platform.support.config.ConfigEntry
{
    /**
     * @return the secret value contained in this entry.
     */
    SecretValue getSecretValue();
}
