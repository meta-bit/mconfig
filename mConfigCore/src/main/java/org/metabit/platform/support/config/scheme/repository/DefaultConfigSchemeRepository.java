package org.metabit.platform.support.config.scheme.repository;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.NullConfigScheme;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ConfigSchemeRepository using ConcurrentHashMap.
 */
public class DefaultConfigSchemeRepository implements ConfigSchemeRepository
{
    private final Map<String, ConfigScheme> schemes = new ConcurrentHashMap<>();

    @Override
    public void registerScheme(String configName, ConfigScheme scheme)
        {
        if (configName == null || scheme == null)
            {
            return;
            }
        schemes.put(configName, scheme);
        }

    @Override
    public ConfigScheme getScheme(String configName)
        {
        if (configName == null)
            {
            return NullConfigScheme.INSTANCE;
            }
        return schemes.getOrDefault(configName, NullConfigScheme.INSTANCE);
        }

    @Override
    public Set<String> getRegisteredNames()
        {
        return Collections.unmodifiableSet(schemes.keySet());
        }
}
