package org.metabit.platform.support.config.interfaces;


import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.schema.ConfigSchema;
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
    void setConfigSchema(final ConfigSchema scheme);

    /**
     * add a config layer to this collection.
     *
     * @param singleConfig a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     */
    void add(ConfigLayerInterface singleConfig, ConfigLocation location);

    /** {@inheritDoc} */
    List<ConfigLocation> getSourceLocations();

    /**
     * get the context for this configuration.
     * @return the context
     */
    ConfigFactoryInstanceContext getContext();

    /**
     * the schema of this configuration.
     * @return the schema; a null schema if there is none
     */
    ConfigSchema getConfigSchema();

    /**
     * whether this is a bootstrap lookup of a module (see {@code BootstrapConfigContext.getRestrictedConfig()}).
     * Bootstrap configurations of the cloud modules typically hold credentials without declaring them in a schema.
     * @return true for a bootstrap lookup
     */
    boolean isBootstrapLookup();
}
//___EOF___
