package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.util.ConfigIOUtil;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Common base class for Configuration implementations.
 * Handles type conversions and default implementations for type-safe getters and setters.
 */
public abstract class AbstractConfiguration implements Configuration
{
    protected boolean exceptionOnNullFlag = false;

    protected EnumSet<ConfigScope> allowedScopes = EnumSet.allOf(ConfigScope.class);

    protected AbstractConfiguration()
        {
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        if (scopes == null)
            {
            throw new IllegalArgumentException("scopes must not be null");
            }
        this.allowedScopes = scopes;
        }

    @Override
    public String getString(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsString() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public Boolean getBoolean(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsBoolean() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public Integer getInteger(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsInteger() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public Long getLong(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsLong() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public Double getDouble(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsDouble() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsBigInteger() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsBigDecimal() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public byte[] getBytes(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            if (entry == null)
                {
                return null;
                }
            switch (entry.getType())
                {
                case BYTES:
                    return entry.getValueAsBytes();
                case STRING:
                    String tmpString = entry.getValueAsString();
                    // 1. try base64 decoding.
                    try
                        {
                        return Base64.getDecoder().decode(tmpString);
                        }
                    catch (IllegalArgumentException ignored)
                        {
                        }
                    // 2. try hex decoding.
                    byte[] tmpBytes = ConfigIOUtil.hexDecode(tmpString);
                    return Objects.requireNonNullElseGet(tmpBytes, ()->tmpString.getBytes(StandardCharsets.UTF_8)); // since JDK9
                // 3. convert utf8 to bytes.

                default:
                    throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE);
                }
            }
        catch (ConfigCheckedException ccex)
            {
            throw new ConfigException(ccex);
            }
        }

    private ConfigEntry getEntryWithExceptionCheck(String fullKey)
            throws ConfigCheckedException
        {
        ConfigEntry entry = getConfigEntryFromFullKey(fullKey, allowedScopes);
        if (entry == null && exceptionOnNullFlag)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY);
            }
        return entry;
        }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, ConfigEntryType.BOOLEAN, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    protected abstract void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope)
            throws ConfigCheckedException;

    private void putInOrderedScopes(String fullKey, Object value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (scopes == null || scopes.isEmpty())
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
            }

        // Sort scopes from most specific to most generic (descending ordinal)
        List<ConfigScope> sortedScopes = new ArrayList<>(scopes);
        sortedScopes.sort(Comparator.comparingInt(ConfigScope::ordinal).reversed());

        ConfigEntryType type = ConfigEntryType.STRING;
        if (value instanceof Boolean) type = ConfigEntryType.BOOLEAN;
        else if (value instanceof Number) type = ConfigEntryType.NUMBER;
        else if (value instanceof byte[]) type = ConfigEntryType.BYTES;
        else if (value instanceof List) type = ConfigEntryType.MULTIPLE_STRINGS;

        for (ConfigScope scope : sortedScopes)
            {
            try
                {
                putGeneric(fullKey, value, type, scope);
                return;
                }
            catch (ConfigException|ConfigCheckedException ignored)
                {
                }
            }
        throw new ConfigException(ConfigException.ConfigExceptionReason.NO_WRITEABLE_LOCATION);
        }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        { putInOrderedScopes(fullKey, value, scopes); }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        { return Set.of(); }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes)
        { return Map.of(); }

    @Override
    public List<String> getListOfStrings(String fullKey)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? entry.getValueAsStringList() : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }
}
//___EOF___
