package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Read-only {@link Configuration} view that overlays {@code overrides} on top of {@code parent}.
 * Overrides take precedence. They are converted to the requested type and, if {@code parent} has
 * a schema, checked against it when read; an override that cannot be converted or does not match
 * the schema gives a {@link ConfigException}, not {@code null}.
 * <p>
 * An entry from the overrides has no scope and no location: it was not read from anywhere.
 * <p>
 * Consider using Configuration.put*(key,value,ConfigScope.RUNTIME) instead.
 * The RUNTIME scope is for temporary overrides that are not persisted.
 *
 * @see ConfigUtil#withOverrides(Configuration, Map)
 */
public class OverridingConfiguration extends DelegatingConfigurationView
{
    private final Map<String, Object> overrides;

    /** a conversion a {@link ConfigEntry} offers, e.g. {@code ConfigEntry::getValueAsInteger}. */
    private interface Conversion<T>
    {
        T convert(ConfigEntry entry) throws ConfigCheckedException;
    }

    public OverridingConfiguration(Configuration parent, Map<String, Object> overrides)
        {
        super("OverridingConfiguration", parent);
        this.overrides = new HashMap<>();
        if (overrides != null)
            {
            for (Map.Entry<String, Object> entry : overrides.entrySet())
                { this.overrides.put(entry.getKey(), overrideValue(entry.getValue())); }
            }
        }

    /**
     * @return the override for the key as an entry of the given type, checked against the schema;
     *         null if the override is null.
     */
    private ConfigEntry overrideEntry(String key, ConfigEntryType type)
        {
        Object raw = overrideValue(overrides.get(key));
        if (raw == null)
            { return null; }
        ConfigEntryMetadata noSource = new ConfigEntryMetadata((ConfigSource) null);
        ConfigSchema schema = source.getConfigSchema();
        if (schema != null)
            {
            // the specification makes an override on a SECRET key a secret entry: redacted, left out of exports.
            ConfigEntrySpecification specification = schema.getSpecification(key);
            if (specification != null && !(specification instanceof NullConfigEntrySpecification))
                { noSource.setSpecification(specification); }
            // the value is not put into the message: it may be a secret.
            ConfigEntry asRead = new GenericConfigEntryLeaf(key, raw, ConfigEntryType.STRING, noSource);
            if (!schema.checkConfigEntryValidity(key, asRead))
                { throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID, "the override for key \""+key+"\" does not match the schema"); }
            }
        return new GenericConfigEntryLeaf(key, raw, type, noSource);
        }

    /**
     * the override value as the entry holds it: lists and byte arrays keep their form (their toString() would
     * corrupt them), copied so neither the caller's object nor a reader's result can change the override.
     */
    private static Object overrideValue(Object raw)
        {
        if (raw instanceof byte[])
            { return ((byte[]) raw).clone(); }
        if (raw instanceof Collection)
            {
            List<String> copy = new ArrayList<>();
            for (Object element : (Collection<?>) raw)
                { copy.add(element == null ? null : element.toString()); }
            return Collections.unmodifiableList(copy);
            }
        return raw;
        }

    private <T> T fromOverride(String key, ConfigEntryType type, Conversion<T> conversion)
        {
        ConfigEntry entry = overrideEntry(key, type);
        if (entry == null)
            { return null; }
        try
            {
            return conversion.convert(entry);
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE, "the override for key \""+key+"\" cannot be converted to the requested type");
            }
        }

    @Override
    public String getString(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getString(fullKey); }
        return fromOverride(key, ConfigEntryType.STRING, ConfigEntry::getValueAsString);
        }

    @Override
    public Boolean getBoolean(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getBoolean(fullKey); }
        return fromOverride(key, ConfigEntryType.BOOLEAN, ConfigEntry::getValueAsBoolean);
        }

    @Override
    public Integer getInteger(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getInteger(fullKey); }
        return fromOverride(key, ConfigEntryType.NUMBER, ConfigEntry::getValueAsInteger);
        }

    @Override
    public Long getLong(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getLong(fullKey); }
        return fromOverride(key, ConfigEntryType.NUMBER, ConfigEntry::getValueAsLong);
        }

    @Override
    public Double getDouble(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getDouble(fullKey); }
        return fromOverride(key, ConfigEntryType.NUMBER, ConfigEntry::getValueAsDouble);
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getBigInteger(fullKey); }
        return fromOverride(key, ConfigEntryType.NUMBER, ConfigEntry::getValueAsBigInteger);
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getBigDecimal(fullKey); }
        return fromOverride(key, ConfigEntryType.NUMBER, ConfigEntry::getValueAsBigDecimal);
        }

    @Override
    public byte[] getBytes(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getBytes(fullKey); }
        return fromOverride(key, ConfigEntryType.BYTES, ConfigEntry::getValueAsBytes);
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getListOfStrings(fullKey); }
        return fromOverride(key, ConfigEntryType.MULTIPLE_STRINGS, ConfigEntry::getValueAsStringList);
        }

    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getSecret(fullKey); }
        boolean binary = overrides.get(key) instanceof byte[];
        byte[] tmpBytes = fromOverride(key, binary ? ConfigEntryType.BYTES : ConfigEntryType.STRING, ConfigEntry::getValueAsBytes);
        if (tmpBytes == null)
            { return null; }
        SecretValue secretValue = new BasicSecretValue(tmpBytes, binary ? SecretType.SYMMETRIC_KEY : SecretType.PLAIN_TEXT);
        Arrays.fill(tmpBytes, (byte) 0); // the constructor copied it
        return secretValue;
        }

    /**
     * {@inheritDoc}
     * An override applies whatever the scopes; its entry has no scope and no location.
     */
    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        String key = fullKey.trim();
        if (!overrides.containsKey(key))
            { return super.getConfigEntryFromFullKey(fullKey, scopes); }
        return overrideEntry(key, ConfigEntryType.STRING);
        }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        Set<String> keys = new HashSet<>(overrides.keySet());
        keys.addAll(source.getAllConfigurationKeysFlattened(scopes));
        return Collections.unmodifiableSet(keys);
        }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        {
        Map<String, ConfigEntrySpecification> map = new HashMap<>();
        ConfigSchema schema = source.getConfigSchema();
        for (String key : overrides.keySet())
            {
            ConfigEntrySpecification spec = (schema != null) ? schema.getSpecification(key) : null;
            map.put(key, (spec != null) ? spec : NullConfigEntrySpecification.INSTANCE);
            }
        map.putAll(source.getAllConfigurationKeysWithSchemasFlattened(scopes));
        return Collections.unmodifiableMap(map);
        }

    @Override
    public boolean isEmpty()
        {
        return overrides.isEmpty() && source.isEmpty();
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        Set<String> allKeys = new HashSet<>(overrides.keySet());
        source.getEntryKeyTreeIterator().forEachRemaining(allKeys::add);
        return allKeys.iterator();
        }
}
//___EOF___
