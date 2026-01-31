package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

/**
 * Immutable {@link Configuration} view with prefix remapping on {@code source}.
 * Keys matching {@code newPrefix} are remapped to {@code oldPrefix + suffix} for lookup.
 *
 * @see ConfigUtil#remapped(Configuration, String, String)
 */
public class RemappedConfiguration implements Configuration
{
    private final Configuration source;
    private final String        oldPrefix;
    private final String        newPrefix;
    private final ConfigScheme  scheme;

    public RemappedConfiguration(Configuration source, String oldPrefix, String newPrefix)
        {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.oldPrefix = oldPrefix != null ? oldPrefix.trim() : "";
        this.newPrefix = newPrefix != null ? newPrefix.trim() : "";
        this.scheme = source.getConfigScheme();
        }

    private String computeMapped(String key)
        {
        if (key.startsWith(newPrefix))
            { return oldPrefix+key.substring(newPrefix.length()); }
        return key;
        }

    private boolean validate(String key, String strVal)
        {
        if (scheme == null) // no scheme, nothing to validate by
            { return true; }
        ConfigEntrySpecification spec = scheme.getSpecification(key);
        if (spec == null) // key not found in scheme, nothing to validate by
            { return true; }
        try
            {
            ConfigEntryMetadata dummyMeta = new ConfigEntryMetadata((ConfigSource) null);
            ConfigEntry tempEntry = new StringConfigEntryLeaf(key, strVal != null ? strVal : "", dummyMeta);
            return scheme.checkConfigEntryValidity(key, tempEntry);
            }
        catch (Exception e)
            {
            throw new IllegalArgumentException("Configuration validation failed for remapped key '"+key+"': "+strVal, e);
            }
        }

    // Typed getters - delegate with validation
    @Override
    public String getString(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getString(mappedKey);
        }

    @Override
    public Boolean getBoolean(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getBoolean(mappedKey);
        }

    @Override
    public Integer getInteger(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getInteger(mappedKey);
        }

    @Override
    public Long getLong(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getLong(mappedKey);
        }

    @Override
    public Double getDouble(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getDouble(mappedKey);
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getBigInteger(mappedKey);
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getBigDecimal(mappedKey);
        }

    @Override
    public byte[] getBytes(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getBytes(mappedKey);
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getListOfStrings(mappedKey);
        }

    @Override
    public SecretValue getSecret(String fullKey)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        String strVal = source.getString(mappedKey);
        if (strVal != null)
            { validate(key, strVal); }
        return source.getSecret(mappedKey);
        }

    // Puts UOE
    // ... (same as OverridingConfiguration, throw UOE for all put*)

    @Override
    public void put(String fullKey, String value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("RemappedConfiguration is read-only");
        }

    @Override
    public ConfigScheme getConfigScheme()
        {
        return scheme;
        }

    // Subscribe UOE
    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("RemappedConfiguration does not support updates");
        }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("RemappedConfiguration does not support updates");
        }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("RemappedConfiguration does not support updates");
        }

    // ... other subscribe

    @Override
    public ConfigCursor getConfigCursor()
        {
        throw new UnsupportedOperationException("RemappedConfiguration does not support cursors");
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        return Collections.emptyList();
        }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        Set<String> res = new HashSet<>();
        for (String sourceKey : source.getAllConfigurationKeysFlattened(scopes))
            {
            if (sourceKey.startsWith(oldPrefix))
                {
                String suffix = sourceKey.substring(oldPrefix.length());
                res.add(newPrefix+suffix);
                }
            else
                {
                res.add(sourceKey);
                }
            }
        return res;
        }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes)
        {
        Map<String, ConfigEntrySpecification> res = new HashMap<>();
        Map<String, ConfigEntrySpecification> sourceMap = source.getAllConfigurationKeysWithSchemesFlattened(scopes);
        for (String sourceKey : sourceMap.keySet())
            {
            String newKey;
            if (sourceKey.startsWith(oldPrefix))
                {
                String suffix = sourceKey.substring(oldPrefix.length());
                newKey = newPrefix+suffix;
                }
            else
                {
                newKey = sourceKey;
                }
            res.put(newKey, sourceMap.get(sourceKey));
            }
        return res;
        }

    // Basic
    @Override
    public String getConfigName()
        {
        return source.getConfigName();
        }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        String key = fullKey.trim();
        String mappedKey = computeMapped(key);
        ConfigEntry entry = source.getConfigEntryFromFullKey(mappedKey, scopes);
        if (entry != null)
            {
            try
                {
                String strVal = entry.getValueAsString();
                validate(key, strVal);
                }
            catch (ConfigCheckedException e)
                {
                throw new IllegalArgumentException("Validation failed", e);
                }
            }
        return entry;
        }

    @Override
    public void setConfigScheme(ConfigScheme scheme)
        {
        throw new UnsupportedOperationException("Immutable view");
        }

    @Override
    public boolean isWriteable()
        {
        return false;
        }

    @Override
    public int flush()
            throws ConfigCheckedException
        {
        return 0;
        }

    @Override
    public boolean reload()
            throws ConfigCheckedException
        {
        return false;
        }

    @Override
    public boolean isEmpty()
        {
        return source.isEmpty();
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        return source.getEntryKeyTreeIterator();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        source.limitScopes(scopes);
        }

    @Override
    public boolean isClosed()
        {
        return false;
        }

    @Override
    public void close()
        {
        // no-op
        }

    // Add all missing put methods with UOE...
}