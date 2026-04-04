package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.util.ConfigTypeConverter;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.*;
import java.util.List;

/**
 * A unified implementation of ConfigEntry leaf nodes that handles all type conversions
 * through ConfigTypeConverter.
 */
public class GenericConfigEntryLeaf extends AbstractConfigEntry implements SecretConfigEntry
{
    private Object value;
    private ConfigEntryType type;

    public GenericConfigEntryLeaf(String key, Object value, ConfigEntryType type, ConfigEntryMetadata meta)
        {
        super(key, meta);
        this.value = value;
        this.type = (type != null) ? type : ConfigEntryType.STRING;
        }

    @Override
    public SecretValue getSecretValue()
        {
        if (value instanceof SecretValue) return (SecretValue) value;
        if (!isSecret()) return null;
        try
            {
            byte[] bytes = getValueAsBytes();
            return new BasicSecretValue(bytes, getType() == ConfigEntryType.BYTES ? SecretType.SYMMETRIC_KEY : SecretType.PLAIN_TEXT);
            }
        catch (ConfigCheckedException e)
            {
            return null;
            }
        }

    @Override
    public ConfigEntryType getType() { return type; }

    @Override
    public String getValueAsString() throws ConfigCheckedException { return ConfigTypeConverter.toString(value); }

    @Override
    public Boolean getValueAsBoolean() throws ConfigCheckedException { return ConfigTypeConverter.toBoolean(value); }

    @Override
    public Integer getValueAsInteger() throws ConfigCheckedException
        {
        try { return ConfigTypeConverter.toInteger(value); }
        catch (NumberFormatException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public Long getValueAsLong() throws ConfigCheckedException
        {
        try { return ConfigTypeConverter.toLong(value); }
        catch (NumberFormatException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public Double getValueAsDouble() throws ConfigCheckedException
        {
        try { return ConfigTypeConverter.tryToDouble(value); }
        catch (NumberFormatException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public BigInteger getValueAsBigInteger() throws ConfigCheckedException
        {
        try { return ConfigTypeConverter.toBigInteger(value); }
        catch (NumberFormatException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public BigDecimal getValueAsBigDecimal() throws ConfigCheckedException
        {
        try { return ConfigTypeConverter.toBigDecimal(value); }
        catch (NumberFormatException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public byte[] getValueAsBytes() throws ConfigCheckedException { return ConfigTypeConverter.toBytes(value); }

    @Override
    public List<String> getValueAsStringList() throws ConfigCheckedException { return ConfigTypeConverter.toStringList(value); }

    @Override
    public URI getValueAsURI() throws ConfigCheckedException { return ConfigTypeConverter.toURI(value); }

    @Override
    public Path getValueAsPath() throws ConfigCheckedException { return ConfigTypeConverter.toPath(value, null); }

    @Override
    public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException { return ConfigTypeConverter.toPath(value, fs); }

    @Override
    public LocalDate getValueAsLocalDate() throws ConfigCheckedException { return ConfigTypeConverter.toLocalDate(value); }

    @Override
    public LocalTime getValueAsLocalTime() throws ConfigCheckedException { return ConfigTypeConverter.toLocalTime(value); }

    @Override
    public LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException { return ConfigTypeConverter.toLocalDateTime(value); }

    @Override
    public OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException { return ConfigTypeConverter.toOffsetDateTime(value); }

    @Override
    public ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException { return ConfigTypeConverter.toZonedDateTime(value); }

    @Override
    public Duration getValueAsDuration() throws ConfigCheckedException { return ConfigTypeConverter.toDuration(value); }

    @Override
    public void putString(String newValue) throws ConfigCheckedException
        {
        ConfigStorageInterface storage = this.meta.getLocation().getStorage();
        if (!storage.isGenerallyWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        this.value = newValue;
        this.writeBack();
        }

    @Override
    public void putValue(Object newValue, ConfigEntryType newValueType) throws ConfigCheckedException
        {
        ConfigStorageInterface storage = this.meta.getLocation().getStorage();
        if (!storage.isGenerallyWriteable())
            {
                throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        this.value = newValue;
        this.type = newValueType;
        this.writeBack();
        }

    @Override
    public String toString()
        {
        return "GenericConfigEntryLeaf{" +
                "key='" + key + '\'' +
                ", value='" + (isSecret() ? "[REDACTED]" : ConfigTypeConverter.toString(value)) + '\'' +
                ", scope=" + getScope() +
                ", type=" + type +
                '}';
        }
}
