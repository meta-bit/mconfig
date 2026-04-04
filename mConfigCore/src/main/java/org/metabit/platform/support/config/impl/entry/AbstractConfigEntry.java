package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.util.ConfigTypeConverter;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.Duration;

/**
 * Common base class for ConfigEntry implementations.
 */
public abstract class AbstractConfigEntry implements ConfigEntry
    {
    protected final String              key;
    protected final ConfigEntryMetadata meta;

    protected AbstractConfigEntry(String key, ConfigEntryMetadata meta)
        {
        this.key = key;
        this.meta = meta;
        }

    @Override
    public String getKey()
        { return key; }

    @Override
    public ConfigScope getScope()
        { return (meta != null) ? meta.getScope() : null; }

    @Override
    public ConfigLocation getLocation()
        { return (meta != null) ? meta.getLocation() : null; }

    @Override
    public URI getURI()
        { return (meta != null && meta.getLocation() != null) ? meta.getLocation().getURI(key, null) : null; }

    @Override
    public URI getValueAsURI() throws ConfigCheckedException
        { return ConfigTypeConverter.toURI(this.getValueAsString()); }

    @Override
    public Path getValueAsPath() throws ConfigCheckedException
        { return ConfigTypeConverter.toPath(this.getValueAsString(), null); }

    @Override
    public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException
        { return ConfigTypeConverter.toPath(this.getValueAsString(), fs); }

    @Override
    public LocalDate getValueAsLocalDate() throws ConfigCheckedException
        { return ConfigTypeConverter.toLocalDate(this.getValueAsString()); }

    @Override
    public LocalTime getValueAsLocalTime() throws ConfigCheckedException
        { return ConfigTypeConverter.toLocalTime(this.getValueAsString()); }

    @Override
    public LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException
        { return ConfigTypeConverter.toLocalDateTime(this.getValueAsString()); }

    @Override
    public OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException
        { return ConfigTypeConverter.toOffsetDateTime(this.getValueAsString()); }

    @Override
    public ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException
        { return ConfigTypeConverter.toZonedDateTime(this.getValueAsString()); }

    @Override
    public Duration getValueAsDuration() throws ConfigCheckedException
        { return ConfigTypeConverter.toDuration(this.getValueAsString()); }

    @Override
    public ConfigEntrySpecification getSpecification() throws ConfigCheckedException
        {
        if (meta != null && meta.getSpecification() != null)
            {
            return meta.getSpecification();
            }
        return NullConfigEntrySpecification.INSTANCE;
        }

    @Override
    public boolean isSecret()
        {
        try
            {
            return getSpecification().isSecret();
            }
        catch (ConfigCheckedException e)
            {
            return false;
            }
        }

    @Override
    public String getComment()
        {
        return (meta != null) ? meta.getComment() : null;
        }

    @Override
    public void setComment(String comment)
        {
        if (meta != null)
            {
            meta.setComment(comment);
            }
        }

    @Override
    public abstract ConfigEntryType getType();

    public ConfigEntryMetadata getMeta()
        {
        return meta;
        }

    /**
     * Helper to write the entry back to its source.
     */
    protected void writeBack() throws ConfigCheckedException
        {
        if (meta != null && meta.getSource() != null && meta.getSource().getLayer() != null)
            {
            meta.getSource().getLayer().writeEntry(this);
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        }
    }
