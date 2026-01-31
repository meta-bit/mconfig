/*
 * Copyright (c) metabit 2018-2026. placed under CC-BY-ND-4.0 license.
 * Full license text available at https://tldrlegal.com/license/creative-commons-attribution-noderivatives-4.0-international-(cc-by-nd-4.0)#fulltext
 * You may: distribute, use for commercial and non-commercial purposes
 * You must: give credit, include/keep copyright, state any changes
 * You mustn't: distribute modified versions, sublicense
 */

package org.metabit.platform.support.config;

/**
 * Checked configuration class for mConfig.
 * Used internally, mostly; with few exceptions:
 * - config writing. If this causes I/O errors, they are forwarded within this
 * checked exception, if respective functionality is enabled.
 * <br></br>
 * Rule of thumb: if calling code messes up, that's a runtime exception,
 * and the unchecked @see org.metabit.platform.support.config.ConfigException
 * will be used.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigCheckedException extends Exception
{
private final ConfigExceptionReason reason;

/*
public ConfigCheckedException(String s)
    {
    super(s);
    reason = ConfigExceptionReason.REASON_IN_STRING;
    }
*/

    /**
     * <p>Constructor for ConfigCheckedException.</p>
     *
     * @param e a {@link java.lang.Exception} object
     */
    public ConfigCheckedException(Exception e)
    {
    super(e);
    reason = ConfigExceptionReason.REASON_IN_THROWABLE;
    }

    /**
     * <p>Constructor for ConfigCheckedException.</p>
     *
     * @param err a {@link java.lang.Error} object
     */
    public ConfigCheckedException(Error err)
    {
    super(err);
    reason = ConfigExceptionReason.REASON_IN_THROWABLE;
    }

    /**
     * <p>Constructor for ConfigCheckedException.</p>
     *
     * @param reason a {@link org.metabit.platform.support.config.ConfigCheckedException.ConfigExceptionReason} object
     */
    public ConfigCheckedException(ConfigExceptionReason reason)
    {
    super(reason.name());
    this.reason = reason;
    }

    /**
     * <p>Getter for the field <code>reason</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigCheckedException.ConfigExceptionReason} object
     */
    public ConfigExceptionReason getReason() { return reason; }

    /**
     * enum to tell exception reasons / causes apart.
     */
    public enum ConfigExceptionReason
    {
        REASON_IN_STRING,
        REASON_IN_THROWABLE,
        INPUT_INVALID,
        KEY_FORMAT_INVALID,
        OBJECT_CLASS_MISMATCHING_WITH_TYPE,
        NOT_WRITEABLE,
        INVALID_USE,
        NO_MATCHING_DATA,
        /**
         * for use when writing, could not find any writeable existing location within
         * the specified scope(s). If automatic generation was enabled,
         * this too failed. effectively: could not perform the write.
         */
        NO_WRITEABLE_LOCATION,
        /**
         * automatic type conversion failed.
         */
        CONVERSION_FAILURE
    }
}
//___EOF___
