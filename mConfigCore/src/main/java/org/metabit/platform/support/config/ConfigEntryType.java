package org.metabit.platform.support.config;

/**
 * <p>ConfigEntryType class.</p>
 *
 * 
 * @version $Id: $Id
 */
public enum ConfigEntryType
{
    /**
     * the most basic type: a text string.
     * by default, in unquoted text formats, trailing whitespace is trimmed.
     * Windows equivalent: REG_SZ, REG_EXPAND_SZ ( REG_EXPAND_SZ is not automatically expanded, yet - Windows ExpandEnvironmentStrings is not available in Java)
     * transferred in Java as String
     */
    STRING,

    /**
     * an integer of unspecified size.
     * <p>
     * Windows equivalents: REG_DWORD, REG_DWORD_LITTLE_ENDIAN, REG_DWORD_BIG_ENDIAN, REG_QWORD, REG_QWORD_LITTLE_ENDIAN
     * transferred in Java as Integer, Long, BigInteger, or BigDecimal
     */
    NUMBER,

    /**
     * a boolean value, true (yes, 1) or false (no, 0)
     * <p>
     * no specific Windows equivalent; custom is to use a DWORD and store 0 or 1.
     * transferred in Java as Boolean object
     */
    BOOLEAN,
    //.....
    // look e.g. at Windows registry types

    /**
     * a sequence of bytes.
     * Windows equivalent: REG_BINARY
     * <p>
     * and the strange REG_NONE
     * transferred in Java as byte[]
     */
    BYTES,

    /**
     * multiple Strings
     * Windows equivalent: REG_MULTI_SZ
     * transferred in Java as List<String></String>
     */
    MULTIPLE_STRINGS,

    /**
     * a single value from a predefined list of options.
     * transferred in Java as String.
     */
    ENUM,

    /**
     * multiple values from a predefined list of options.
     * transferred in Java as List<String>.
     */
    ENUM_SET,

    /**
     * a Uniform Resource Identifier (RFC 3986).
     * transferred in Java as java.net.URI.
     */
    URI,

    /**
     * a path to a file or directory on the filesystem.
     * transferred in Java as java.nio.file.Path.
     */
    FILEPATH,
    /**
     * a calendar date (ISO-8601).
     * transferred in Java as java.time.LocalDate.
     */
    DATE,
    /**
     * a time of day (ISO-8601).
     * transferred in Java as java.time.LocalTime.
     */
    TIME,
    /**
     * a combined date and time (ISO-8601).
     * transferred in Java as java.time.LocalDateTime, OffsetDateTime or ZonedDateTime.
     */
    DATETIME,
    /**
     * a duration (ISO-8601, e.g. PT1H30M).
     * transferred in Java as java.time.Duration.
     */
    DURATION,
}

/*

REG_LINK 	A null-terminated Unicode string that contains the target path of a symbolic link that was created by calling the RegCreateKeyEx function with REG_OPTION_CREATE_LINK.
 */
