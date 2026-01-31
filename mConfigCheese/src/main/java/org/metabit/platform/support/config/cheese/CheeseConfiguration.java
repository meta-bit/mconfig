package org.metabit.platform.support.config.cheese;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

/**
 * mConfig Cheese - the facade that allows for code fragrances like getType(key, default).
 * <br/>
 * Wrap your regular {@link Configuration} with this to get the extra methods.
 */
public class CheeseConfiguration implements Configuration
{
    private final Configuration wrapped;

    public CheeseConfiguration(Configuration wrapped)
        {
        this.wrapped = wrapped;
        }

    public String getString(String key, String defaultValue)
        {
        try
            {
            return wrapped.getString(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    public Boolean getBoolean(String key, Boolean defaultValue)
        {
        try
            {
            return wrapped.getBoolean(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    public Integer getInteger(String key, Integer defaultValue)
        {
        try
            {
            return wrapped.getInteger(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    public Long getLong(String key, Long defaultValue)
        {
        try
            {
            return wrapped.getLong(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    public Double getDouble(String key, Double defaultValue)
        {
        try
            {
            return wrapped.getDouble(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    public BigInteger getBigInteger(String key, BigInteger defaultValue)
        {
        try
            {
            return wrapped.getBigInteger(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue)
        {
        try
            {
            return wrapped.getBigDecimal(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() == ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY ||
                    e.getReason() == ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND)
                {
                return defaultValue;
                }
            throw e;
            }
        }

    @Override
    public String getString(String fullKey)
            throws ConfigException
        {
        return wrapped.getString(fullKey);
        }

    @Override
    public Boolean getBoolean(String fullKey)
            throws ConfigException
        {
        return wrapped.getBoolean(fullKey);
        }

    @Override
    public Integer getInteger(String fullKey)
            throws ConfigException
        {
        return wrapped.getInteger(fullKey);
        }

    @Override
    public Long getLong(String fullKey)
            throws ConfigException
        {
        return wrapped.getLong(fullKey);
        }

    @Override
    public Double getDouble(String fullKey)
            throws ConfigException
        {
        return wrapped.getDouble(fullKey);
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
            throws ConfigException
        {
        return wrapped.getBigInteger(fullKey);
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
            throws ConfigException
        {
        return wrapped.getBigDecimal(fullKey);
        }

    @Override
    public byte[] getBytes(String fullKey)
            throws ConfigException
        {
        return wrapped.getBytes(fullKey);
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
            throws ConfigException
        {
        return wrapped.getListOfStrings(fullKey);
        }

    @Override
    public SecretValue getSecret(String fullKey)
            throws ConfigException
        {
        return wrapped.getSecret(fullKey);
        }

    @Override
    public void put(String fullKey, String value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link String} object
     * @param value   a {@link Long} object
     * @param scope   a {@link ConfigScope} object
     * @throws ConfigException if any.
     */
    @Override
    public void put(String fullKey, Long value, ConfigScope scope)
            throws ConfigException
        {
        wrapped.put(fullKey, value, scope);
        }

    /**
     * <p>put.</p>
     *
     * @param fullKey a {@link String} object
     * @param value   a {@link Long} object
     * @param scopes  a {@link EnumSet} object
     * @throws ConfigException if any.
     */
    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope)
        {
        wrapped.put(fullKey, value, scope);
        }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes)
        {
        wrapped.put(fullKey, value, scopes);
        }

    @Override
    public ConfigScheme getConfigScheme()
        {
        return wrapped.getConfigScheme();
        }

    /**
     * get notified when the configuration changes.
     * Note: The scopes for which notifications are sent exclude RUNTIME by default.
     * You can set the scope filter globally with the parameter UPDATE_CHECK_SCOPES
     *
     * @param listener the listener to be executed upon an update.
     */
    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        { wrapped.subscribeToUpdates(listener); }

    /**
     * PLANNED @TODO: subscribe to updates for individual entries
     *
     * @param fullKey  full key of the configuration entry to which receive update notifications for
     * @param listener the listener to be executed upon an update.
     */
    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        { wrapped.subscribeToUpdates(fullKey, listener); }

    /**
     * Remove subscription for all updates where this listener might be called
     *
     * @param listener listener to unsubscribe.
     */
    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        { wrapped.unsubscribeFromUpdates(listener); }

    @Override
    public ConfigCursor getConfigCursor()
        {
        return wrapped.getConfigCursor();
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        return wrapped.getSourceLocations();
        }

    /**
     * Retrieves all configuration keys from all configuration layers,
     * combining them into a single set with keys flattened into a string format.
     * The method collects keys recursively from each configuration layer.
     *
     * @param scopes the scopes to retrieve keys from.
     * @return a {@link Set} containing all flattened configuration keys
     *         across all configuration layers.
     */
    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        return wrapped.getAllConfigurationKeysFlattened(scopes);
        }

    /**
     * Retrieves all configuration keys from all configuration layers,
     * along with their corresponding scheme entries if they exist.
     *
     * @param scopes the scopes to retrieve keys from.
     * @return a {@link Map} with flattened keys as keys and their {@link ConfigEntrySpecification} as values.
     *         Values can be null if no matching scheme entry exists.
     */
    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes)
        {
        return wrapped.getAllConfigurationKeysWithSchemesFlattened(scopes);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        wrapped.limitScopes(scopes);
        }

    @Override
    public String getConfigName()
        {
        return wrapped.getConfigName();
        }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        return wrapped.getConfigEntryFromFullKey(fullKey, scopes);
        }

    @Override
    public void setConfigScheme(ConfigScheme scheme)
        {
        wrapped.setConfigScheme(scheme);
        }

    @Override
    public boolean isWriteable()
        {
        return wrapped.isWriteable();
        }

    @Override
    public int flush()
            throws ConfigCheckedException
        { return wrapped.flush(); }

    @Override
    public boolean reload()
            throws ConfigCheckedException
        { return wrapped.reload(); }

    @Override
    public boolean isEmpty()
        { return wrapped.isEmpty(); }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        { return wrapped.getEntryKeyTreeIterator(); }

    @Override
    public boolean isClosed()
        {
        return wrapped.isClosed();
        }

    @Override
    public void close()
            throws Exception
        { wrapped.close(); }
}
