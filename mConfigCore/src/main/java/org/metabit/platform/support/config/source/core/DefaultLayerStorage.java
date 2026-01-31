package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>DefaultLayerStorage class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class DefaultLayerStorage implements ConfigStorageInterface
{
    private ConfigLoggingInterface logger;

    /** {@inheritDoc} */
    @Override
    public String getStorageName()
        {
        return "DefaultLayerStorage";
        }

    /** {@inheritDoc} */
    @Override
    public String getStorageID()
        {
        return "defaults";
        }

    /** {@inheritDoc} */
    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        return true; // everything is OK for the default layer storage.
        }

    /** {@inheritDoc} */
    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.logger = ctx.getLogger();
        return true;
        }

    /** {@inheritDoc} */
    @Override
    public void exit()
        { }

    /** {@inheritDoc} */
    @Override
    public ConfigStorageInterface clone()
            throws CloneNotSupportedException
        {
        super.clone();
        return null;
        }

    /** {@inheritDoc} */
    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
        }


    /** {@inheritDoc} */
    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        if (configLocation.getStorage() != this)
            throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
        String keyString = (key == null) ? "" : URLEncoder.encode(key, StandardCharsets.UTF_8);
        String locationString = URLEncoder.encode(configLocation.toLocationString(), StandardCharsets.UTF_8);
        String uristring = String.format("mconfig:runtimedefaults/%s/%s", locationString, keyString); //@CHECK
        if (optionalFragment != null)
            uristring += "#"+URLEncoder.encode(optionalFragment, StandardCharsets.UTF_8);
        return URI.create(uristring);
        }

    /** {@inheritDoc} */
    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        logger.debug("default layer is not read");
        }

    /** {@inheritDoc} */
    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        {
        logger.debug("trying to create inside default storage");
        return null;
        }

    /**
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param location            location to look in
     * @param blobConfig          the blob config we're working on
     */
    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        // no default BLOB.
        return;
        }

    /** {@inheritDoc} */
    @Override
    public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        return java.util.Collections.emptySet();
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        return false; // defaults are not supposed to change at runtime
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        }
}
//___EOF___
