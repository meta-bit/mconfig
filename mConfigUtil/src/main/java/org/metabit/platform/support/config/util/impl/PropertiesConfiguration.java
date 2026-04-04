package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

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
    private final ConfigEventList events = new ConfigEventList(1000);

    public PropertiesConfiguration(Properties props)
        { this.props = props != null ? props : new Properties(); }

    @Override
    public ConfigEventList getEvents()
        {
        return events;
        }

    private ConfigEntry getEntry(String key, ConfigEntryType type)
        {
        String value = props.getProperty(key.trim());
        if (value == null) return null;
        ConfigEntryMetadata dummy = new ConfigEntryMetadata((ConfigSource) null);
        return new GenericConfigEntryLeaf(key, value, type, dummy);
        }

    @Override
    public String getString(String fullKey)
        { return props.getProperty(fullKey.trim()); }

    @Override
    public Boolean getBoolean(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.BOOLEAN);
        try { return entry != null ? entry.getValueAsBoolean() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public Integer getInteger(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.NUMBER);
        try { return entry != null ? entry.getValueAsInteger() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public Long getLong(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.NUMBER);
        try { return entry != null ? entry.getValueAsLong() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public Double getDouble(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.NUMBER);
        try { return entry != null ? entry.getValueAsDouble() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.NUMBER);
        try { return entry != null ? entry.getValueAsBigInteger() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.NUMBER);
        try { return entry != null ? entry.getValueAsBigDecimal() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public byte[] getBytes(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.BYTES);
        try { return entry != null ? entry.getValueAsBytes() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        ConfigEntry entry = getEntry(fullKey, ConfigEntryType.MULTIPLE_STRINGS);
        try { return entry != null ? entry.getValueAsStringList() : null; } catch (ConfigCheckedException e) { return null; }
        }

    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        { return null; }

    private void throwReadOnly() { throw new UnsupportedOperationException("PropertiesConfiguration is read-only"); }

    @Override public void put(String fullKey, String value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, String value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, Boolean value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, Integer value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, Long value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, Double value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, BigInteger value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, BigDecimal value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, byte[] value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, List<String> value, ConfigScope scope) { throwReadOnly(); }
    @Override public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes) { throwReadOnly(); }
    @Override public void put(String fullKey, String value) { throwReadOnly(); }
    @Override public void put(String fullKey, Boolean value) { throwReadOnly(); }
    @Override public void put(String fullKey, Integer value) { throwReadOnly(); }
    @Override public void put(String fullKey, Long value) { throwReadOnly(); }
    @Override public void put(String fullKey, Double value) { throwReadOnly(); }
    @Override public void put(String fullKey, BigInteger value) { throwReadOnly(); }
    @Override public void put(String fullKey, BigDecimal value) { throwReadOnly(); }
    @Override public void put(String fullKey, byte[] value) { throwReadOnly(); }
    @Override public void put(String fullKey, List<String> value) { throwReadOnly(); }

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        { throw new UnsupportedOperationException("ReadOnly"); }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        { throw new UnsupportedOperationException("ReadOnly"); }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        { throw new UnsupportedOperationException("ReadOnly"); }

    @Override
    public ConfigSchema getConfigSchema()
        { return null; }

    @Override
    public ConfigCursor getConfigCursor()
        { throw new UnsupportedOperationException("ReadOnly"); }

    @Override
    public List<ConfigLocation> getSourceLocations()
        { return Collections.emptyList(); }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        { return new HashSet<>(props.stringPropertyNames()); }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        { return Collections.emptyMap(); }

    @Override
    public String getConfigName()
        { return "properties"; }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        String key = fullKey.trim();
        String value = props.getProperty(key);
        if (value == null) return null;
        ConfigEntryMetadata dummy = new ConfigEntryMetadata((ConfigSource) null);
        return new GenericConfigEntryLeaf(key, value, ConfigEntryType.STRING, dummy);
        }

    @Override
    public void setConfigSchema(ConfigSchema scheme)
        { throw new UnsupportedOperationException("Immutable"); }

    @Override
    public boolean isWriteable()
        { return false; }

    @Override
    public int flush() throws ConfigCheckedException
        { return 0; }

    @Override
    public boolean reload() throws ConfigCheckedException
        { return false; }

    @Override
    public boolean isEmpty()
        { return props.isEmpty(); }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        { return props.stringPropertyNames().iterator(); }

    @Override
    public boolean isClosed()
        { return false; }

    @Override
    public void close()
        { /* no-op */ }

    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        { /* no-op */ }
}