/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * configuration for all the settings a ConfigFactory can have.
 * The possible keys are listed in the ConfigFactory.Feature enum.
 * This is providing type-safe access for these features, to fail early if something is amiss.
 * the getters will return null if feature is not set, except for the boolean (default false).
 *
 * @version $Id: $Id
 */
public class ConfigFactorySettings
{
    public static final  Pattern validPathPatternAllowsEmpty = Pattern.compile("^[\\w\\s\\-.]*(?:/(?:\\w|\\s|-|\\.)+)*$"); // word-letters and spaces, with slashes allowed.
    public static final  Pattern validPathPattern            = Pattern.compile("^(?:\\w|\\s|-|\\.)+(?:/(?:\\w|\\s|-|\\.)+)*$"); // word-letters and spaces, with slashes allowed.
    public static final  Pattern validIdPattern              = Pattern.compile("^[\\w\\s.-]+$"); // simple ID, no slashes or dots traversal.
    private static final String  TYPE_MISMATCH_MSG           = "type mismatch: trying to set {0} with {1} value; but its type is {2}";

    private final Map<ConfigFeatureInterface, Object> settings    = new HashMap<>();
    private final Map<String, Object>                 rawSettings = new HashMap<>();
    private final Map<Path, ConfigScope>              directories = new HashMap<>();

    /**
     * <p>Constructor for ConfigFactorySettings.</p>
     */
    public ConfigFactorySettings() { initDefaults(this); }

    /**
     * <p>initDefaults.</p>
     *
     * @param cfs a {@link org.metabit.platform.support.config.impl.ConfigFactorySettings} object
     */
    public static void initDefaults(ConfigFactorySettings cfs)
        {
        for (ConfigFeatureInterface feature : ConfigFeatureRegistry.getAll().values())
            {
            Object o = feature.getDefaultValue();
            if (o != null)
                {
                cfs.putInternal(feature, o);
                }
            }
        }

    private void putInternal(ConfigFeatureInterface feature, Object value)
        {
        settings.put(feature, value);
        }

    private Object get(ConfigFeatureInterface feature)
        {
        return settings.get(feature);
        }

    /**
     * Compatibility with Map interface (previously inherited)
     */
    public Object get(Object key)
        {
        if (key instanceof ConfigFeatureInterface)
            {
            return settings.get((ConfigFeatureInterface) key);
            }
        return null;
        }

    /**
     * <p>isSet.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @return a boolean
     */
    public boolean isSet(final ConfigFeature feature)
        {
        return isSet((ConfigFeatureInterface) feature);
        }

    /**
     * Check if a feature is set by interface.
     */
    public boolean isSet(final ConfigFeatureInterface feature)
        {
        if (feature.getValueType() == ConfigFeature.ValueType.BOOLEAN)
            return true; // booleans always have a value (default is "false")
        return this.get(feature) != null;
        }

    /**
     * Check if a feature is set by name.
     *
     * @param featureName the name of the feature
     * @return true if set
     */
    public boolean isSet(final String featureName)
        {
        ConfigFeatureInterface feature = ConfigFeatureRegistry.get(featureName);
        if (feature != null)
            {
            return isSet(feature);
            }
        return rawSettings.containsKey(featureName);
        }

    private <T> T getFeatureTyped(ConfigFeatureInterface feature, java.util.function.Function<Object, T> converter, ConfigFeature.ValueType expectedType)
        {
        if (feature.getValueType() != expectedType)
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature.name(), expectedType.name(), feature.getValueType().name()));
        Object v = get(feature);
        if (v == null) v = feature.getDefaultValue();
        if (v == null) return null;
        return converter.apply(v);
        }

    public Boolean getBoolean(final ConfigFeature feature) { return getBoolean((ConfigFeatureInterface) feature); }
    public Boolean getBoolean(final ConfigFeatureInterface feature)
        {
        Boolean b = getFeatureTyped(feature, v -> (Boolean) v, ConfigFeature.ValueType.BOOLEAN);
        if (b != null) return b;
        Object def = feature.getDefaultValue();
        if (def instanceof Boolean) return (Boolean) def;
        return Boolean.FALSE;
        }

    public String getString(final ConfigFeature feature) { return getString((ConfigFeatureInterface) feature); }
    public String getString(final ConfigFeatureInterface feature)
        { return getFeatureTyped(feature, String::valueOf, ConfigFeature.ValueType.STRING); }

    public Integer getInteger(final ConfigFeature feature) { return getInteger((ConfigFeatureInterface) feature); }
    public Integer getInteger(final ConfigFeatureInterface feature)
        {
        Integer i = getFeatureTyped(feature, v -> (Integer) v, ConfigFeature.ValueType.NUMBER);
        if (i != null) return i;
        Object def = feature.getDefaultValue();
        if (def instanceof Integer) return (Integer) def;
        return 0;
        }

    public List<String> getStrings(final ConfigFeature feature) { return getStrings((ConfigFeatureInterface) feature); }
    public List<String> getStrings(final ConfigFeatureInterface feature)
        {
        if (feature.getValueType() != ConfigFeature.ValueType.STRINGLIST)
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature.name(), "List of Strings", feature.getValueType().name()));
        Object value = get(feature);
        if (value == null) return Collections.emptyList();
        if (!(value instanceof List)) return Collections.singletonList(String.valueOf(value));
        List<String> out = new ArrayList<>();
        for (Object e : (List<?>) value) out.add(String.valueOf(e));
        return Collections.unmodifiableList(out);
        }

    /**
     * <p>getObject.</p>
     */
    public <T> T getObject(final ConfigFeature feature, final Class<T> expectedClass)
        {
        return getObject((ConfigFeatureInterface) feature, expectedClass);
        }

    /**
     * Get an object feature value by interface.
     */
    public <T> T getObject(final ConfigFeatureInterface feature, final Class<T> expectedClass)
        {
        if (feature == null || expectedClass == null)
            throw new IllegalArgumentException("feature and expectedClass must not be null");

        if (feature.getValueType() != ConfigFeature.ValueType.SPECIAL_CLASS)
            throw new IllegalArgumentException(feature.name()+" is asked to get a special object value, but entry doesn't have that type");

        Object value = get(feature);
        if (value == null)
            return null;

        if (expectedClass.isInstance(value))
            return expectedClass.cast(value);

        if (feature.getClassType() != null && expectedClass.isAssignableFrom(feature.getClassType()))
            return expectedClass.cast(value);

        throw new IllegalArgumentException(
                feature.name()+" is asked to get a special object value of type "+expectedClass.getCanonicalName()+
                        ", but stored value is of type "+value.getClass().getCanonicalName());
        }

    /**
     * <p>getStringMap.</p>
     */
    public Map<String, String> getStringMap(final ConfigFeature feature)
        {
        return getStringMap((ConfigFeatureInterface) feature);
        }

    /**
     * Get a string map feature value by interface.
     */
    public Map<String, String> getStringMap(final ConfigFeatureInterface feature)
        {
        if (feature.getValueType() != ConfigFeature.ValueType.SPECIAL_CLASS)
            throw new IllegalArgumentException(feature.name()+" is asked for Map<String,String>, but entry doesn't have SPECIAL_CLASS type");

        Object value = get(feature);
        if (value == null)
            return Collections.emptyMap();

        if (!(value instanceof Map))
            throw new IllegalArgumentException(feature.name()+" is expected to hold Map<String,String> but stored type is "+value.getClass().getCanonicalName());

        Map<?, ?> raw = (Map<?, ?>) value;
        Map<String, String> result = new HashMap<>(raw.size());
        for (Map.Entry<?, ?> e : raw.entrySet())
            {
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String))
                throw new IllegalArgumentException(feature.name()+" must be a Map<String,String>, but found entry type "+
                        (e.getKey() == null ? "null" : e.getKey().getClass().getSimpleName())+"->"+
                        (e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName()));
            result.put((String) e.getKey(), (String) e.getValue());
            }
        return Collections.unmodifiableMap(result);
        }

    private void setFeatureTyped(ConfigFeatureInterface feature, Object value, ConfigFeature.ValueType expectedType)
        {
        if (feature.getValueType() != expectedType)
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature.name(), expectedType.name(), feature.getValueType().name()));
        putInternal(feature, value);
        }

    public void setBoolean(final ConfigFeature feature, final Boolean value) { setBoolean((ConfigFeatureInterface) feature, value); }
    public void setBoolean(final ConfigFeatureInterface feature, final Boolean value)
        { setFeatureTyped(feature, value, ConfigFeature.ValueType.BOOLEAN); }

    public void setString(final ConfigFeature feature, final String value) { setString((ConfigFeatureInterface) feature, value); }
    public void setString(final ConfigFeatureInterface feature, final String value)
        {
        if (feature.getValueType() != ConfigFeature.ValueType.STRING)
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature.name(), "String", feature.getValueType().name()));
        if (value == null) throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);

        if (feature instanceof ConfigFeature)
            {
            switch ((ConfigFeature) feature)
                {
                case COMPANY_NAME:
                case APPLICATION_NAME:
                    if (!value.isEmpty() && (!validIdPattern.matcher(value).matches() || value.contains("..")))
                        throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
                    break;
                case SUB_PATH:
                    if (!value.isEmpty() && (!validPathPattern.matcher(value).matches() || value.contains("..")))
                        throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
                    break;
                default:
                    break;
                }
            }
        putInternal(feature, value);
        }

    public void setStrings(final ConfigFeature feature, final List<String> values) { setStrings((ConfigFeatureInterface) feature, values); }
    public void setStrings(final ConfigFeatureInterface feature, final List<String> values)
        { setFeatureTyped(feature, values, ConfigFeature.ValueType.STRINGLIST); }

    public void setInteger(final ConfigFeature feature, final Integer value) { setInteger((ConfigFeatureInterface) feature, value); }
    public void setInteger(final ConfigFeatureInterface feature, final Integer value)
        { setFeatureTyped(feature, value, ConfigFeature.ValueType.NUMBER); }

    /**
     * <p>setObject.</p>
     */
    public void setObject(final ConfigFeature feature, final Object value)
        {
        setObject((ConfigFeatureInterface) feature, value);
        }

    /**
     * Set an object feature value by interface.
     */
    public void setObject(final ConfigFeatureInterface feature, final Object value)
        {
        if (feature.getValueType() != ConfigFeature.ValueType.SPECIAL_CLASS)
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature.name(), "special object", feature.getValueType().name()));

        if (value == null)
            {
            putInternal(feature, null);
            return;
            }

        // Validation logic
        if (feature instanceof ConfigFeature)
            {
            ConfigFeature cf = (ConfigFeature) feature;
            if (!cf.isSpecialClassType(value.getClass()))
                throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, cf.name(), "special object", cf.getValueType().name()));

            if (cf == ConfigFeature.TESTMODE_PARAMETERS)
                {
                if (!(value instanceof Map))
                    throw new IllegalArgumentException("TESTMODE_PARAMETERS must be a Map<String,String>");
                }
            }
        else if (feature.getClassType() != null && !feature.getClassType().isInstance(value))
            {
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature.name(), "special object", feature.getValueType().name()));
            }

        putInternal(feature, value);
        }

    /**
     * Set a feature by name (string-based).
     */
    @SuppressWarnings("unchecked")
    public void setFeature(String name, Object value)
        {
        ConfigFeatureInterface feature = ConfigFeatureRegistry.get(name);
        if (feature != null)
            {
            switch (feature.getValueType())
                {
                case BOOLEAN:
                    setBoolean(feature, (Boolean) value);
                    break;
                case STRING:
                    setString(feature, (String) value);
                    break;
                case NUMBER:
                    setInteger(feature, (Integer) value);
                    break;
                case STRINGLIST:
                    setStrings(feature, (List<String>) value);
                    break;
                case SPECIAL_CLASS:
                    setObject(feature, value);
                    break;
                }
            }
        else
            {
            rawSettings.put(name, value);
            }
        }

    public Map<Path, ConfigScope> getDirectories() { return directories; }

    public void addDirectory(Path path, ConfigScope scope) { directories.put(path, scope); }

    /**
     * Internal access to settings for debugging.
     */
    void forEach(java.util.function.BiConsumer<ConfigFeatureInterface, Object> action)
        {
        settings.forEach(action);
        }
}
//___EOF___
