package org.metabit.platform.support.config;

import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.net.URI;
import java.nio.file.Path;

/**
 * <p>ConfigLocation interface</p>
 * Where is a configuration located? What do we know about that location?
 * NB: ConfigLocation is supposed to be a leaf, not a branch.
 * related class: ConfigSource is a ConfigLocation that has been instantiated,
 * so there is a layer to it.
 *
 * @version $Id: $Id
 */
public interface ConfigLocation
{
    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String toString();
    /**
     * <p>toLocationString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String toLocationString();
    /**
     * <p>getURI.</p>
     *
     * @param key a {@link java.lang.String} object
     * @param optionalFragment a {@link java.lang.String} object
     * @return a {@link java.net.URI} object
     */
    URI getURI(final String key, final String optionalFragment);

    /**
     * <p>getScope.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigScope} object
     */
    ConfigScope getScope();
    /**
     * <p>isWriteable.</p>
     *
     * @return a boolean
     */
    boolean isWriteable();

    //---- "private" internal interface ----
    // @TODO we may want to split this later back into two classes,
    // making the "internal" interface a ConfigSource again.
    // (internal extends external one, is not exported, different package.
    /**
     * get Storage this location is associated with (accessed through which)
     *
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigStorageInterface} object
     */
    ConfigStorageInterface getStorage();
    /**
     * internal handle for use within the specific ConfigStorage.
     * <em>private</em> method
     * @return a {@link java.lang.Object} object; each ConfigStorage knows its own type for this.
     */
    Object getStorageInstanceHandle(); // specific to the storage

    /* tried to put the ConfigLayerInterface instance handle here; as of now, it is part of the ConfigEntry itself.
    //
       the issue is this:
       the *location*, as in ConfigLocation, is where we will go looking.
       some bits of it are uncertain.
       once we actually go looking and instantiate things, *then* we get the actual layer instance, and know
       which format is used.
     */

    /**
     * derive a new ConfigLocation given to the current one.
     *
     * @param file file Path to apply relative to
     * @return derived ConfigLocation, or null if not applicable/possible
     */
    ConfigLocation derive(final Path file);

    /**
     * derive a new ConfigLocation given to the current one.
     *
     * @param uri URI or URL to apply in relative
     * @return derived ConfigLocation, or null if not applicable/possible
     *         -- @CHECK usage in the long run, @TODO remove if not needed+
     */
    @Deprecated
    ConfigLocation derive(final URI uri);
}
//___EOF___