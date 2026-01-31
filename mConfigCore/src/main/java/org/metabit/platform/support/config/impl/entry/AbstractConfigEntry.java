package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.scheme.NullConfigEntrySpecification;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.time.format.DateTimeParseException;

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
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try
            {
            return new URI(s);
            }
        catch (URISyntaxException e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public Path getValueAsPath() throws ConfigCheckedException
        {
        return getValueAsPath(FileSystems.getDefault());
        }

    @Override
    public Path getValueAsPath(FileSystem fs) throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try
            {
            return fs.getPath(s);
            }
        catch (Exception e)
            {
            throw new ConfigCheckedException(e);
            }
        }

    @Override
    public LocalDate getValueAsLocalDate() throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public LocalTime getValueAsLocalTime() throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try { return LocalTime.parse(s); }
        catch (DateTimeParseException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try { return LocalDateTime.parse(s); }
        catch (DateTimeParseException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try { return OffsetDateTime.parse(s); }
        catch (DateTimeParseException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try { return ZonedDateTime.parse(s); }
        catch (DateTimeParseException e) { throw new ConfigCheckedException(e); }
        }

    @Override
    public Duration getValueAsDuration() throws ConfigCheckedException
        {
        String s = getValueAsString();
        if (s == null || s.isEmpty()) return null;
        try { return Duration.parse(s); }
        catch (java.time.format.DateTimeParseException e) { throw new ConfigCheckedException(e); }
        }

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
