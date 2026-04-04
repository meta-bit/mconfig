package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.ConfigScope;

/**
 * InMemoryLayerSource for RUNTIME scope.
 */
public class RuntimeInMemoryLayerSource extends InMemoryLayerSource
{
    public RuntimeInMemoryLayerSource()
        {
        super(ConfigScope.RUNTIME);
        }
}
