package org.metabit.platform.support.config.scheme.provider;

import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import java.util.Map;

/**
 * Interface for discovering configuration schemes.
 * Implementations can discover schemes from classpath, filesystem, or external registries.
 */
public interface ConfigSchemeProvider
{
    /**
     * Discover schemes.
     *
     * @param ctx the factory instance context
     * @return a map of named schemes (key is config name, value is scheme)
     */
    Map<String, ConfigScheme> discoverSchemes(ConfigFactoryInstanceContext ctx);
}
