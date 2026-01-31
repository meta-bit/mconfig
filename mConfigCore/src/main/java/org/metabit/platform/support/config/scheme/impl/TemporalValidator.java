package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Map;

/**
 * Validator for temporal types (DATE, TIME, DATETIME).
 */
public class TemporalValidator
{
    private final ConfigEntryType type;
    private final String after;
    private final String before;
    private final boolean requireOffset;

    public TemporalValidator(ConfigEntryType type, Map<String, Object> flags)
        {
        this.type = type;
        this.after = (String) flags.get("AFTER");
        this.before = (String) flags.get("BEFORE");
        this.requireOffset = Boolean.TRUE.equals(flags.get("REQUIRE_OFFSET"));
        }

    public boolean validate(ConfigEntry entry)
        {
        try
            {
            // System.out.println("[DEBUG_LOG] TemporalValidator.validate: type=" + type + ", entry=" + entry.getKey() + ", value=" + entry.getValueAsString());
            switch (type)
                {
                case DATE:
                    return validateDate(entry.getValueAsLocalDate());
                case TIME:
                    return validateTime(entry.getValueAsLocalTime());
                case DATETIME:
                    return validateDateTime(entry);
                default:
                    return true;
                }
            }
        catch (Exception e)
            {
            // e.printStackTrace();
            return false;
            }
        }

    private boolean validateDate(LocalDate val)
        {
        if (val == null) return false;
        if (after != null)
            {
            LocalDate a = parseDate(after);
            if (!val.isAfter(a)) return false;
            }
        if (before != null)
            {
            LocalDate b = parseDate(before);
            if (!val.isBefore(b)) return false;
            }
        return true;
        }

    private LocalDate parseDate(String s)
        {
        if ("now".equalsIgnoreCase(s)) return LocalDate.now();
        return LocalDate.parse(s);
        }

    private boolean validateTime(LocalTime val)
        {
        if (val == null) return false;
        if (after != null)
            {
            LocalTime a = parseTime(after);
            if (!val.isAfter(a)) return false;
            }
        if (before != null)
            {
            LocalTime b = parseTime(before);
            if (!val.isBefore(b)) return false;
            }
        return true;
        }

    private LocalTime parseTime(String s)
        {
        if ("now".equalsIgnoreCase(s)) return LocalTime.now();
        return LocalTime.parse(s);
        }

    private boolean validateDateTime(ConfigEntry entry)
        {
        Temporal val = null;
        boolean hasOffset = false;
        try 
            { 
            val = entry.getValueAsOffsetDateTime(); 
            if (val != null) hasOffset = true;
            }
        catch (Exception e)
            {
            try 
                { 
                val = entry.getValueAsZonedDateTime(); 
                if (val != null) hasOffset = true;
                }
            catch (Exception e3) 
                {
                try { val = entry.getValueAsLocalDateTime(); }
                catch (Exception e2) { return false; }
                }
            }

        if (requireOffset && !hasOffset)
            {
            return false;
            }

        if (after != null)
            {
            Temporal a = parseDateTime(after);
            if (!isAfter(val, a)) return false;
            }
        if (before != null)
            {
            Temporal b = parseDateTime(before);
            if (!isBefore(val, b)) return false;
            }
        return true;
        }

    private Temporal parseDateTime(String s)
        {
        if ("now".equalsIgnoreCase(s)) return OffsetDateTime.now();
        try { return OffsetDateTime.parse(s); }
        catch (DateTimeParseException e)
            {
            try { return LocalDateTime.parse(s); }
            catch (DateTimeParseException e2)
                {
                return ZonedDateTime.parse(s);
                }
            }
        }

    private boolean isAfter(Temporal t1, Temporal t2)
        {
        if (t1 instanceof OffsetDateTime && t2 instanceof OffsetDateTime)
            return ((OffsetDateTime) t1).isAfter((OffsetDateTime) t2);
        if (t1 instanceof LocalDateTime && t2 instanceof LocalDateTime)
            return ((LocalDateTime) t1).isAfter((LocalDateTime) t2);
        if (t1 instanceof ZonedDateTime && t2 instanceof ZonedDateTime)
            return ((ZonedDateTime) t1).isAfter((ZonedDateTime) t2);
        
        // Fallback to string comparison for mixed types or other cases? 
        // ISO format usually sorts correctly.
        return t1.toString().compareTo(t2.toString()) > 0;
        }

    private boolean isBefore(Temporal t1, Temporal t2)
        {
        if (t1 instanceof OffsetDateTime && t2 instanceof OffsetDateTime)
            return ((OffsetDateTime) t1).isBefore((OffsetDateTime) t2);
        if (t1 instanceof LocalDateTime && t2 instanceof LocalDateTime)
            return ((LocalDateTime) t1).isBefore((LocalDateTime) t2);
        if (t1 instanceof ZonedDateTime && t2 instanceof ZonedDateTime)
            return ((ZonedDateTime) t1).isBefore((ZonedDateTime) t2);
        
        return t1.toString().compareTo(t2.toString()) < 0;
        }
}
