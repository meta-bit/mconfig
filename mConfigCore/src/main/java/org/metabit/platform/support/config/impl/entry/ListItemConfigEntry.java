package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

/**
 * A ConfigEntry implementation representing a single item within a list.
 */
public class ListItemConfigEntry extends AbstractConfigEntry
{
    private final Object itemValue;
    private final ConfigEntryType itemType;
    private final int index;

    public ListItemConfigEntry(String key, int index, Object itemValue, ConfigEntryType itemType, ConfigEntryMetadata meta)
        {
        super(key, meta);
        this.index = index;
        this.itemValue = itemValue;
        this.itemType = itemType;
        }

    @Override
    public String getKey()
        {
        return String.valueOf(index);
        }

    @Override
    public ConfigEntryType getType()
        {
        return itemType;
        }

    @Override
    public String getValueAsString() throws ConfigCheckedException
        {
        return (itemValue == null) ? null : String.valueOf(itemValue);
        }

    @Override
    public Boolean getValueAsBoolean() throws ConfigCheckedException
        {
        if (itemValue == null) return null;
        if (itemValue instanceof Boolean) return (Boolean) itemValue;
        return Boolean.valueOf(getValueAsString());
        }

    @Override
    public Integer getValueAsInteger() throws ConfigCheckedException
        {
        if (itemValue == null) return null;
        if (itemValue instanceof Number) return ((Number) itemValue).intValue();
        return Integer.valueOf(getValueAsString());
        }

    @Override
    public Long getValueAsLong() throws ConfigCheckedException
        {
        if (itemValue == null) return null;
        if (itemValue instanceof Number) return ((Number) itemValue).longValue();
        return Long.valueOf(getValueAsString());
        }

    @Override
    public Double getValueAsDouble() throws ConfigCheckedException
        {
        if (itemValue == null) return null;
        if (itemValue instanceof Number) return ((Number) itemValue).doubleValue();
        return Double.valueOf(getValueAsString());
        }

    @Override
    public java.math.BigInteger getValueAsBigInteger() throws ConfigCheckedException
        {
        if (itemValue == null) return null;
        if (itemValue instanceof java.math.BigInteger) return (java.math.BigInteger) itemValue;
        return new java.math.BigInteger(getValueAsString());
        }

    @Override
    public java.math.BigDecimal getValueAsBigDecimal() throws ConfigCheckedException
        {
        if (itemValue == null) return null;
        if (itemValue instanceof java.math.BigDecimal) return (java.math.BigDecimal) itemValue;
        if (itemValue instanceof Number) return new java.math.BigDecimal(itemValue.toString());
        return new java.math.BigDecimal(getValueAsString());
        }

    @Override
    public byte[] getValueAsBytes() throws ConfigCheckedException
        {
        if (itemValue instanceof byte[]) return (byte[]) itemValue;
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.CONVERSION_FAILURE);
        }

    @Override
    public List<String> getValueAsStringList() throws ConfigCheckedException
        {
        return Collections.singletonList(getValueAsString());
        }

    @Override
    public URI getValueAsURI() throws ConfigCheckedException
        {
        if (itemValue instanceof URI) return (URI) itemValue;
        return super.getValueAsURI();
        }

    @Override
    public Path getValueAsPath() throws ConfigCheckedException
        {
        if (itemValue instanceof Path) return (Path) itemValue;
        return super.getValueAsPath();
        }

    @Override
    public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException
        {
        if (itemValue instanceof Path)
            {
            Path p = (Path) itemValue;
            if (p.getFileSystem().equals(fs)) return p;
            }
        return super.getValueAsPath(fs);
        }

    @Override
    public LocalDate getValueAsLocalDate() throws ConfigCheckedException
        {
        if (itemValue instanceof LocalDate) return (LocalDate) itemValue;
        return super.getValueAsLocalDate();
        }

    @Override
    public LocalTime getValueAsLocalTime() throws ConfigCheckedException
        {
        if (itemValue instanceof LocalTime) return (LocalTime) itemValue;
        return super.getValueAsLocalTime();
        }

    @Override
    public LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException
        {
        if (itemValue instanceof LocalDateTime) return (LocalDateTime) itemValue;
        return super.getValueAsLocalDateTime();
        }

    @Override
    public OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException
        {
        if (itemValue instanceof OffsetDateTime) return (OffsetDateTime) itemValue;
        return super.getValueAsOffsetDateTime();
        }

    @Override
    public ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException
        {
        if (itemValue instanceof ZonedDateTime) return (ZonedDateTime) itemValue;
        return super.getValueAsZonedDateTime();
        }

    @Override
    public void putString(String value) throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
        }

    @Override
    public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
        }
}
