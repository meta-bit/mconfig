/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.config;

/**
 * unchecked Exception indicating severe issues mConfig could not recover from.
 * <br/>
 * After lengthy consideration, this is an unchecked runtime exception.
 * There is a checked one for library-internal use ; the API-facing one is unchecked.
 * <br/>
 * Quote from
 *
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html"> Java Documentation on the Unchecked Exceptions controversy </a>
 * <q>If a client can reasonably be expected to recover from an exception,
 *    make it a checked exception.<br/>
 *    If a client cannot do anything to recover from the exception,
 *    make it an unchecked exception.</q>
 * Configurations provide your code details and settings it needs to run with.
 * If it doesn't get these, how can it run?
 *  <br/>
 *  mConfig has a number of ways to fall back, use defaults, and so on.
 *  If it throws its Exception, none of these worked; and your code won't get
 *  (all of) its config values.
 *  <br/>
 *
 *  This is not to be thrown if the configuration inputs (files etc)
 *  are not found or trouble - unless explicitly asked so by a setting.
 *  <br/>
 *  Even I/O errors will be kept quiet and in the warning/error log, if we can
 *  provide the code with its configuration in the end.
 *  <br/>
 *  This will be thrown on code errors, e.g. imvalid config Schemes, invalid
 *  call parameters,
 * 
 * @version $Id: $Id
 */
public class ConfigException extends RuntimeException
{
    private final ConfigExceptionReason reason;

    //used e.g. for invalid feature set calls
    /**
     * <p>Constructor for ConfigException.</p>
     *
     * @param s a {@link java.lang.String} object
     */
    public ConfigException(String s)
        {
        super(s);
        reason = ConfigExceptionReason.REASON_IN_STRING;
        }

    /**
     * <p>Constructor for ConfigException.</p>
     *
     * @param e a {@link java.lang.Exception} object
     */
    public ConfigException(Exception e)
        {
        super(e);
        reason = ConfigExceptionReason.REASON_IN_THROWABLE;
        }

    /**
     * <p>Constructor for ConfigException.</p>
     *
     * @param reason a {@link org.metabit.platform.support.config.ConfigException.ConfigExceptionReason} object
     * @param e a {@link java.lang.Error} object
     */
    public ConfigException(ConfigExceptionReason reason, Error e)
        {
        super(e);
        this.reason = reason;
        }

    /**
     * <p>Constructor for ConfigException.</p>
     *
     * @param err a {@link java.lang.Error} object
     */
    public ConfigException(Error err)
        {
        super(err);
        reason = ConfigExceptionReason.REASON_IN_THROWABLE;
        }

    /**
     * <p>Constructor for ConfigException.</p>
     *
     * @param reason a {@link org.metabit.platform.support.config.ConfigException.ConfigExceptionReason} object
     */
    public ConfigException(ConfigExceptionReason reason)
        {
        super(reason.name());
        this.reason = reason;
        }

    /**
     * <p>Constructor for ConfigException.</p>
     *
     * @param reason a {@link org.metabit.platform.support.config.ConfigException.ConfigExceptionReason} object
     * @param detail detail message
     */
    public ConfigException(ConfigExceptionReason reason, String detail)
        {
        super(reason.name() + ": " + detail);
        this.reason = reason;
        }

    /**
     * <p>Getter for the field <code>reason</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigException.ConfigExceptionReason} object
     */
    public ConfigExceptionReason getReason() { return reason; }

    @Override
    public String toString()
        {
        return "ConfigException{" +
                "reason=" + reason +
                ", message='" + getMessage() + '\'' +
                '}';
        }

    public enum ConfigExceptionReason
    {
        FACTORY_INIT_FAILED,
        /**
         * this reason is given if Java threw a ServiceConfigurationError.
         */
        JAVA_SERVICE_CONFIGURATION_ERROR,
        KEY_FORMAT_INVALID,
        /**
         * when in a config Scheme the default value does not match the validator.
         */
        DEFAULT_VALIDATOR_MISMATCH,
        /**
         * when you've set a ConfigFeature to the ConfigFactory which turns out to be invalid.
         */
        CONFIG_FEATURE_VALUE_INVALID,
        REASON_IN_STRING,
        REASON_IN_THROWABLE,
        NO_MATCHING_ENTRY,
        NO_CONFIGURATION_FOUND,
        NOT_WRITEABLE,
        ARGUMENT_INVALID,
        ARGUMENT_REFUSED,

        /**
         * when writing, could not find any writeable existing location within
         * the specified scope(s). If automatic generation was enabled,
         * this too failed. effectively: could not perform the write.
         */
        NO_WRITEABLE_LOCATION,
        /**
         * there is something inconsistent in the code. a state was reached
         * that never should have, some checks failed, this sort of thing.
         * Not your fault as a user.
         */
        CODE_LOGIC_ERROR,
        /**
         * automatic type conversion failed.
         */
        CONVERSION_FAILURE,
        /**
         * when a configuration scheme contains features that are marked as mandatory
         * but are not known to this version of the library.
         */
        UNKNOWN_MANDATORY_FEATURE
    }
}
