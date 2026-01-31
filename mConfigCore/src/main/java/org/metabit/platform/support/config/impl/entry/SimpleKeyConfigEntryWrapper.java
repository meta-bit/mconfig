package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * A wrapper for ConfigEntry that provides a simple key instead of a full path.
 */
public class SimpleKeyConfigEntryWrapper implements ConfigEntry
{
    private final ConfigEntry delegate;
    private final String simpleKey;

    public SimpleKeyConfigEntryWrapper(ConfigEntry delegate, String simpleKey)
        {
        this.delegate = delegate;
        this.simpleKey = simpleKey;
        }

    @Override public String getKey() { return simpleKey; }
    @Override public String getValueAsString() throws ConfigCheckedException { return delegate.getValueAsString(); }
    @Override public Boolean getValueAsBoolean() throws ConfigCheckedException { return delegate.getValueAsBoolean(); }
    @Override public Integer getValueAsInteger() throws ConfigCheckedException { return delegate.getValueAsInteger(); }
    @Override public Long getValueAsLong() throws ConfigCheckedException { return delegate.getValueAsLong(); }
    @Override public Double getValueAsDouble() throws ConfigCheckedException { return delegate.getValueAsDouble(); }
    @Override public java.math.BigInteger getValueAsBigInteger() throws ConfigCheckedException { return delegate.getValueAsBigInteger(); }
    @Override public java.math.BigDecimal getValueAsBigDecimal() throws ConfigCheckedException { return delegate.getValueAsBigDecimal(); }
    @Override public byte[] getValueAsBytes() throws ConfigCheckedException { return delegate.getValueAsBytes(); }
    @Override public java.util.List<String> getValueAsStringList() throws ConfigCheckedException { return delegate.getValueAsStringList(); }
    @Override public java.net.URI getValueAsURI() throws ConfigCheckedException { return delegate.getValueAsURI(); }
    @Override public Path getValueAsPath() throws ConfigCheckedException { return delegate.getValueAsPath(); }
    @Override public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException { return delegate.getValueAsPath(fs); }
    @Override public java.time.LocalDate getValueAsLocalDate() throws ConfigCheckedException { return delegate.getValueAsLocalDate(); }
    @Override public java.time.LocalTime getValueAsLocalTime() throws ConfigCheckedException { return delegate.getValueAsLocalTime(); }
    @Override public java.time.LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException { return delegate.getValueAsLocalDateTime(); }
    @Override public java.time.OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException { return delegate.getValueAsOffsetDateTime(); }
    @Override public java.time.ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException { return delegate.getValueAsZonedDateTime(); }
    @Override public java.time.Duration getValueAsDuration() throws ConfigCheckedException { return delegate.getValueAsDuration(); }
    @Override public ConfigScope getScope() { return delegate.getScope(); }
    @Override public ConfigEntryType getType() { return delegate.getType(); }
    @Override public ConfigLocation getLocation() { return delegate.getLocation(); }
    @Override public java.net.URI getURI() { return delegate.getURI(); }
    @Override public org.metabit.platform.support.config.interfaces.ConfigEntrySpecification getSpecification() throws ConfigCheckedException { return delegate.getSpecification(); }
    @Override public void putString(String value) throws ConfigCheckedException { delegate.putString(value); }
    @Override public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException { delegate.putValue(value, valueType); }
}
