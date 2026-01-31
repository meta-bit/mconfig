package org.metabit.platform.support.config;

import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;

/**
 * <p>ConfigSource interface.</p>
 * Something you can potentially get Configurations from.
 *
 * ConfigSource is a ConfigLocation that has been instantiated, so there is a layer to it.
 * 
 * @version $Id: $Id
 */
public interface ConfigSource extends ConfigLocation
{
    /**
     * <p>getStorageFormat.</p>
     *
     * @return the format instance for this ConfigSource
     */
    ConfigFormatInterface getStorageFormat(); // usually specific to the storage, too.
    /**
     * <p>getLayer.</p>
     *
     * @return layerThisIsStoredIn layer this source is in.
     */
    ConfigLayerInterface getLayer(); // if layered

    /**
     * <p>hasChangedSincePreviousCheck.</p>
     *
     * @return a boolean
     */
    boolean hasChangedSincePreviousCheck();
}
