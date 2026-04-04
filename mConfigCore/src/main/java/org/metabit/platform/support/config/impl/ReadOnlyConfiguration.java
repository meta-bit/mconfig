package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;

/**
 * Base class for read-only configurations to avoid repeating UnsupportedOperationException.
 */
public abstract class ReadOnlyConfiguration extends AbstractConfiguration
{
    @Override
    public boolean isWriteable()
        {
        return false;
        }

    @Override
    protected void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope)
            throws ConfigCheckedException
        {
        throw new UnsupportedOperationException("ReadOnlyConfiguration");
        }

    @Override
    public int flush() throws ConfigCheckedException
        {
        return 0;
        }
}