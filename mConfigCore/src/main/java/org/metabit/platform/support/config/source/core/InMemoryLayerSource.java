package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>InMemoryLayerSource class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class InMemoryLayerSource implements ConfigStorageInterface
{
    private ConfigLoggingInterface            logger;
    private ConfigFactoryInstanceContext      settings;
    private ConfigLocation                    ourLocation;
    // private ConfigLocationImpl                ourSecondLocation;
    private Map<String, ConfigLayerInterface> configInstances;
    private boolean changedFlag;
    final static String DEFAULTCHARSET = "UTF-8";

    /** {@inheritDoc} */
    @Override
    public String getStorageName()
        {
        return "memory";
        }

    /** {@inheritDoc} */
    @Override
    public String getStorageID()
        {
        return "RAM";
        }

    /** {@inheritDoc} */
    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        return true;
        }


    // hard-code to SESSION scope
    /** {@inheritDoc} */
    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.settings = ctx;
        this.logger = ctx.getLogger();
        this.configInstances = new HashMap<>();
        this.changedFlag = false;
        //@TODO if we make a separate convert-from-and-to-string format, we might use it here.
        // or the JAR handle instead of null
        ourLocation = new ConfigLocationImpl(ConfigScope.SESSION, this, null, this);
        ctx.getSearchList().insertAtScopeStart(ourLocation, ConfigScope.SESSION);
        // ourSecondLocation = new ConfigLocationImpl(ConfigScope.RUNTIME, this, null, this);
        // ctx.getSearchList().insertAtScopeEnd(ourLocation, ConfigScope.RUNTIME);
        return true;
        }

    /** {@inheritDoc} */
    @Override
    public void exit()
        { }

    /** {@inheritDoc} */
    @Override
    public boolean isGenerallyWriteable()
        {
        return true;
        }


    /** {@inheritDoc} */
    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        if (configLocation.getStorage() != this)
            throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
        try
            {
            // NB this allows empty keys for safety and debug only
            String keyString = (key == null) ? "" : URLEncoder.encode(key, DEFAULTCHARSET);
            String locationString = URLEncoder.encode(configLocation.toLocationString(), DEFAULTCHARSET);
            String uristring = String.format("mconfig:memory/%s/%s", locationString, keyString);
            if (optionalFragment != null)
                uristring += "#"+URLEncoder.encode(optionalFragment, DEFAULTCHARSET);
            return URI.create(uristring);
            }
        catch (UnsupportedEncodingException e)
            {
            throw new ConfigException(e);
            } // meaning DEFAULTCHARSET would be wrong.
        }

    /** {@inheritDoc} */
    @Override
    public void tryToReadConfigurationLayers(final String sanitizedConfigName, final ConfigLocation possibleSource, LayeredConfigurationInterface cfgCollector)
        {
        logger.debug("trying to create config \""+sanitizedConfigName+"\" from memory storage");
        ConfigLayerInterface cfgInstance = this.configInstances.get(sanitizedConfigName);
        if (cfgInstance == null)
            {
            logger.debug("memory config "+sanitizedConfigName+" not found");
            return;
            }
        // else
        logger.debug("memory config "+sanitizedConfigName+" found");
        cfgCollector.add(cfgInstance, possibleSource); //@CHECK
        return;
        }

    /** {@inheritDoc} */
    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String sanitizedConfigName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        {
        logger.debug("trying to create config \""+sanitizedConfigName+"\" inside memory storage");
        InMemoryLayer instance = new InMemoryLayer(settings, ourLocation, ConfigScope.SESSION);
        this.configInstances.put(sanitizedConfigName, instance);
        return instance;
        }

    /**
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param location            location to look in
     * @param blobConfig          the blob config we're working on
     */
    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        // ignored. blob storage in memory is possible, but what's the use?
        // we can't override individual entries this way, just complete blobs
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
        boolean tmp = changedFlag;
        changedFlag = false;
        return tmp;
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        changedFlag = true;
        }

    @Override
    public String toString()
        {
        return "In-Memory Storage";
        }
}
//___EOF___
