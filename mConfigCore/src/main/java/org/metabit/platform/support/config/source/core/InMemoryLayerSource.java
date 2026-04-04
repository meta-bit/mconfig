package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>InMemoryLayerSource class.</p>
 * Base for Runtime and Session InMemoryLayerSources.
 *
 * @version $Id: $Id
 */
public class InMemoryLayerSource implements ConfigStorageInterface
{
    private       ConfigLoggingInterface            logger;
    private       ConfigFactoryInstanceContext      settings;
    private       ConfigLocation                    ourLocation;
    private final Map<String, ConfigLayerInterface> configInstances;
    private       boolean                           hasChanged;
    private final ConfigScope                       fixedScope;
    final static  String                            DEFAULTCHARSET = "UTF-8";

    /** {@inheritDoc} */
    @Override
    public String getStorageName()
        {
        return "memory-"+fixedScope.name().toLowerCase();
        }

    /** {@inheritDoc} */
    @Override
    public String getStorageID()
        {
        return "RAM-"+fixedScope.name();
        }

    /** {@inheritDoc} */
    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        return true;
        }


    public InMemoryLayerSource()
        {
        this(ConfigScope.RUNTIME);
        }

    public InMemoryLayerSource(ConfigScope scope)
        {
        this.configInstances = new HashMap<>();
        this.fixedScope = scope;
        this.hasChanged = false;
        }

    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.settings = ctx;
        this.logger = ctx.getLogger();
        //@TODO if we make a separate convert-from-and-to-string format, we might use it here.
        // or the JAR handle instead of null

        ourLocation = new ConfigLocationImpl(fixedScope, this, null, fixedScope);
        ctx.getSearchList().insertAtScopeStart(ourLocation, fixedScope);

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
    public void updateConfigurationLayers(final String sanitizedConfigName, final ConfigLocation possibleSource, LayeredConfigurationInterface cfgCollector)
        {
        logger.debug("trying to create config \""+sanitizedConfigName+"\" from memory storage at scope "+possibleSource.getScope());
        ConfigLayerInterface cfgInstance = this.configInstances.get(sanitizedConfigName);
        if (cfgInstance == null)
            {
            cfgInstance = new InMemoryLayer(settings, possibleSource, possibleSource.getScope());
            this.configInstances.put(sanitizedConfigName, cfgInstance);
            }
        // else
        logger.debug("memory config "+sanitizedConfigName+" found for scope "+possibleSource.getScope());
        cfgCollector.add(cfgInstance, possibleSource); //@CHECK
        return;
        }

    /** {@inheritDoc} */
    @Override
    public ConfigLayerInterface createConfigurationLayer(String sanitizedConfigName, ConfigLocation location, ConfigSchema configScheme, LayeredConfiguration layeredConfiguration)
        {
        logger.debug("trying to create config \""+sanitizedConfigName+"\" inside memory storage for scope "+location.getScope());
        InMemoryLayer instance = new InMemoryLayer(settings, location, location.getScope());
        this.configInstances.put(sanitizedConfigName, instance);
        return instance;
        }

    /**
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param location            location to look in
     * @param blobConfig          the blob config we're working on
     */
    @Override
    public void updateBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
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
        boolean tmp = hasChanged;
        hasChanged = false;
        return tmp;
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        hasChanged = true;
        }

    @Override
    public boolean isPreferredTestLocation(ConfigLocation location)
        {
        // In-memory locations are always preferred in test mode because they are isolated and safe.
        ConfigFactorySettings settings = this.settings.getSettings();
        if (settings.getBoolean(org.metabit.platform.support.config.ConfigFeature.TEST_MODE))
            {
            return true;
            }
        return false;
        }

    @Override
    public String toString()
        {
        return "In-Memory Storage";
        }
}
//___EOF___
