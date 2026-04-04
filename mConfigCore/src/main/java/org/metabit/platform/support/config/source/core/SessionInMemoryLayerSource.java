package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.ConfigScope;

/**
 * InMemoryLayerSource for SESSION scope.
 */
public class SessionInMemoryLayerSource extends InMemoryLayerSource
{
    public SessionInMemoryLayerSource()
        {
        super(ConfigScope.SESSION);
        }
}
