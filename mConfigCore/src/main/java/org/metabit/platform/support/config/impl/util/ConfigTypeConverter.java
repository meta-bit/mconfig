package org.metabit.platform.support.config.impl.util;

import org.metabit.platform.support.config.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.*;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for type conversion between internal ConfigEntry values and typed accessors.
 */
public final class ConfigTypeConverter
{
    private ConfigTypeConverter() {}

    public static String toString(Object value)
        {
        if (value == null) return null;
        if (value instanceof byte[]) return new String((byte[]) value, StandardCharsets.UTF_8);
        return String.valueOf(value);
        }

    public static Boolean toBoolean(Object value)
        {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.valueOf(toString(value));
        }

    public static Integer toInteger(Object value)
        {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.valueOf(toString(value));
        }

    public static Long toLong(Object value)
        {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(toString(value));
        }

    public static Double tryToDouble(Object value)
        {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.valueOf(toString(value));
        }

    public static BigInteger toBigInteger(Object value)
        {
        if (value == null) return null;
        if (value instanceof BigInteger) return (BigInteger) value;
        if (value instanceof Number) return BigInteger.valueOf(((Number) value).longValue());
        return new BigInteger(toString(value));
        }

    public static BigDecimal toBigDecimal(Object value)
        {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return new BigDecimal(toString(value));
        }

    public static byte[] toBytes(Object value)
        {
        if (value == null) return null;
        if (value instanceof byte[]) return (byte[]) value;
        return toString(value).getBytes(StandardCharsets.UTF_8);
        }

    public static List<String> toStringList(Object value)
        {
        if (value == null) return null;
        if (value instanceof List) return (List<String>) value;
        return Arrays.stream(toString(value).split(","))
                .map(String::trim)
                .filter(str->!str.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }

    public static URI toURI(Object value)
        {
        if (value == null) return null;
        if (value instanceof URI) return (URI) value;
        return URI.create(toString(value));
        }

    public static Path toPath(Object value, FileSystem fs)
        {
        if (value == null) return null;
        if (value instanceof Path) return (Path) value;
        return (fs != null ? fs : FileSystems.getDefault()).getPath(toString(value));
        }

    public static LocalDate toLocalDate(Object value)
        {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        return LocalDate.parse(toString(value));
        }

    public static LocalTime toLocalTime(Object value)
        {
        if (value == null) return null;
        if (value instanceof LocalTime) return (LocalTime) value;
        return LocalTime.parse(toString(value));
        }

    public static LocalDateTime toLocalDateTime(Object value)
        {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        return LocalDateTime.parse(toString(value));
        }

    public static OffsetDateTime toOffsetDateTime(Object value)
        {
        if (value == null) return null;
        if (value instanceof OffsetDateTime) return (OffsetDateTime) value;
        return OffsetDateTime.parse(toString(value));
        }

    public static ZonedDateTime toZonedDateTime(Object value)
        {
        if (value == null) return null;
        if (value instanceof ZonedDateTime) return (ZonedDateTime) value;
        return ZonedDateTime.parse(toString(value));
        }

    public static Duration toDuration(Object value)
        {
        if (value == null) return null;
        if (value instanceof Duration) return (Duration) value;
        return Duration.parse(toString(value));
        }
}
