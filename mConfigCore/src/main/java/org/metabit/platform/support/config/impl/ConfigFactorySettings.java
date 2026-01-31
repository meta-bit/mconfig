/*
 * Copyright (c) metabit 2018-2026. placed under CC-BY-ND-4.0 license.
 * Full license text available at https://tldrlegal.com/license/creative-commons-attribution-noderivatives-4.0-international-(cc-by-nd-4.0)#fulltext
 * You may: distribute, use for commercial and non-commercial purposes
 * You must: give credit, include/keep copyright, state any changes
 * You mustn't: distribute modified versions, sublicense
 */

package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.scheme.ConfigScheme;

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
 * 
 * @version $Id: $Id
 */
public class ConfigFactorySettings extends HashMap<ConfigFeature, Object> implements Map<ConfigFeature, Object>
{
    public static final Pattern validPathPatternAllowsEmpty = Pattern.compile("^(?:\\w|\\s|-|\\.)*(?:/(?:\\w|\\s|-|\\.)+)*$"); // word-letters and spaces, with slashes allowed.
    public static final Pattern validPathPattern            = Pattern.compile("^(?:\\w|\\s|-|\\.)+(?:/(?:\\w|\\s|-|\\.)+)*$"); // word-letters and spaces, with slashes allowed.
    public static final Pattern validIdPattern              = Pattern.compile("^[\\w\\s.-]+$"); // simple ID, no slashes or dots traversal.
    private static final String TYPE_MISMATCH_MSG = "type mismatch: trying to set {0} with {1} value; but its type is {2}";

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
        for (ConfigFeature feature : ConfigFeature.values())
            {
            Object o = feature.getDefault();
            if (o != null)
                { cfs.put(feature, o); }
            // null values are OK/valid; e.g. for COMPANY_NAME and so on.
            }
        return;
        }

    private final Map<Path, ConfigScope> directories = new HashMap<>();

    /**
     * <p>isSet.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @return a boolean
     */
    public boolean isSet(final ConfigFeature feature)
        {
        if (feature.isBooleanType())
            return true; // booleans always have a value (default is "false")
        return this.get(feature) != null;
        }

    /**
     * <p>getBoolean.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @return a {@link java.lang.Boolean} object
     */
    public Boolean getBoolean(final ConfigFeature feature)
        {
        if (!feature.isBooleanType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "Boolean", feature.getType().name()));
        if (!this.isSet(feature)) // the default for all booleans here is "false".
            return false;
        Boolean tmp = (Boolean) this.get(feature);
        if (tmp == null)
            return false;
        return tmp;
        }

    /**
     * <p>getString.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @return a {@link java.lang.String} object
     */
    public String getString(final ConfigFeature feature)
        {
        if (!feature.isStringType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "String", feature.getType().name()));
        return (String) this.get(feature);
        }

    /**
     * <p>getInteger.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @return a {@link java.lang.Integer} object
     */
    public Integer getInteger(final ConfigFeature feature)
        {
        if (!feature.isNumberType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "Integer", feature.getType().name()));
        return (Integer) this.get(feature);
        }

    /**
     * <p>getStrings.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @return a {@link java.util.List} object
     */
    @SuppressWarnings("unchecked")
    public List<String> getStrings(final ConfigFeature feature)
        {
        if (!feature.isStringListType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "List of Strings", feature.getType().name()));

        Object value = this.get(feature);
        if (value == null)
            return Collections.emptyList();

        if (!(value instanceof List))
            throw new IllegalArgumentException(feature+" is expected to hold List<String> but stored type is "+value.getClass().getCanonicalName());

        List<?> raw = (List<?>) value;
        List<String> out = new ArrayList<>(raw.size());
        for (Object e : raw)
            {
            if (!(e instanceof String))
                throw new IllegalArgumentException(feature+" must be a List<String>, but found element type "+
                        (e == null ? "null" : e.getClass().getSimpleName()));
            out.add((String) e);
            }
        return Collections.unmodifiableList(out);
        }

    /**
     * Store an object for SPECIAL_CLASS features with tolerant type checks.
     * For TESTMODE_PARAMETERS, validates Map<String,String> and normalizes the value.
     */
    @SuppressWarnings("unchecked")
    public <T> void setObject(final ConfigFeature feature, final T value, final Class<T> expectedClass)
        {
        if (feature == null)
            throw new IllegalArgumentException("feature must not be null");
        if (feature.getType() != ConfigFeature.ValueType.SPECIAL_CLASS)
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "special object type", feature.getType().name()));

        // null stores as null
        if (value == null)
            {
            this.put(feature, null);
            return;
            }
        // Special normalization for TESTMODE_PARAMETERS: ensure Map<String,String>
        if (feature == ConfigFeature.TESTMODE_PARAMETERS)
            {
            if (!(value instanceof Map))
                throw new IllegalArgumentException("TESTMODE_PARAMETERS must be a Map<String,String>, but got "+value.getClass().getCanonicalName());

            Map<?, ?> raw = (Map<?, ?>) value;
            Map<String, String> normalized = new HashMap<>(raw.size());
            for (Map.Entry<?, ?> e : raw.entrySet())
                {
                if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String))
                    throw new IllegalArgumentException("TESTMODE_PARAMETERS must be a Map<String,String>, but found entry type "+
                            (e.getKey() == null ? "null" : e.getKey().getClass().getSimpleName())+"->"+
                            (e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName()));
                normalized.put((String) e.getKey(), (String) e.getValue());
                }
            this.put(feature, normalized);
            return;
            }

        // Generic SPECIAL_CLASS handling: accept assignable runtime types
        Class<?> declared = null;
        // introspect declared special class if available
        if (feature.isSpecialClassType(Object.class))
            {
            // no-op, feature declares some special class
            }
        // best-effort: use the feature's declared class via isSpecialClassType check when expectedClass is not provided
        boolean expectedOk = (expectedClass == null) || expectedClass.isInstance(value);
        // if a declared class exists, ensure assignable
        // cannot access classType directly; rely on the isSpecialClassType contract using the runtime class
        // default to true if no declared class available
        boolean declaredOk = feature.isSpecialClassType(value.getClass());

        if (!expectedOk && !declaredOk)
            throw new IllegalArgumentException(feature+" special value type mismatch: value="+value.getClass().getCanonicalName()+
                    (expectedClass != null ? (", expected="+expectedClass.getCanonicalName()) : ""));

        this.put(feature, value);
        }

    /*
     * @param expectedClass a {@link java.lang.Class} object
     * @param <T> a T class
     * @return a T object
     */
    public <T extends Object> T getObject(final ConfigFeature feature, final Class<T> expectedClass)
        {
        if (feature == null || expectedClass == null)
            throw new IllegalArgumentException("feature and expectedClass must not be null");

        // Only SPECIAL_CLASS is supported here
        if (feature.getType() != ConfigFeature.ValueType.SPECIAL_CLASS)
            throw new IllegalArgumentException(feature+" is asked to get a special object value, but entry doesn't have that type");

        Object value = this.get(feature);
        if (value == null)
            return null;

        // Be tolerant: accept when the stored value is assignable to the requested class
        if (expectedClass.isInstance(value))
            return expectedClass.cast(value);

        // Also accept when feature declares a compatible class hierarchy
        if (feature.isSpecialClassType(expectedClass))
            return expectedClass.cast(value); // safe due to previous check

        throw new IllegalArgumentException(
                feature+" is asked to get a special object value of type "+expectedClass.getCanonicalName()+
                        ", but stored value is of type "+value.getClass().getCanonicalName());
        }

    /**
     * Convenience accessor for Map<String,String> SPECIAL_CLASS features.
     * Returns an immutable empty map if unset. Validates content types defensively.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getStringMap(final ConfigFeature feature)
        {
        if (feature == null)
            throw new IllegalArgumentException("feature must not be null");
        if (feature.getType() != ConfigFeature.ValueType.SPECIAL_CLASS)
            throw new IllegalArgumentException(feature+" is asked for Map<String,String>, but entry doesn't have SPECIAL_CLASS type");

        Object value = this.get(feature);
        if (value == null)
            return Collections.emptyMap();

        if (!(value instanceof Map))
            throw new IllegalArgumentException(feature+" is expected to hold Map<String,String> but stored type is "+value.getClass().getCanonicalName());

        Map<?, ?> raw = (Map<?, ?>) value;
        Map<String, String> result = new HashMap<>(raw.size());
        for (Map.Entry<?, ?> e : raw.entrySet())
            {
            if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String))
                throw new IllegalArgumentException(feature+" must be a Map<String,String>, but found entry type "+
                        (e.getKey() == null ? "null" : e.getKey().getClass().getSimpleName())+"->"+
                        (e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName()));
            result.put((String) e.getKey(), (String) e.getValue());
            }
        return Collections.unmodifiableMap(result);
        }
// ... existing code ...

    /**
     * <p>setBoolean.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @param value a {@link java.lang.Boolean} object
     */
    public void setBoolean(final ConfigFeature feature, final Boolean value)
        {
        if (!feature.isBooleanType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "Boolean", feature.getType().name()));
        this.put(feature, value);
        return;
        }

    /**
     * <p>setString.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @param value a {@link java.lang.String} object
     */
    public void setString(final ConfigFeature feature, final String value)
        {
        //@IMPROVEMENT explicitly prohibit null values?
        if (!feature.isStringType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "String", feature.getType().name()));
        // Checks
        if (value == null)
            throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
        switch (feature)
            {
            //@TODO more checks
            case COMPANY_NAME:
            case APPLICATION_NAME:
                if (value.isEmpty()) break;
                if (!validIdPattern.matcher(value).matches() || value.contains(".."))
                    throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
                break;
            case SUB_PATH:
                if (value.isEmpty()) break;  // "" is explicitly allowed.
                if (!validPathPattern.matcher(value).matches() || value.contains(".."))
                    throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
                break;
            default: // no other checks.
                break;
            }
        this.put(feature, value);
        return;
        }

    /**
     * <p>setStrings.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @param values a {@link java.util.List} object
     */
    public void setStrings(final ConfigFeature feature, final List<String> values)
        {
        if (!feature.isStringListType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "List of Strings", feature.getType().name()));
        this.put(feature, values);
        return;
        }

    /**
     * <p>setInteger.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @param value a {@link java.lang.Integer} object
     */
    public void setInteger(final ConfigFeature feature, final Integer value)
        {
        if (!feature.isNumberType())
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "Integer", feature.getType().name()));
        this.put(feature, value);
        return;
        }

    /**
     * <p>setObject.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @param value a {@link java.lang.Object} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    public void setObject(final ConfigFeature feature, final Object value)
            throws ConfigException
        {
        if (!feature.isSpecialClassType(value.getClass()))
            throw new IllegalArgumentException(MessageFormat.format(TYPE_MISMATCH_MSG, feature, "special object", feature.getType().name()));
        try
            {
            switch (feature)
                {
                default: // this is intentionally NOT the last branch.
                    break;
                // specific checks necessary since Java type system doesn't carry over what a list consists of.
                case CONFIG_SCHEME_LIST: // Map<String,ConfigScheme>
                    @SuppressWarnings("unchecked") Map<String, ConfigScheme> tmp = (Map<String, ConfigScheme>) value;
                    boolean tmp2 = tmp.isEmpty();
                    // case TESTMODE_PARAMETERS: // Map<String, String>
                }
            }
        catch (ClassCastException|NullPointerException ex)
            {
            throw new ConfigException(ex);
            }
        this.put(feature, value);
        return;
        }

}
//___EOF___
