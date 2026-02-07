package org.metabit.platform.support.config;

import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.Duration;

/**
 * the entry in a configuration (the thing you'll want to access).
 *
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigEntry
{
    /**
     * if the entry is of type string, return its value.
     * if the entry is of another type, try to convert to string.
     *
     * @return String representation of the entry value.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if entry value is not convertible.
     */
    String          getValueAsString()      throws ConfigCheckedException;
    /**
     * <p>getValueAsBoolean.</p>
     *
     * @return a {@link java.lang.Boolean} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    Boolean         getValueAsBoolean()     throws ConfigCheckedException;
    /**
     * <p>getValueAsInteger.</p>
     *
     * @return a {@link java.lang.Integer} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    Integer         getValueAsInteger()     throws ConfigCheckedException;
    /**
     * <p>getValueAsLong.</p>
     *
     * @return a {@link java.lang.Long} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    Long            getValueAsLong()     throws ConfigCheckedException;
    /**
     * <p>getValueAsDouble.</p>
     *
     * @return a {@link java.lang.Double} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    Double          getValueAsDouble()      throws ConfigCheckedException;
    /**
     * <p>getValueAsBigInteger.</p>
     *
     * @return a {@link java.math.BigInteger} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    BigInteger      getValueAsBigInteger()  throws ConfigCheckedException;
    /**
     * <p>getValueAsBigDecimal.</p>
     *
     * @return a {@link java.math.BigDecimal} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    BigDecimal      getValueAsBigDecimal()  throws ConfigCheckedException;
    /**
     * <p>getValueAsBytes.</p>
     *
     * @return an array of {@link byte} objects
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    byte[]          getValueAsBytes()       throws ConfigCheckedException;
    /**
     * <p>getValueAsChars.</p>
     *
     * @return an array of {@link char} objects
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    default char[]  getValueAsChars()       throws ConfigCheckedException
        {
        byte[] bytes = getValueAsBytes();
        if (bytes == null) return null;
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) chars[i] = (char) bytes[i];
        return chars;
        }
    /**
     * <p>getValueAsStringList.</p>
     *
     * @return a {@link java.util.List} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    List<String>    getValueAsStringList()  throws ConfigCheckedException;

    /**
     * <p>getValueAsURI.</p>
     *
     * @return a {@link java.net.URI} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a URI.
     */
    URI             getValueAsURI()         throws ConfigCheckedException;

    /**
     * <p>getValueAsPath.</p>
     *
     * @return a {@link java.nio.file.Path} object using the default FileSystem.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a Path.
     */
    Path            getValueAsPath()        throws ConfigCheckedException;

    /**
     * <p>getValueAsPath.</p>
     *
     * @param fs the FileSystem to use for path conversion.
     * @return a {@link java.nio.file.Path} object.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a Path.
     */
    Path            getValueAsPath(FileSystem fs) throws ConfigCheckedException;

    /**
     * <p>getValueAsLocalDate.</p>
     *
     * @return a {@link java.time.LocalDate} object.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a LocalDate.
     */
    LocalDate       getValueAsLocalDate() throws ConfigCheckedException;

    /**
     * <p>getValueAsLocalTime.</p>
     *
     * @return a {@link java.time.LocalTime} object.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a LocalTime.
     */
    LocalTime       getValueAsLocalTime() throws ConfigCheckedException;

    /**
     * <p>getValueAsLocalDateTime.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a LocalDateTime.
     */
    LocalDateTime   getValueAsLocalDateTime() throws ConfigCheckedException;

    /**
     * <p>getValueAsOffsetDateTime.</p>
     *
     * @return a {@link java.time.OffsetDateTime} object.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a OffsetDateTime.
     */
    OffsetDateTime  getValueAsOffsetDateTime() throws ConfigCheckedException;

    /**
     * <p>getValueAsZonedDateTime.</p>
     *
     * @return a {@link java.time.ZonedDateTime} object.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the value cannot be converted to a ZonedDateTime.
     */
    ZonedDateTime   getValueAsZonedDateTime() throws ConfigCheckedException;

    /**
     * Get value as `java.time.Duration` (ISO-8601 format, e.g. "PT30S").
     *
     * @return Duration value
     * @throws ConfigCheckedException if value cannot be converted to Duration
     */
    Duration getValueAsDuration() throws ConfigCheckedException;


    /**
     * the key of this entry (usually: name, without path or context)
     *
     * @return key of the entry, as String.
     */
    String getKey();
    /**
     * <p>getScope.</p>
     *
     * @return the Scope this entry was found in.
     */
    ConfigScope getScope();

    /**
     * shortcut to the specification type, if there is one. Fallback to STRING type(s) if none is provided.
     *
     * @return type of the configuration entry.
     */
    ConfigEntryType getType();

    /**
     * get the full location this entry is stored in.
     *
     * @return the location of the entry.
     */
    ConfigLocation getLocation();
    /**
     * <p>getURI.</p>
     *
     * @return the location this entry was found in, as URI.
     */
    URI getURI();


    /**
     * check whether this entry contains a secret value.
     *
     * @return true if it is a secret, false otherwise.
     */
    default boolean isSecret() { return false; }

    /**
     * the
     *
     * @return the config entry specification, if given. if it isn't set, it may return null if enabled, or throw Exception.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if there was no specification
     */
    ConfigEntrySpecification getSpecification()
            throws ConfigCheckedException;

    /**
     * get the comment associated with this entry.
     * returning "null" instead of an empty string is intentional.
     * software using this needs to be caredfully processing the result anyhow,
     * since the comment-retrieval depends on heuristics.
     * <br/>
     * for comments to be read at all, set the ConfigFeature.COMMENTS_READING to true.
     * @return comment, or null if none.
     */
    default String getComment() { return null; }

    /**
     * set the comment for this entry.
     * Note: this does not automatically write the entry back to its source.
     * <br/>
     * for comments to be written at all, set the ConfigFeature.COMMENTS_WRITING to true.
     * @param comment the comment to be set
     */
    default void setComment(String comment) { }

    /**
     *  if isWritable - put a string.
     *
     * @param value the value to be written
     * @throws org.metabit.platform.support.config.ConfigCheckedException on severe errors
     */
    void putString(final String value) throws ConfigCheckedException;

    /**
     * if writeable, write a value of specified type
     *
     * @param value the value to be written
     * @param valueType the type what is to be written
     * @throws org.metabit.platform.support.config.ConfigCheckedException on failure
     */
    void putValue(final Object value, ConfigEntryType valueType)
            throws ConfigCheckedException;


    enum ConfigEntryFlags
    {
        /*
         * this entry must be present; if missing, entire config is considered invalid
         */
        MANDATORY,
        /*
         * can't write to
         */
        READ_ONLY,
        /*
         * can't read from.
         * use case e.g. write via mConfig, read from elsewhere
         */
        WRITE_ONLY,
        /*
         * security relevance.
         * private keys, symmetric keys, API keys, license IDs.
         */
        SECRET,
        /*
         * privacy relevance.
         * GDPR compliance, personal information
         */
        SENSITIVE

        // IMMUTABLE - @TODO CHECK
    }

}
//___EOF___
