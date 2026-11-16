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
     * The value's bytes are decoded as UTF-8; malformed sequences become U+FFFD.
     *
     * @return an array of {@link char} objects
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    default char[]  getValueAsChars()       throws ConfigCheckedException
        {
        return org.metabit.platform.support.config.impl.util.ConfigIOUtil.utf8ToChars(getValueAsBytes());
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


    /**
     * declarative markers on a configuration entry.
     * <p>
     * Not every flag is acted upon yet; the state of each is noted below. A flag that is
     * not yet enforced is still parsed and retained, so schemas may declare it today,
     * but it has no effect on behaviour.
     *
     * @see org.metabit.platform.support.config.interfaces.ConfigEntrySpecification
     */
    enum ConfigEntryFlags
    {
        /**
         * this entry must be present; if missing, the entire config is considered invalid.
         * <p>
         * Enforced. Also determines the default arity of the entry.
         */
        MANDATORY,
        /**
         * can't write to.
         * <p>
         * Distinct from the writability of a storage location: this marks a single entry as
         * non-writable even where its layer would accept writes.
         * <p>
         * Not enforced yet.
         */
        READ_ONLY,
        /**
         * can't read from.
         * use case e.g. write via mConfig, read from elsewhere.
         * <p>
         * Not enforced yet.
         */
        WRITE_ONLY,
        /**
         * security relevance: cryptographic secrets and credentials.
         * private keys, symmetric keys, API keys, passwords, license IDs.
         * <p>
         * A secret entry is represented differently - it is kept out of the JVM String pool
         * where possible, redacted in logs, retrieved through
         * {@link org.metabit.platform.support.config.Configuration#getSecret(String)}, and
         * wiped on erase. Secrecy may also originate from the configuration source itself
         * (Vault, AWS Secrets Manager, Kubernetes), with no schema involved.
         * <p>
         * Most SECRET entries are also {@link #SENSITIVE}, though rarely the reverse.
         * <p>
         * <b>Not enforced through this flag.</b> Secret handling is currently driven solely by
         * the schema property {@code "SECRET": true} and by {@code ConfigEntry.isSecret()}.
         * Declaring the flag alone leaves the value unprotected.
         */
        SECRET,
        /**
         * privacy relevance: information that must not become visible where it does not belong.
         * GDPR compliance, personal information.
         * <p>
         * Unlike {@link #SECRET}, this does not change how the value is represented - the
         * application reads it with the ordinary typed getters. It restricts where the value
         * may be disclosed: logs, exports, diagnostic output, generated documentation, and
         * which storage places are considered acceptable for it.
         * <p>
         * What is restricted is the disclosure of the <em>value</em>. The storage location
         * stays traceable: {@link ConfigEntry#getScope()}, {@link ConfigEntry#getLocation()}
         * and {@link ConfigEntry#getURI()} keep working, so the data remains findable,
         * correctable and erasable by whoever it belongs to.
         * <p>
         * Not enforced yet.
         */
        SENSITIVE

        // IMMUTABLE - @TODO CHECK
    }

}
//___EOF___
