package org.metabit.platform.support.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all configuration features (core and extension).
 * Supports case-insensitive lookup and provides type information for validation.
 */
public final class ConfigFeatureRegistry
{
    private static final Map<String, ConfigFeatureInterface> registry = new ConcurrentHashMap<>();

    private ConfigFeatureRegistry() {}

    /**
     * Register a configuration feature.
     * @param feature the feature to register
     */
    public static void register(ConfigFeatureInterface feature)
    {
        String normalizedName = normalize(feature.name());
        registry.put(normalizedName, feature);
    }

    /**
     * Get a registered configuration feature by name.
     * @param name the name of the feature (case-insensitive)
     * @return the feature, or null if not found
     */
    public static ConfigFeatureInterface get(String name)
    {
        if (name == null) return null;
        return registry.get(normalize(name));
    }

    /**
     * Get all registered configuration features.
     * @return a map of all registered features (normalized name to feature)
     */
    public static Map<String, ConfigFeatureInterface> getAll()
    {
        return new ConcurrentHashMap<>(registry);
    }

    private static String normalize(String name)
    {
        return name.toUpperCase().replace("_", "");
    }
}
