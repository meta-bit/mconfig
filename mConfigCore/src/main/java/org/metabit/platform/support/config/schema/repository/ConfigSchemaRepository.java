package org.metabit.platform.support.config.schema.repository;

import org.metabit.platform.support.config.schema.ConfigSchema;
import java.util.Set;

/**
 * A central, thread-safe registry for all ConfigSchema objects.
 * Separates scheme management from the ConfigFactory and allows for better reuse.
 */
public interface ConfigSchemaRepository
{
    /**
     * Register a schema. If a schema with the same name exists, it follows the "Last-One-Wins" strategy.
     *
     * @param configName the name of the configuration
     * @param schema the schema to register
     */
    void registerSchema(String configName, ConfigSchema schema);

    /**
     * Retrieve a schema by name.
     *
     * @param configName the name of the configuration
     * @return the registered schema, or a NullConfigSchema if none is registered.
     */
    ConfigSchema getSchema(String configName);

    /**
     * @return all known configuration names.
     */
    Set<String> getRegisteredNames();
}
