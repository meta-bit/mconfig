package org.metabit.platform.support.config.interfaces;


import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import java.util.List;

/**
 * <p>LayeredConfigurationInterface interface.</p>
 *
 * 
 * @version $Id: $Id
 */
public interface LayeredConfigurationInterface extends BasicConfiguration
{
    /** {@inheritDoc} */
    void setConfigScheme(final ConfigScheme scheme);

    /**
     * add a config layer to this collection.
     *
     * @param singleConfig a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     */
    void add(ConfigLayerInterface singleConfig, ConfigLocation location);

//    ConfigEntry getConfigEntryForWriting(ConfigScope scope, String fullKey)     throws ConfigCheckedException;

    /** {@inheritDoc} */
    List<ConfigLocation> getSourceLocations();

    /**
     * get the context for this configuration.
     * @return the context
     */
    ConfigFactoryInstanceContext getContext();
}
