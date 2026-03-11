package org.metabit.platform.support.config.schema.repository;

import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.NullConfigSchema;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ConfigSchemaRepository using ConcurrentHashMap.
 */
public class DefaultConfigSchemaRepository implements ConfigSchemaRepository
{
    private final Map<String, ConfigSchema> schemas = new ConcurrentHashMap<>();

    @Override
    public void registerSchema(String configName, ConfigSchema schema)
        {
        if (configName == null || schema == null)
            {
            return;
            }
        schemas.put(configName, schema);
        }

    @Override
    public ConfigSchema getSchema(String configName)
        {
        if (configName == null)
            {
            return NullConfigSchema.INSTANCE;
            }
        return schemas.getOrDefault(configName, NullConfigSchema.INSTANCE);
        }

    @Override
    public Set<String> getRegisteredNames()
        {
        return Collections.unmodifiableSet(schemas.keySet());
        }
}
