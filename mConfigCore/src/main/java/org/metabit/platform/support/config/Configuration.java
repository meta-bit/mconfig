package org.metabit.platform.support.config;

import org.metabit.platform.support.config.interfaces.BasicConfiguration;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>Configuration interface.</p>
 *
 * 
 * @version $Id: $Id
 */
public interface Configuration extends BasicConfiguration
{
    /**
     * get a String entry from a config.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return String value. If there was format specific escaping, it has been un-escaped.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    String getString(final String fullKey)
            throws ConfigException;

    /**
     * get a Boolean entry from a config.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * see java.lang.Boolean.parseBoolean for details.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return String value. If there was format specific escaping, it has been un-escaped.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    Boolean getBoolean(final String fullKey)
            throws ConfigException;

    /**
     * get an Integer entry from a config.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * see java.lang.Integer.parseInteger for details, decimal interpretation as default.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return String value. If there was format specific escaping, it has been un-escaped.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    Integer getInteger(final String fullKey)
            throws ConfigException;

    /**
     * get a Long integer entry from a config.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * see java.lang.Long.parseLong for details, decimal interpretation as default.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return String value. If there was format specific escaping, it has been un-escaped.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    Long getLong(final String fullKey)
            throws ConfigException;

    /**
     * get a Double precision floating point entry from a config.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * see java.lang.Double.parseDouble for details, decimal interpretation as default.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return String value. If there was format specific escaping, it has been un-escaped.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    Double getDouble(final String fullKey)
            throws ConfigException;

    /**
     * <p>getBigInteger.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @return a {@link java.math.BigInteger} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    BigInteger getBigInteger(final String fullKey)
            throws ConfigException;

    /**
     * <p>getBigDecimal.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @return a {@link java.math.BigDecimal} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    BigDecimal getBigDecimal(final String fullKey)
            throws ConfigException;



    /**
     * get an array of bytes entry from a config.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * strings will be converted to byte arrays, using base64 if applicable, hex as a fallback, UTF-8 as last resort.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return String value. If there was format specific escaping, it has been un-escaped.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    byte[] getBytes(final String fullKey)
            throws ConfigException;

    /**
     * get a List of Strings a config entry.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * Strings will result in a List with a single element.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return List of String
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     *                         In some cases, you may want to check the exception whether it reports a constructed/composite type.
     *                         If that happens, your key is not fully describing the entry yet.
     */
    List<String> getListOfStrings(final String fullKey);

    /**
     * get a SecretValue entry from a config.
     *
     * @param fullKey full key to the entry; if tree structure, including full path.
     * @return SecretValue instance.
     * @throws org.metabit.platform.support.config.ConfigException if neither a configuration, nor a default for this could be read;
     *                         or, if the type was not matching and not convertible either.
     */
    SecretValue getSecret(final String fullKey)
            throws ConfigException;

    // ---- writing interface ----
    // put as opposed to putString etc, because compiler knows the type
    // we can't do that for the gets, because you may want conversion.

    /**
     * put = write a value to a key, single scope.
     * <p>
     * automatic creation of new configurations is controlled by settings in the CFB and ConfigFactory.
     *
     * @param fullKey the full key of the entry to be written
     * @param value the value to be written. variations of the function with different types exist.
     * @param scope the Configuration Scope this entry is to be written to.
     * @throws org.metabit.platform.support.config.ConfigException  if not writable in general;
     *                          if the entry could not be written anywhere in the specified scope,
     *                          or if a parameter was invalid.
     */
    void put(final String fullKey, final String value, final ConfigScope scope)  throws ConfigException;

    /**
     * put = write a value to a key, multiple scopes with fall-forward.
     * <p>
     * automatic creation of new configurations is controlled by settings in the CFB and ConfigFactory.
     *
     * @param fullKey the full key of the entry to be written
     * @param value the value to be written. variations of the function with different types exist.
     * @param scopes a set of valid scopes. If the value cannot be written to the most generic/global scope in the set,
     *               then the other scopes are attempted in succession, from the most general to the most specific.
     * @throws org.metabit.platform.support.config.ConfigException  if not writable in general;
     *                          if the entry could not be written anywhere in one the specified scopes,
     *                          or if a parameter was invalid.
     */
    void put(final String fullKey, final String value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Boolean} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Boolean value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Boolean} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Boolean value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Integer} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Integer value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Integer} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Integer value, final EnumSet<ConfigScope> scopes) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Long} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Long value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Long} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Long value, final EnumSet<ConfigScope> scopes) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Double} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Double value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.lang.Double} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final Double value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.math.BigInteger} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final BigInteger value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.math.BigInteger} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final BigInteger value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.math.BigDecimal} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final BigDecimal value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.math.BigDecimal} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final BigDecimal value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value an array of {@link byte} objects
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final byte[] value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value an array of {@link byte} objects
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final byte[] value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.util.List} object
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final List<String> value, final ConfigScope scope) throws ConfigException;
    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param value a {@link java.util.List} object
     * @param scopes a {@link java.util.EnumSet} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    void put(final String fullKey, final List<String> value, final EnumSet<ConfigScope> scopes) throws ConfigException;

    /*
     * get a list of all ConfigEvents for this Configuration.
     * for ConfigEvents not specific to any single Configuration, see the respective function in the {ConfigFactory}.
     *
     * @return ConfigEventList
     */
    // ConfigEventList getEvents();

    /**
     * get the config scheme associated with this configuration.
     *
     * @return return the config scheme; or return null if this configuration hasn't any.
     */
    ConfigScheme getConfigScheme();

    /**
     * get notified when the configuration changes.
     * Note: The scopes for which notifications are sent exclude RUNTIME by default.
     * You can set the scope filter globally with the parameter UPDATE_CHECK_SCOPES
     *
     * @param listener the listener to be executed upon an update.
     */
    void subscribeToUpdates(Consumer<ConfigLocation> listener);

    /**
     * Per-entry subscriptions for changes.
     * supported via SourceChangeNotifier. Use carefully, it adds overhead.
     * @param fullKey  full key of the configuration entry to which receive update notifications for
     * @param listener the listener to be executed upon an update.
     */
    void subscribeToUpdates(final String fullKey, Consumer<ConfigLocation> listener);

    /**
     * Remove subscription for all updates where this listener might be called
     *
     * @param listener listener to unsubscribe.
     */
    void unsubscribeFromUpdates(Consumer<ConfigLocation> listener);

    /*
    Missing feature: 2D-access
    2D iterator for tree-like structures. This interface to have an an accessor (obtain, release? simple get?)
    The iterator, e.g. ConfigIterator, to contain the actual functionality.

     */
    /**
     * <p>getConfigCursor.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigCursor} object
     */
    ConfigCursor getConfigCursor();

    /**
     * get the list of all source locations the layers refer to.
     * @return a list of ConfigLocation entries
     */
    List<ConfigLocation> getSourceLocations();

    /**
     * Retrieves all configuration keys from all configuration layers,
     * combining them into a single set with keys flattened into a string format.
     * The method collects keys recursively from each configuration layer.
     *
     * @param scopes the scopes to retrieve keys from.
     * @return a {@link Set} containing all flattened configuration keys
     *         across all configuration layers.
     */
    Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes);

    /**
     * Retrieves all configuration keys from all configuration layers,
     * along with their corresponding scheme entries if they exist.
     *
     * @param scopes the scopes to retrieve keys from.
     * @return a {@link Map} with flattened keys as keys and their {@link ConfigEntrySpecification} as values.
     *         Values can be null if no matching scheme entry exists.
     */
    Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes);

    /**
     * Limit the scopes from which this configuration reads values on subsequent get operations (typed gets, getSecret).
     * Does not affect puts, getConfigCursor, getAllConfigurationKeysFlattened (which take explicit scopes), etc.
     * Default before calling: all scopes.
     *
     * @param scopes the allowed scopes. Non-null.
     * @throws IllegalArgumentException if scopes is null.
     */
    void limitScopes(EnumSet<ConfigScope> scopes);

}
