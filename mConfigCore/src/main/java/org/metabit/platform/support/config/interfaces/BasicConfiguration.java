package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.ConfigEntry;

import java.util.EnumSet;
import java.util.Iterator;

/**
 * the actual thing.
 * handles ConfigEntry objects.
 * For the abstraction to specific java types, see Configuration.
 *
 * 
 * @version $Id: $Id
 */
public interface BasicConfiguration extends AutoCloseable
{
    String getConfigName();
    /**
     * get a config entry.
     *
     * @param fullKey full key to use (ASCII string, for hierarchy, separator / )
     * @param scopes the scopes to search in.
     * @return the ConfigEntry, or null
     */
    ConfigEntry getConfigEntryFromFullKey(final String fullKey, EnumSet<ConfigScope> scopes);

    // ConfigEntry getConfigEntryForWriting(final ConfigScope scope, final String fullKey);


    /**
     * set the config scheme to use and validate against.
     *
     * @param scheme ConfigScheme to use.
     */
    void setConfigScheme(final ConfigScheme scheme);
    /**
     * check whether this configuration is writable
     *
     * @return true if writable, false if read-only
     */
    boolean isWriteable(); // or "changeable"... mutable... but "writable"/"writeable" is the term most people are familiar with.

    /**
     * flush all write caches, if write-buffering is activated
     *
     * @return number of flushed entries
     * @throws org.metabit.platform.support.config.ConfigCheckedException on severe errors.
     */
    int flush()
            throws ConfigCheckedException;

    /**
     * flush all read caches, so subsequent reads get the most recent state of
     * configuration source contents
     *
     * @return true if there was anything to be re-read, false otherwise.
     * @throws org.metabit.platform.support.config.ConfigCheckedException on severe errors.
     */
    boolean reload()
            throws ConfigCheckedException;

    /**
     * check for empty configurations
     *
     * @return true if there is not a single entry with value in the configuration; false otherwise.
     */
    boolean isEmpty();

    /**
     * iterator to get all valid keys. Will do a tree walk over all
     * config sources which permit iteration; all content from sources
     * which do not will be invisible for this, but still effective.
     *
     * @return String-Iterator over all iterable(!) keys.
     * feature request MLIB-64
     */
    Iterator<String> getEntryKeyTreeIterator();
    boolean isClosed();
}
