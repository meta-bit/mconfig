package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;

import java.util.Locale;

/**
 * for a single configuration entry,
 * information about how it:
 * -- the key
 * -- the expected type
 * -- the default value
 * -- optional: limitations for the value
 * -- optional: a description, reference, or other human-usable information about the field
 * -- flags, like "mandatory".
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigEntrySpecification
{
    /**
     * <p>getKey.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getKey();

    /**
     * <p>getType.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigEntryType} object
     */
    ConfigEntryType getType();
//
// value limitation patterns... tricky. there's a String representation to which, and from which convert.

    /**
     * limitations to the permissible values. optional.
     * <p>
     * The format depends on the ConfigEntryValueType.
     * String: a (Java) RegExp.
     * Integer: a range specification, which can be included in the generated documentation
     *
     * @return string representation of the limitation pattern.
     */
    String getValueLimitations();

    /**
     * <p>getDefaultEntry. default entry (instead of default type)</p>
     *
     * @return a {@link ConfigEntry} object
     */
    ConfigEntry getDefaultEntry();

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getDescription();

    /**
     * <p>getDescription.</p>
     *
     * @param locale the locale for which to get the description.
     * @return a {@link java.lang.String} object
     */
    default String getDescription(Locale locale)
        {
        return getDescription();
        }

    /**
     * <p>isMandatory.</p>
     *
     * @return a boolean
     */
    boolean isMandatory();

    /**
     * <p>isSecret.</p>
     *
     * @return a boolean
     */
    boolean isSecret();

    /**
     * validate an entry against this specification.
     * @param entry the entry to validate.
     * @return true if valid, false otherwise.
     */
    boolean validateEntry(ConfigEntry entry);

    /**
     * Checks whether the configuration entry is marked as hidden from
     * automatic documentation (e.g. commandline --help features)
     *
     * @return true if the configuration entry is hidden, false otherwise.
     */
    boolean isHidden();

    /**
     * get the minimum number of occurrences for this entry.
     * Default is 1 for mandatory entries, 0 for optional ones.
     *
     * @return the minimum arity.
     */
    default int getMinArity()
        {
        return isMandatory() ? 1 : 0;
        }

    /**
     * get the maximum number of occurrences for this entry.
     * Default is 1 for non-list types, -1 (unlimited) for list types.
     *
     * @return the maximum arity, or -1 for unlimited.
     */
    default int getMaxArity()
        {
        ConfigEntryType type = getType();
        if (type == ConfigEntryType.MULTIPLE_STRINGS || type == ConfigEntryType.ENUM_SET)
            {
            return -1;
            }
        return 1;
        }

    /**
     * check if this specification defines a list-like entry.
     *
     * @return true if it is a list, false otherwise.
     */
    default boolean isList()
        {
        int max = getMaxArity();
        return max == -1 || max > 1;
        }

    /**
     * Check if this specification contains unknown features that are marked as mandatory.
     * If true, the system should treat this specification as invalid or unusable.
     *
     * @return true if there are unknown mandatory features, false otherwise.
     */
    default boolean hasUnknownMandatoryFeatures()
        {
        return false;
        }
}
