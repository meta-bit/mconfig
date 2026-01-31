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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Immutable {@link Configuration} view adapting {@link Properties}.
 * Typed getters parse lazily from property strings (null on parse fail).
 * No scheme or locations.
 * <p>
 * For adapting legacy code, step by step. Using this, you have almost none
 * of the features mConfig supplies, but you can get programmatically generated
 * Properties into the unified structure.
 * <p>
 * Consider loading the Properties via mConfig itself, e.g. by supplying their
 * paths via ConfigFeature.ADDITIONAL_USER_DIRECTORIES and similar features.
 *
 * @see ConfigUtil#fromProperties(Properties)
 */
public class PropertiesConfiguration implements Configuration
{
    private final Properties props;

    public PropertiesConfiguration(Properties props)
        { this.props = props != null ? props : new Properties(); }

    private String parseString(String s)
        {
        return s;
        }

    private Boolean parseBoolean(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        String lower = s.trim().toLowerCase();
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
            {
            return Integer.valueOf(s.trim());
            }
        catch (NumberFormatException ignored)
            {
            return null;
            }
        }

    // Similar parse methods for Long, Double, BigInteger, BigDecimal, bytes, list...

    private Long parseLong(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return Long.valueOf(s.trim()); }
        catch (NumberFormatException ignored)
            { return null; }
        }

    private Double parseDouble(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return Double.valueOf(s.trim()); }
        catch (NumberFormatException ignored)
            { return null; }
        }

    private BigInteger parseBigInteger(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return new BigInteger(s.trim()); }
        catch (NumberFormatException ignored)
            { return null; }
        }

    private BigDecimal parseBigDecimal(String s)
        {
        if (s == null || s.trim().isEmpty())
            { return null; }
        try
            { return new BigDecimal(s.trim()); }
        catch (NumberFormatException ignored)
            { return null; }
        }

    private byte[] parseBytes(String s)
        {
        return s != null ? s.getBytes(StandardCharsets.UTF_8) : null;
        }

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
        return null; // or delegate if needed
        }

    // Typed getters
    @Override
    public String getString(String fullKey)
        {
        return props.getProperty(fullKey.trim());
        }

    @Override
    public Boolean getBoolean(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseBoolean(s);
        }

    @Override
    public Integer getInteger(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseInteger(s);
        }

    // Repeat for all typed getters using parseXXX

    @Override
    public Long getLong(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseLong(s);
        }

    @Override
    public Double getDouble(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseDouble(s);
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseBigInteger(s);
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseBigDecimal(s);
        }

    @Override
    public byte[] getBytes(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseBytes(s);
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseListOfStrings(s);
        }

    @Override
    public SecretValue getSecret(String fullKey)
        {
        String s = props.getProperty(fullKey.trim());
        return parseSecret(s);
        }

    // Puts UOE (all)
    @Override
    public void put(String fullKey, String value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration is read-only");
        }

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration does not support updates");
        }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration does not support updates");
        }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        throw new UnsupportedOperationException("PropertiesConfiguration does not support updates");
        }

    @Override
    public ConfigScheme getConfigScheme()
        {
        return null;
        }

    // Subscribe UOE

    @Override
    public ConfigCursor getConfigCursor()
        {
        throw new UnsupportedOperationException("PropertiesConfiguration does not support cursors");
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        return Collections.emptyList();
        }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        return new HashSet<>(props.stringPropertyNames());
        }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes)
        {
        return Collections.emptyMap();
        }

    // Basic
    @Override
    public String getConfigName()
        {
        return "properties";
        }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        String key = fullKey.trim();
        String value = props.getProperty(key);
        if (value == null)
            {
            return null;
            }
        ConfigEntryMetadata dummy = new ConfigEntryMetadata((ConfigSource) null);
        return new StringConfigEntryLeaf(key, value, dummy);
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
        return props.isEmpty();
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        return props.stringPropertyNames().iterator();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        // no-op: single layer, ignores scopes
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