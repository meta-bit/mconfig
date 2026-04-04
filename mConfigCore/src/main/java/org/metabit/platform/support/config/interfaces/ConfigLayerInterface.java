package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.ConfigSource;

import java.util.Iterator;

/**
 * a single configuration file, registry tree entry, or such.
 * if there are multiple layers, they are combined or searched in order.
 * <p>
 * This is a hierarchical construct: a tree of keys,
 * with keys for branches, and with key-value(-metadata) leaves.
 * <p>
 * simplified access allows a combined String where "/" are separators for keys.
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigLayerInterface extends Comparable<ConfigLayerInterface>
{
    /**
     * <p>getEntry.</p>
     *
     * @param hierarchicalKey key(s), with '/' as separator for key parts.
     * @return the matching ConfigEntry instance, or null if no match was found.
     */
    ConfigEntry getEntry(final String hierarchicalKey);

    /**
     * get an entry with a specific specification.
     * @param hierarchicalKey key(s), with '/' as separator for key parts.
     * @param specification the specification for this entry.
     * @return the matching ConfigEntry instance, or null if no match was found.
     */
    default ConfigEntry getEntry(String hierarchicalKey, ConfigEntrySpecification specification)
        {
        ConfigEntry entry = getEntry(hierarchicalKey);
        if (entry != null && specification != null)
            {
            // @TODO: wrap or update entry with specification if needed.
            }
        return entry;
        }
    // void        putEntry/createEntry.

    /**
     * <p>isWriteable.</p>
     *
     * @return a boolean
     */
    boolean isWriteable();

    /**
     * check for emptiness.
     *
     * @return true if there is not a single entry with value; false otherwise
     */
    boolean isEmpty();

    /**
     * <p>getScope.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigScope} object
     */
    ConfigScope getScope();

    /**
     * <p>writeEntry.</p>
     *
     * @param entryToWrite a {@link org.metabit.platform.support.config.ConfigEntry} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    void writeEntry(ConfigEntry entryToWrite)
            throws ConfigCheckedException;

    /**
     * flush write-cache, if applicable.
     *
     * @return 0 for none. &gt;0 number of entries flushed; &lt;0 for status codes.
     * @throws org.metabit.platform.support.config.ConfigCheckedException on extreme IO errors
     */
    int flush()
            throws ConfigCheckedException;

    /**
     * <p>getSource.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigSource} object
     */
    ConfigSource getSource();

    /** {@inheritDoc} */
    @Override
    int compareTo(ConfigLayerInterface o);

    // return null on refusal/being impossible.
    /**
     * <p>tryToGetKeyIterator.</p>
     *
     * @return a {@link java.util.Iterator} object
     */
    Iterator<String> tryToGetKeyIterator();

    // boolean hasChangedSincePreviousCheck();
}
