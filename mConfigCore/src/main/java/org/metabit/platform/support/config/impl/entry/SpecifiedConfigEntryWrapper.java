package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;

/**
 * (decorator pattern)
 * a wrapper for ConfigEntry which applies a specification (e.g. from a scheme)
 */
public class SpecifiedConfigEntryWrapper implements ConfigEntry
{
    private final ConfigEntry delegate;
    private final ConfigEntrySpecification specification;

    public SpecifiedConfigEntryWrapper(ConfigEntry delegate, ConfigEntrySpecification specification)
        {
        this.delegate = delegate;
        this.specification = specification;
        }

    @Override public String getValueAsString() throws ConfigCheckedException { return delegate.getValueAsString(); }
    @Override public Boolean getValueAsBoolean() throws ConfigCheckedException { return delegate.getValueAsBoolean(); }
    @Override public Integer getValueAsInteger() throws ConfigCheckedException { return delegate.getValueAsInteger(); }
    @Override public Long getValueAsLong() throws ConfigCheckedException { return delegate.getValueAsLong(); }
    @Override public Double getValueAsDouble() throws ConfigCheckedException { return delegate.getValueAsDouble(); }
    @Override public BigInteger getValueAsBigInteger() throws ConfigCheckedException { return delegate.getValueAsBigInteger(); }
    @Override public BigDecimal getValueAsBigDecimal() throws ConfigCheckedException { return delegate.getValueAsBigDecimal(); }
    @Override public byte[] getValueAsBytes() throws ConfigCheckedException { return delegate.getValueAsBytes(); }
    @Override public List<String> getValueAsStringList() throws ConfigCheckedException { return delegate.getValueAsStringList(); }
    @Override public URI getValueAsURI() throws ConfigCheckedException { return delegate.getValueAsURI(); }
    @Override public Path getValueAsPath() throws ConfigCheckedException { return delegate.getValueAsPath(); }
    @Override public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException { return delegate.getValueAsPath(fs); }
    @Override public java.time.LocalDate getValueAsLocalDate() throws ConfigCheckedException { return delegate.getValueAsLocalDate(); }
    @Override public java.time.LocalTime getValueAsLocalTime() throws ConfigCheckedException { return delegate.getValueAsLocalTime(); }
    @Override public java.time.LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException { return delegate.getValueAsLocalDateTime(); }
    @Override public java.time.OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException { return delegate.getValueAsOffsetDateTime(); }
    @Override public java.time.ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException { return delegate.getValueAsZonedDateTime(); }
    @Override public java.time.Duration getValueAsDuration() throws ConfigCheckedException { return delegate.getValueAsDuration(); }
    @Override public String getKey() { return delegate.getKey(); }
    @Override public ConfigScope getScope() { return delegate.getScope(); }
    @Override public ConfigEntryType getType() { return delegate.getType(); }
    @Override public ConfigLocation getLocation() { return delegate.getLocation(); }
    @Override public URI getURI() { return delegate.getURI(); }
    @Override public ConfigEntrySpecification getSpecification() throws ConfigCheckedException { return specification; }
    @Override public boolean isSecret() { return specification.isSecret() || delegate.isSecret(); }

    @Override
    public void putString(String value) throws ConfigCheckedException
        {
        delegate.putString(value);
        }

    @Override
    public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException
        {
        delegate.putValue(value, valueType);
        }

    @Override
    public String toString()
        {
        if (isSecret())
            {
            return delegate.getClass().getSimpleName() + "{key='" + getKey() + "', value='[REDACTED]', scope=" + getScope() + "}";
            }
        return delegate.toString();
        }
}
