package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.util.ConfigIOUtil;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.SecretConfigEntry;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;

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

    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        if (scopes == null)
            {
            throw new IllegalArgumentException("scopes must not be null");
            }
        this.allowedScopes = scopes;
        }

    private <T> T getTyped(String fullKey, TypedGetter<T> getter)
            throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            return (entry != null) ? getter.get(entry) : null;
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    private interface TypedGetter<T>
        {
        T get(ConfigEntry entry) throws ConfigCheckedException;
        }

    @Override
    public String getString(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsString); }

    @Override
    public Boolean getBoolean(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsBoolean); }

    @Override
    public Integer getInteger(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsInteger); }

    @Override
    public Long getLong(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsLong); }

    @Override
    public Double getDouble(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsDouble); }

    @Override
    public BigInteger getBigInteger(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsBigInteger); }

    @Override
    public BigDecimal getBigDecimal(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsBigDecimal); }

    @Override
    public List<String> getListOfStrings(String fullKey) throws ConfigException
        { return getTyped(fullKey, ConfigEntry::getValueAsStringList); }

    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        try
            {
            ConfigEntry entry = getEntryWithExceptionCheck(fullKey);
            if (entry == null) return null;
            if (entry instanceof SecretConfigEntry)
                {
                return ((SecretConfigEntry) entry).getSecretValue();
                }
            if (entry.isSecret())
                {
                byte[] bytes = entry.getValueAsBytes();
                return new BasicSecretValue(bytes, SecretType.PLAIN_TEXT);
                }
            return null;
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
                    try
                        {
                        return Base64.getDecoder().decode(tmpString);
                        }
                    catch (IllegalArgumentException ignored)
                        {
                        }
                    byte[] tmpBytes = ConfigIOUtil.hexDecode(tmpString);
                    if (tmpBytes != null) return tmpBytes;
                    return tmpString.getBytes(StandardCharsets.UTF_8);
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

    private void putTyped(String fullKey, Object value, ConfigEntryType type, ConfigScope scope)
            throws ConfigException
        {
        try
            {
            putGeneric(fullKey, value, type, scope);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public void put(String fullKey, String value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.STRING, scope); }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.BOOLEAN, scope); }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.NUMBER, scope); }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.NUMBER, scope); }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.NUMBER, scope); }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.NUMBER, scope); }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.NUMBER, scope); }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.BYTES, scope); }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope) throws ConfigException
        { putTyped(fullKey, value, ConfigEntryType.MULTIPLE_STRINGS, scope); }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.STRING, scopes); }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.BOOLEAN, scopes); }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.NUMBER, scopes); }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.NUMBER, scopes); }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.NUMBER, scopes); }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.NUMBER, scopes); }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.NUMBER, scopes); }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.BYTES, scopes); }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes) throws ConfigException
        { putWithScopes(fullKey, value, ConfigEntryType.MULTIPLE_STRINGS, scopes); }

    private void putWithScopes(String fullKey, Object value, ConfigEntryType type, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (scopes == null || scopes.isEmpty()) return;
        
        // Find the best (highest priority) scope among those provided.
        // Priority is determined by the ordinal - higher ordinal means higher priority.
        ConfigScope best = null;
        for (ConfigScope s : scopes)
            {
            if (best == null || s.ordinal() > best.ordinal())
                {
                best = s;
                }
            }
        
        if (best != null)
            {
            putTyped(fullKey, value, type, best);
            }
        }

    protected abstract void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope)
            throws ConfigCheckedException;

    private void putWithPreferredScope(String fullKey, Object value, ConfigEntryType type)
            throws ConfigException
        {
        ConfigSchema schema = getConfigSchema();
        ConfigScope preferred = null;
        if (schema != null)
            {
            ConfigEntrySpecification spec = schema.getSpecification(fullKey);
            if (spec != null) preferred = spec.getWriteScope();
            }

        if (preferred != null)
            {
            putTyped(fullKey, value, type, preferred);
            return;
            }
        putWithScopes(fullKey, value, type, EnumSet.allOf(ConfigScope.class));
        }

    @Override
    public void put(String fullKey, String value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.STRING); }

    @Override
    public void put(String fullKey, Boolean value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.BOOLEAN); }

    @Override
    public void put(String fullKey, Integer value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.NUMBER); }

    @Override
    public void put(String fullKey, Long value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.NUMBER); }

    @Override
    public void put(String fullKey, Double value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.NUMBER); }

    @Override
    public void put(String fullKey, BigInteger value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.NUMBER); }

    @Override
    public void put(String fullKey, BigDecimal value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.NUMBER); }

    @Override
    public void put(String fullKey, byte[] value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.BYTES); }

    @Override
    public void put(String fullKey, List<String> value) throws ConfigException
        { putWithPreferredScope(fullKey, value, ConfigEntryType.MULTIPLE_STRINGS); }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        { return Collections.emptySet(); }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        { return Collections.emptyMap(); }
}