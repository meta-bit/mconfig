package org.metabit.platform.support.config;

/**
 * Interface for configuration features.
 * Allows extending the library with module-specific features while maintaining type safety.
 */
public interface ConfigFeatureInterface
{
    /**
     * Get the unique name of the feature.
     * @return the feature name (usually UPPER_SNAKE_CASE)
     */
    String name();

    /**
     * Get the type of the value this feature accepts.
     * @return the value type
     */
    ConfigFeature.ValueType getValueType();

    /**
     * Get the class type for SPECIAL_CLASS features.
     * @return the class type, or null if not a special class
     */
    Class<?> getClassType();

    /**
     * Get the default value for this feature.
     * @return the default value, or null if none
     */
    Object getDefaultValue();

}
