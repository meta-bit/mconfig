package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Immutable {@link Configuration} view that overlays {@code overrides} on top of {@code parent}.
 * Overrides take precedence, with lazy type conversion and scheme validation on access.
 * <p>
 * Consider using Configuration.put*(key,value,ConfigScope.RUNTIME) instead.
 * The RUNTIME scope is for temporary overrides that are not persisted.
 * <p>
 * @see ConfigUtil#withOverrides(Configuration, Map)
 */
public class OverridingConfiguration implements Configuration
{
    private final Configuration       parent;
    private final Map<String, Object> overrides;
    private final ConfigScheme        scheme;

    public OverridingConfiguration(Configuration parent, Map<String, Object> overrides)
        {
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.overrides = overrides != null ? new HashMap<>(overrides) : new HashMap<>();
        this.scheme = parent.getConfigScheme();
        }

    private String rawToString(Object raw)
        {
        return raw == null ? null : raw.toString();
        }

    private boolean validate(String key, String strVal)
        {
        if (scheme == null) // nothing to validate
            { return true; }
        ConfigEntrySpecification spec = scheme.getSpecification(key);
        if (spec == null) // unknown key, still nothing to validate
            { return true; }
        try
            {
            ConfigEntryMetadata dummyMeta = new ConfigEntryMetadata((ConfigSource) null);
            ConfigEntry tempEntry = new StringConfigEntryLeaf(key, strVal != null ? strVal : "", dummyMeta);
            return scheme.checkConfigEntryValidity(key, tempEntry);
            }
        catch (Exception e)
            {
            throw new IllegalArgumentException("Configuration validation failed for override key '"+key+"': "+strVal, e);
            }
        }

    private String parseString(String s)
        { return s; }

    private Boolean parseBoolean(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        String lower = s.trim().toLowerCase(Locale.ROOT);
        switch (lower)
            {
            case "true":
            case "1":
            case "yes":
                return Boolean.TRUE;
            case "false":
            case "0":
            case "no":
                return Boolean.FALSE;
            default:
                return null;
            }
        }

    private Integer parseInteger(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return Integer.valueOf(s.trim()); }
        catch (NumberFormatException e)
            { return null; }
        }

    private Long parseLong(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return Long.valueOf(s.trim()); }
        catch (NumberFormatException e)
            { return null; }
        }

    private Double parseDouble(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return Double.valueOf(s.trim()); }
        catch (NumberFormatException e)
            { return null; }
        }

    private BigInteger parseBigInteger(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return new BigInteger(s.trim()); }
        catch (NumberFormatException e)
            { return null; }
        }

    private BigDecimal parseBigDecimal(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return new BigDecimal(s.trim()); }
        catch (NumberFormatException e)
            { return null; }
        }

    private byte[] parseBytes(String s)
        { return s != null ? s.getBytes(StandardCharsets.UTF_8) : null; }

    private List<String> parseListOfStrings(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(str->!str.isEmpty())
                .collect(Collectors.toList());
        }

    private SecretValue parseSecret(String s)
        {
        return s == null ? null : new BasicSecretValue(s.getBytes(StandardCharsets.UTF_8), SecretType.PLAIN_TEXT);
        }

    // Typed getters
    @Override
    public String getString(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseString(strVal);
            }
        return parent.getString(fullKey);
        }

    @Override
    public Boolean getBoolean(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseBoolean(strVal);
            }
        return parent.getBoolean(fullKey);
        }

    @Override
    public Integer getInteger(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseInteger(strVal);
            }
        return parent.getInteger(fullKey);
        }

    @Override
    public Long getLong(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseLong(strVal);
            }
        return parent.getLong(fullKey);
        }

    @Override
    public Double getDouble(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseDouble(strVal);
            }
        return parent.getDouble(fullKey);
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseBigInteger(strVal);
            }
        return parent.getBigInteger(fullKey);
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseBigDecimal(strVal);
            }
        return parent.getBigDecimal(fullKey);
        }

    @Override
    public byte[] getBytes(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseBytes(strVal);
            }
        return parent.getBytes(fullKey);
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseListOfStrings(strVal);
            }
        return parent.getListOfStrings(fullKey);
        }

    @Override
    public SecretValue getSecret(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            return parseSecret(strVal);
            }
        return parent.getSecret(fullKey);
        }

    // Put methods - UOE
    @Override
    public void put(String fullKey, String value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("OverridingConfiguration is read-only");
        }

    @Override
    public ConfigScheme getConfigScheme()
        {
        return scheme;
        }

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("OverridingConfiguration does not support updates");
        }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("OverridingConfiguration does not support updates");
        }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("OverridingConfiguration does not support updates");
        }

    @Override
    public ConfigCursor getConfigCursor()
        {
        throw new UnsupportedOperationException("OverridingConfiguration does not support cursors");
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        return Collections.emptyList();
        }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        Set<String> keys = new HashSet<>(overrides.keySet());
        keys.addAll(parent.getAllConfigurationKeysFlattened(scopes));
        return Collections.unmodifiableSet(keys);
        }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes)
        {
        Map<String, ConfigEntrySpecification> map = new HashMap<>();
        if (scheme != null)
            {
            for (String key : overrides.keySet())
                { map.put(key, scheme.getSpecification(key)); }
            }
        map.putAll(parent.getAllConfigurationKeysWithSchemesFlattened(scopes));
        return Collections.unmodifiableMap(map);
        }

    // BasicConfiguration impl
    @Override
    public String getConfigName()
        {
        return parent.getConfigName();
        }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            String strVal = rawToString(overrides.get(key));
            validate(key, strVal);
            ConfigEntryMetadata dummyMeta = new ConfigEntryMetadata((ConfigSource) null);
            return new StringConfigEntryLeaf(key, strVal != null ? strVal : "", dummyMeta);
            }
        return parent.getConfigEntryFromFullKey(fullKey, scopes);
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
        return overrides.isEmpty() && parent.isEmpty();
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        // Simple union; assumes flat keys
        Set<String> allKeys = getAllConfigurationKeysFlattened(ConfigUtil.ALL_SCOPES);
        return allKeys.iterator();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        parent.limitScopes(scopes);
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
}