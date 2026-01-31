package org.metabit.platform.support.config.scheme.repository;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import java.util.Set;

/**
 * A central, thread-safe registry for all ConfigScheme objects.
 * Separates scheme management from the ConfigFactory and allows for better reuse.
 */
public interface ConfigSchemeRepository
{
    /**
     * Register a scheme. If a scheme with the same name exists, it follows the "Last-One-Wins" strategy.
     *
     * @param configName the name of the configuration
     * @param scheme the scheme to register
     */
    void registerScheme(String configName, ConfigScheme scheme);

    /**
     * Retrieve a scheme by name.
     *
     * @param configName the name of the configuration
     * @return the registered scheme, or a NullConfigScheme if none is registered.
     */
    ConfigScheme getScheme(String configName);

    /**
     * @return all known configuration names.
     */
    Set<String> getRegisteredNames();
}
