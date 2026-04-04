package org.metabit.platform.support.config;

import java.util.Objects;

/**
 * Basic implementation of ConfigFeatureInterface.
 * Implements equals and hashCode based on the feature name to allow
 * different modules to define the same feature and have it resolve to the same value.
 */
public class ConfigFeatureBase implements ConfigFeatureInterface
{
    private final String name;
    private final ConfigFeature.ValueType valueType;
    private final Class<?> classType;
    private final Object defaultValue;

    public ConfigFeatureBase(String name, ConfigFeature.ValueType valueType)
    {
        this(name, valueType, null, null);
    }

    public ConfigFeatureBase(String name, ConfigFeature.ValueType valueType, Object defaultValue)
    {
        this(name, valueType, null, defaultValue);
    }

    public ConfigFeatureBase(String name, ConfigFeature.ValueType valueType, Class<?> classType, Object defaultValue)
    {
        this.name = name;
        this.valueType = valueType;
        this.classType = classType;
        this.defaultValue = defaultValue;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public ConfigFeature.ValueType getValueType()
    {
        return valueType;
    }

    @Override
    public Class<?> getClassType()
    {
        return classType;
    }

    @Override
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ConfigFeatureInterface)) return false;
        ConfigFeatureInterface that = (ConfigFeatureInterface) o;
        return Objects.equals(name, that.name());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
