package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.time.Duration;

/**
 * A ConfigEntry implementation that preserves the original type of the value.
 */
public class TypedConfigEntryLeaf extends AbstractConfigEntry
    {
    private final Object          value;
    private final ConfigEntryType type;

    public TypedConfigEntryLeaf(String key, Object value, ConfigEntryType type, ConfigEntryMetadata meta)
        {
        super(key, meta);
        this.value = value;
        this.type = type;
        validate();
        }

    private void validate()
        {
        if (meta != null && meta.getSpecification() != null)
            {
            if (!meta.getSpecification().validateEntry(this))
                {
                // @TODO: What should we do here? Log a warning? Throw an exception?
                // For now, let's just log if a logger is available.
                }
            }
        }

    @Override
    public ConfigEntryType getType()
        {
        return type;
        }

    @Override
    public String getValueAsString() throws ConfigCheckedException
        {
        return String.valueOf(value);
        }

    @Override
    public Boolean getValueAsBoolean() throws ConfigCheckedException
        {
        if (value instanceof Boolean)
            {
            return (Boolean) value;
            }
        return Boolean.parseBoolean(getValueAsString());
        }

    @Override
    public Integer getValueAsInteger() throws ConfigCheckedException
        {
        if (value instanceof Number)
            {
            return ((Number) value).intValue();
            }
        try
            {
            return Integer.valueOf(getValueAsString());
            }
        catch (NumberFormatException e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public Long getValueAsLong() throws ConfigCheckedException
        {
        if (value instanceof Number)
            {
            return ((Number) value).longValue();
            }
        try
            {
            return Long.valueOf(getValueAsString());
            }
        catch (NumberFormatException e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public Double getValueAsDouble() throws ConfigCheckedException
        {
        if (value instanceof Number)
            {
            return ((Number) value).doubleValue();
            }
        try
            {
            return Double.valueOf(getValueAsString());
            }
        catch (NumberFormatException e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public BigInteger getValueAsBigInteger() throws ConfigCheckedException
        {
        if (value instanceof BigInteger)
            {
            return (BigInteger) value;
            }
        try
            {
            return new BigInteger(getValueAsString());
            }
        catch (NumberFormatException e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public BigDecimal getValueAsBigDecimal() throws ConfigCheckedException
        {
        if (value instanceof BigDecimal)
            {
            return (BigDecimal) value;
            }
        try
            {
            return new BigDecimal(getValueAsString());
            }
        catch (NumberFormatException e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public byte[] getValueAsBytes() throws ConfigCheckedException
        {
        if (value instanceof byte[])
            {
            return (byte[]) value;
            }
        return getValueAsString().getBytes(StandardCharsets.UTF_8);
        }

    @Override
    public List<String> getValueAsStringList() throws ConfigCheckedException
        {
        if (value instanceof List)
            {
            return (List<String>) value;
            }
        return Collections.singletonList(getValueAsString());
        }

    @Override
    public URI getValueAsURI() throws ConfigCheckedException
        {
        if (value instanceof URI)
            {
            return (URI) value;
            }
        return super.getValueAsURI();
        }

    @Override
    public Path getValueAsPath() throws ConfigCheckedException
        {
        if (value instanceof Path)
            {
            return (Path) value;
            }
        return super.getValueAsPath();
        }

    @Override
    public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException
        {
        if (value instanceof Path)
            {
            Path p = (Path) value;
            if (p.getFileSystem().equals(fs))
                {
                return p;
                }
            }
        return super.getValueAsPath(fs);
        }

    @Override
    public LocalDate getValueAsLocalDate() throws ConfigCheckedException
        {
        if (value instanceof LocalDate) return (LocalDate) value;
        return super.getValueAsLocalDate();
        }

    @Override
    public LocalTime getValueAsLocalTime() throws ConfigCheckedException
        {
        if (value instanceof LocalTime) return (LocalTime) value;
        return super.getValueAsLocalTime();
        }

    @Override
    public LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException
        {
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        return super.getValueAsLocalDateTime();
        }

    @Override
    public OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException
        {
        if (value instanceof OffsetDateTime) return (OffsetDateTime) value;
        return super.getValueAsOffsetDateTime();
        }

    @Override
    public ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException
        {
        if (value instanceof ZonedDateTime) return (ZonedDateTime) value;
        return super.getValueAsZonedDateTime();
        }

    /**
     * Get value as `java.time.Duration`.
     */
    public java.time.Duration getValueAsDuration() throws ConfigCheckedException
        {
        if (value instanceof java.time.Duration)
            return (java.time.Duration) value;
        return java.time.Duration.parse(getValueAsString());
        }

    @Override
    public void putString(String value) throws ConfigCheckedException
        {
        putValue(value, ConfigEntryType.STRING);
        }

    @Override
    public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException
        {
        ConfigStorageInterface storage = this.meta.getLocation().getStorage();
        if (!storage.isGenerallyWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        // Ideally we should update our own value and type here, but since TypedConfigEntryLeaf is immutable for now
        // we rely on the layer writing it back and potentially replacing it.
        // For consistency with StringConfigEntryLeaf:
        this.writeBack(); 
        }

    @Override
    public String toString()
        {
        return "TypedConfigEntryLeaf{" +
                "key='" + key + '\'' +
                ", value=" + (isSecret() ? "[REDACTED]" : value) +
                ", type=" + type +
                ", scope=" + getScope() +
                '}';
        }
    }
