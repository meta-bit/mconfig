package org.metabit.platform.support.config.source.envvar;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryFactory;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Environment Variable Config Storage.
 * Reads configuration from environment variables.
 * Default naming scheme: &lt;APPLICATION_NAME&gt;_&lt;CONFIG_NAME&gt;_&lt;KEY&gt;
 * Keys are converted to lower case.
 */
public class EnvVarConfigStorage implements ConfigStorageInterface
{
    private       ConfigFactoryInstanceContext context;
    private       ConfigLoggingInterface       logger;
    private final Map<String, String>          envOverride;

    public EnvVarConfigStorage()
        {
        this.envOverride = null;
        }

    /**
     * Internal constructor for testing.
     *
     * @param envOverride map to use instead of System.getenv()
     */
    EnvVarConfigStorage(Map<String, String> envOverride)
        {
        this.envOverride = envOverride;
        }

    private Map<String, String> getEnv()
        {
        return (envOverride != null) ? envOverride : System.getenv();
        }

    private String getEnv(String name)
        {
        return (envOverride != null) ? envOverride.get(name) : System.getenv(name);
        }

    @Override
    public String getStorageName()
        {
        return "Environment Variables";
        }

    @Override
    public String getStorageID()
        {
        return "envvar";
        }

    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        return true;
        }

    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.context = ctx;
        this.logger = ctx.getLogger();
        return true;
        }

    @Override
    public void exit()
        {
        // Nothing to clean up
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
        }

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        return URI.create("envvar:///"+key);
        }

    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        if (possibleSource.getScope() != ConfigScope.SESSION)
            {
            return;
            }

        EnvVarConfigLayer layer = new EnvVarConfigLayer(context, possibleSource, sanitizedConfigName, envOverride);
        if (!layer.isEmpty())
            {
            layeredCfg.add(layer, possibleSource);
            }
        }

    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        {
        return null; // Environment variables are typically read-only and discovered, not created
        }

    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        // Not supported for env vars
        }

    @Override
    public Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        return Collections.emptySet();
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        return false; // Environment variables don't change during process lifetime usually
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        // Not supported
        }

    private static class EnvVarConfigLayer implements ConfigLayerInterface
    {
        private final ConfigFactoryInstanceContext context;
        private final ConfigSource                 source;
        private final String                       prefix;
        private final Map<String, String>          envOverride;

        public EnvVarConfigLayer(ConfigFactoryInstanceContext context, ConfigLocation location, String configName, Map<String, String> envOverride)
            {
            this.context = context;
            this.envOverride = envOverride;
            this.source = new ConfigLocationImpl(location, this, null, null);
            String appName = context.getSettings().getString(ConfigFeature.APPLICATION_NAME);
            this.prefix = (appName+"_"+configName+"_").toUpperCase();
            }

        private Map<String, String> getEnv()
            {
            return (envOverride != null) ? envOverride : System.getenv();
            }

        private String getEnv(String name)
            {
            return (envOverride != null) ? envOverride.get(name) : System.getenv(name);
            }

        /**
         * @param hierarchicalKey key(s), with '/' as separator for key parts.
         *                        '/' is mapped to '_', and existing '_' to '__'.
         * @return
         */
        @Override
        public ConfigEntry getEntry(String hierarchicalKey)
            {
            String envVarName = (prefix + hierarchicalKey)
                    .replace("__", "_ESC_") // escape existing double underscores
                    .replace("/", "_")      // hierarchy slash converted to underscore
                    .replace("_ESC_", "__") // restore as double underscore
                    .toUpperCase();
            String value = getEnv(envVarName);
            if (value != null)
                {
                return ConfigEntryFactory.createEntry(hierarchicalKey, value, ConfigEntryType.STRING, null, source);
                }
            return null;
            }

        @Override
        public boolean isWriteable()
            {
            return false;
            }

        /**
         * check if this layer is empty.
         * case-insensitive search.
         *
         * @return true if no environment variable keys match the prefix; false otherwise
         */
        @Override
        public boolean isEmpty()
            {
            String searchPrefix = prefix;
            return getEnv().keySet().stream()
                    .noneMatch(k->k.regionMatches(true, 0, searchPrefix, 0, searchPrefix.length()));
            }

        @Override
        public ConfigScope getScope()
            {
            return source.getScope();
            }

        @Override
        public void writeEntry(ConfigEntry entryToWrite)
                throws ConfigCheckedException
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }

        @Override
        public int flush()
                throws ConfigCheckedException
            {
            return 0;
            }

        @Override
        public ConfigSource getSource()
            {
            return source;
            }

        @Override
        public int compareTo(ConfigLayerInterface o)
            {
            return this.getScope().compareTo(o.getScope());
            }

        @Override
        public Iterator<String> tryToGetKeyIterator()
            {
            return getEnv().keySet().stream()
                    .filter(k->k.startsWith(prefix))
                    .map(k->k.substring(prefix.length()).toLowerCase().replace('_', '/'))
                    .collect(Collectors.toList()).iterator();
            }
    }
}
