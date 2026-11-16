package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Read-only {@link Configuration} view adapting {@link Properties}.
 * It behaves like the Properties behind it: a key that is not there gives {@code null}, no
 * exception. The typed getters convert the property string; a value that cannot be converted
 * throws {@link ConfigException} (CONVERSION_FAILURE), as parsing the string in plain Java would.
 * No schema, no locations.
 * <p>
 * {@link #getSecret(String)} returns the property as a {@link SecretValue}. That protects it from
 * here on only: in the Properties it is a plain String. The first such access is logged (INFO,
 * once per JVM) with a hint to read secrets through mConfig itself.
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
public class PropertiesConfiguration extends ReadOnlyConfigurationView
{
    /** once per JVM, not per view or key: views may be created in large numbers. */
    private static final AtomicBoolean SECRET_ACCESS_REPORTED = new AtomicBoolean(false);

    private final Properties      props;
    private final ConfigEventList events = new ConfigEventList(1000);

    /** a conversion a {@link ConfigEntry} offers, e.g. {@code ConfigEntry::getValueAsInteger}. */
    private interface Conversion<T>
    {
        T convert(ConfigEntry entry) throws ConfigCheckedException;
    }

    public PropertiesConfiguration(Properties props)
        {
        super("PropertiesConfiguration");
        this.props = props != null ? props : new Properties();
        }

    @Override
    public ConfigEventList getEvents()
        {
        return events;
        }

    /**
     * @return the property converted to the requested type; null if there is no such property.
     * @throws ConfigException CONVERSION_FAILURE if the property cannot be converted.
     */
    private <T> T converted(String fullKey, ConfigEntryType type, Conversion<T> conversion)
        {
        String key = fullKey.trim();
        String value = props.getProperty(key);
        if (value == null)
            { return null; }
        ConfigEntry entry = new GenericConfigEntryLeaf(key, value, type, new ConfigEntryMetadata((ConfigSource) null));
        try
            {
            return conversion.convert(entry);
            }
        catch (ConfigCheckedException e)
            {
            // the value is not put into the message: it may be a secret.
            throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE, "the property \""+key+"\" cannot be converted to the requested type");
            }
        }

    @Override
    public String getString(String fullKey)
        { return props.getProperty(fullKey.trim()); }

    @Override
    public Boolean getBoolean(String fullKey)
        { return converted(fullKey, ConfigEntryType.BOOLEAN, ConfigEntry::getValueAsBoolean); }

    @Override
    public Integer getInteger(String fullKey)
        { return converted(fullKey, ConfigEntryType.NUMBER, ConfigEntry::getValueAsInteger); }

    @Override
    public Long getLong(String fullKey)
        { return converted(fullKey, ConfigEntryType.NUMBER, ConfigEntry::getValueAsLong); }

    @Override
    public Double getDouble(String fullKey)
        { return converted(fullKey, ConfigEntryType.NUMBER, ConfigEntry::getValueAsDouble); }

    @Override
    public BigInteger getBigInteger(String fullKey)
        { return converted(fullKey, ConfigEntryType.NUMBER, ConfigEntry::getValueAsBigInteger); }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
        { return converted(fullKey, ConfigEntryType.NUMBER, ConfigEntry::getValueAsBigDecimal); }

    @Override
    public byte[] getBytes(String fullKey)
        { return converted(fullKey, ConfigEntryType.BYTES, ConfigEntry::getValueAsBytes); }

    @Override
    public List<String> getListOfStrings(String fullKey)
        { return converted(fullKey, ConfigEntryType.MULTIPLE_STRINGS, ConfigEntry::getValueAsStringList); }

    /**
     * {@inheritDoc}
     * The property is wrapped as a secret; in the Properties it stays a plain String.
     *
     * @return the property as a SecretValue; null if there is no such property.
     */
    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        String key = fullKey.trim();
        String value = props.getProperty(key);
        if (value == null)
            { return null; }
        if (SECRET_ACCESS_REPORTED.compareAndSet(false, true))
            {
            // there is no factory here, hence no ConfigLoggingInterface; the JDK's logger takes it.
            System.getLogger("mConfig").log(System.Logger.Level.INFO,
                    "a secret (\""+LogSafe.text(key)+"\") was read from java.util.Properties through PropertiesConfiguration: "
                    +"there it is a plain String, unprotected. Read secrets through mConfig itself (schema entry with \"SECRET\": true). "
                    +"This is reported once.");
            }
        byte[] tmpBytes = value.getBytes(StandardCharsets.UTF_8);
        SecretValue secretValue = new BasicSecretValue(tmpBytes, SecretType.PLAIN_TEXT);
        Arrays.fill(tmpBytes, (byte) 0); // the constructor copied it
        return secretValue;
        }

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        { throw new UnsupportedOperationException("PropertiesConfiguration does not support updates"); }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        { throw new UnsupportedOperationException("PropertiesConfiguration does not support updates"); }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        { throw new UnsupportedOperationException("PropertiesConfiguration does not support updates"); }

    @Override
    public ConfigSchema getConfigSchema()
        { return null; }

    @Override
    public List<ConfigLocation> getSourceLocations()
        { return Collections.emptyList(); }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        { return new HashSet<>(props.stringPropertyNames()); }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        {
        Map<String, ConfigEntrySpecification> result = new HashMap<>();
        for (String key : props.stringPropertyNames())
            { result.put(key, NullConfigEntrySpecification.INSTANCE); }
        return result;
        }

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
    public boolean reload() throws ConfigCheckedException
        { return false; }

    @Override
    public boolean isEmpty()
        { return props.stringPropertyNames().isEmpty(); } // props.isEmpty() ignores the Properties defaults

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        { return props.stringPropertyNames().iterator(); }

    @Override
    public boolean isClosed()
        { return false; }

}
//___EOF___
