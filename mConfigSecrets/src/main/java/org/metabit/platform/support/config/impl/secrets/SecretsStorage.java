package org.metabit.platform.support.config.impl.secrets;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.net.URI;
import java.util.Map;

public class SecretsStorage implements ConfigStorageInterface
{
    private ConfigLoggingInterface logger;
    private final String providerID;
    private final Map<String, Object> providerConfig;
    private ConfigScope scope = ConfigScope.APPLICATION;

    public SecretsStorage()
        {
        this.providerID = null;
        this.providerConfig = null;
        }

    public SecretsStorage(String providerID, Map<String, Object> providerConfig)
        {
        this.providerID = providerID;
        this.providerConfig = providerConfig;
        }

    public SecretsStorage(String providerID, Map<String, Object> providerConfig, ConfigScope scope)
        {
        this.providerID = providerID;
        this.providerConfig = providerConfig;
        this.scope = scope;
        }

    @Override
    public String getStorageName()
        {
        return "SecretsStorage" + (providerID != null ? " [" + providerID + "]" : "");
        }

    @Override
    public String getStorageID()
        {
        return "secrets" + (providerID != null ? "." + providerID : "");
        }

    public String getProviderID()
        {
        return providerID;
        }

    public Map<String, Object> getProviderConfig()
        {
        return providerConfig;
        }

    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        return true;
        }

    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.logger = ctx.getLogger();
        return true;
        }

    @Override
    public void exit()
        {
        }

    @Override
    public ConfigStorageInterface clone() throws CloneNotSupportedException
        {
        return (ConfigStorageInterface) super.clone();
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return true;
        }

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        return URI.create("mconfig:secrets/" + key);
        }

    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        }

    @Override
    public void provideAdditionalLayers(String sanitizedConfigName, LayeredConfigurationInterface layeredCfg)
        {
        ConfigFactoryInstanceContext ctx = layeredCfg.getContext();
        ConfigSecretsProviderInterface provider = ctx.getConfigSecretsProviders().get(providerID);
        if (provider != null)
            {
            layeredCfg.add(new SecretsConfigLayer(provider, scope), new ConfigLocationImpl(scope, this, null, null));
            }
        }

    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        {
        return null;
        }

    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        }

    @Override
    public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        return java.util.Collections.emptySet();
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        return false;
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        }
}
