package org.metabit.platform.support.config.schema.provider;

import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.schema.ConfigSchema;
import java.util.Map;

/**
 * Interface for discovering configuration schemes.
 * Implementations can discover schemes from classpath, filesystem, or external registries.
 */
public interface ConfigSchemaProvider
{
    /**
     * Discover schemes.
     *
     * @param ctx the factory instance context
     * @return a map of named schemes (key is config name, value is scheme)
     */
    Map<String, ConfigSchema> discoverSchemas(ConfigFactoryInstanceContext ctx);
}
