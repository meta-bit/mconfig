package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

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
    private final ConfigEventList     events = new ConfigEventList(1000);
    private final Map<String, Object> overrides;
    private final ConfigSchema        scheme;

    public OverridingConfiguration(Configuration parent, Map<String, Object> overrides)
        {
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.overrides = overrides != null ? new HashMap<>(overrides) : new HashMap<>();
        this.scheme = parent.getConfigSchema();
        }

    @Override
    public ConfigEventList getEvents()
        {
        // expose parent's events; local overrides aren't tracked separately for now
        return parent.getEvents() != null ? parent.getEvents() : events;
        }

    private String rawToString(Object raw)
        {
        return raw == null ? null : raw.toString();
        }

    private boolean validate(String key, String strVal)
        {
        return AdapterValidator.validate(scheme, key, strVal);
        }

    private ConfigEntry getEntryFromOverrides(String fullKey, ConfigEntryType type)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            Object raw = overrides.get(key);
            String strVal = rawToString(raw);
            validate(key, strVal);
            ConfigEntryMetadata dummyMeta = new ConfigEntryMetadata((ConfigSource) null);
            return new GenericConfigEntryLeaf(key, strVal != null ? strVal : "", type, dummyMeta);
            }
        return null;
        }

    @Override
    public String getString(String fullKey)
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            String s = rawToString(overrides.get(key));
            validate(key, s);
            return s;
            }
        return parent.getString(fullKey);
        }

    @Override
    public Boolean getBoolean(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.BOOLEAN);
        if (entry != null) { try { return entry.getValueAsBoolean(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getBoolean(fullKey);
        }

    @Override
    public Integer getInteger(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.NUMBER);
        if (entry != null) { try { return entry.getValueAsInteger(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getInteger(fullKey);
        }

    @Override
    public Long getLong(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.NUMBER);
        if (entry != null) { try { return entry.getValueAsLong(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getLong(fullKey);
        }

    @Override
    public Double getDouble(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.NUMBER);
        if (entry != null) { try { return entry.getValueAsDouble(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getDouble(fullKey);
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.NUMBER);
        if (entry != null) { try { return entry.getValueAsBigInteger(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getBigInteger(fullKey);
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.NUMBER);
        if (entry != null) { try { return entry.getValueAsBigDecimal(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getBigDecimal(fullKey);
        }

    @Override
    public byte[] getBytes(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.BYTES);
        if (entry != null) { try { return entry.getValueAsBytes(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getBytes(fullKey);
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        ConfigEntry entry = getEntryFromOverrides(fullKey, ConfigEntryType.MULTIPLE_STRINGS);
        if (entry != null) { try { return entry.getValueAsStringList(); } catch (ConfigCheckedException e) { return null; } }
        return parent.getListOfStrings(fullKey);
        }

    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        String key = fullKey.trim();
        if (overrides.containsKey(key))
            {
            String strVal = rawToString(overrides.get(key));
            validate(key, strVal);
            return strVal == null ? null : new BasicSecretValue(strVal.getBytes(StandardCharsets.UTF_8), SecretType.PLAIN_TEXT);
            }
        return parent.getSecret(fullKey);
        }

    private void throwReadOnly() { throw new UnsupportedOperationException("OverridingConfiguration is read-only"); }

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
    public ConfigSchema getConfigSchema()
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
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        {
        Map<String, ConfigEntrySpecification> map = new HashMap<>();
        if (scheme != null)
            {
            for (String key : overrides.keySet())
                { map.put(key, scheme.getSpecification(key)); }
            }
        map.putAll(parent.getAllConfigurationKeysWithSchemasFlattened(scopes));
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
            return new GenericConfigEntryLeaf(key, strVal != null ? strVal : "", ConfigEntryType.STRING, dummyMeta);
            }
        return parent.getConfigEntryFromFullKey(fullKey, scopes);
        }

    @Override
    public void setConfigSchema(ConfigSchema scheme)
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
        Set<String> allKeys = new HashSet<>(overrides.keySet());
        parent.getEntryKeyTreeIterator().forEachRemaining(allKeys::add);
        return allKeys.iterator();
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

    @Override
    public void limitScopes(EnumSet<ConfigScope> scopes)
        {
        parent.limitScopes(scopes);
        }
}