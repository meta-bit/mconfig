package org.metabit.platform.support.config.source.envvar;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryFactory;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.schema.ConfigSchema;

import org.metabit.platform.support.config.impl.logging.LogSafe;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Environment Variable Config Storage.
 * Reads configuration from environment variables.
 * Default naming scheme: &lt;APPLICATION_NAME&gt;_&lt;CONFIG_NAME&gt;_&lt;KEY&gt;, in UPPER CASE.
 * Within the key, the separator '/' is written as '_', and a literal '_' as '__'.
 * Enumerated keys are in lower case, since the variable name does not keep the case.
 */
public class EnvVarConfigStorage implements ConfigStorageInterface
{
    private       ConfigFactoryInstanceContext context;
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
    public void updateConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
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
    public ConfigLayerInterface createConfigurationLayer(String configName, ConfigLocation location, ConfigSchema configScheme, LayeredConfiguration layeredConfiguration)
        {
        return null; // Environment variables are typically read-only and discovered, not created
        }

    @Override
    public void updateBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
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
        /** Windows treats environment variable names as case-insensitive; everything else does not. */
        private static final boolean CASE_INSENSITIVE_NAMES = (File.separatorChar == '\\');

        private final ConfigSource                 source;
        private final String                       prefix;
        private final Map<String, String>          envOverride;
        private final ConfigLoggingInterface       logger;
        // transition 0.8.17 -> 0.9: variables already warned about. Remove with warnAboutLegacyName().
        private final Set<String>                  warnedLegacyNames = ConcurrentHashMap.newKeySet();

        public EnvVarConfigLayer(ConfigFactoryInstanceContext context, ConfigLocation location, String configName, Map<String, String> envOverride)
            {
            this.envOverride = envOverride;
            this.logger = context.getLogger();
            this.source = new ConfigLocationImpl(location, this, null, null);
            String appName = context.getSettings().getString(ConfigFeature.APPLICATION_NAME);
            // the prefix is not escaped; naming rules for application and configuration names: MLIB-143
            this.prefix = (appName+"_"+configName+"_").toUpperCase(Locale.ROOT);
            }

        /**
         * Map a hierarchical key to the part of the variable name after the prefix:
         * a literal '_' is doubled to '__', the separator '/' becomes '_', then UPPER CASE.
         */
        static String encodeKey(String hierarchicalKey)
            {
            return hierarchicalKey
                    .replace("_", "__")
                    .replace('/', '_')
                    .toUpperCase(Locale.ROOT);
            }

        /**
         * Reverse of {@link #encodeKey(String)}, read left to right: '__' is a literal '_',
         * a single '_' is the separator '/'. Lower case, since the variable name does not keep the case.
         * Three or more '_' in a row are ambiguous ("a_/b" and "a/_b" share a name); they decode as "a_/b".
         */
        static String decodeKey(String encodedKey)
            {
            StringBuilder sb = new StringBuilder(encodedKey.length());
            int length = encodedKey.length();
            for (int i = 0; i < length; i++)
                {
                char c = encodedKey.charAt(i);
                if (c != '_')
                    { sb.append(c); }
                else if ((i+1 < length) && (encodedKey.charAt(i+1) == '_'))
                    {
                    sb.append('_');
                    i++;
                    }
                else
                    { sb.append('/'); }
                }
            return sb.toString().toLowerCase(Locale.ROOT);
            }

        private boolean hasPrefix(String envVarName)
            {
            return envVarName.regionMatches(CASE_INSENSITIVE_NAMES, 0, prefix, 0, prefix.length());
            }

        /**
         * a variable belongs to this layer if {@link #getEntry(String)} finds it under that name: the lookup
         * uses the UPPER CASE name, so where names are case-sensitive, a variable with lower-case letters is
         * not one of ours (listing it would offer a key that then cannot be read).
         */
        private boolean isLayerVariable(String envVarName)
            {
            return hasPrefix(envVarName)
                    && (CASE_INSENSITIVE_NAMES || envVarName.equals(envVarName.toUpperCase(Locale.ROOT)));
            }

        /**
         * Until 0.8.16 a literal '_' in a key was not doubled, so the key "max_connections" was read from
         * ..._MAX_CONNECTIONS, the variable of "max/connections". Such a variable is no longer used for
         * the key with the underscore; say so once, instead of dropping the override silently.
         * Transition aid: to be removed in 0.9.
         */
        private void warnAboutLegacyName(String hierarchicalKey, String currentName)
            {
            if ((logger == null) || (hierarchicalKey.indexOf('_') < 0))
                { return; }
            String legacyName = (prefix+hierarchicalKey)
                    .replace("__", "_ESC_")
                    .replace("/", "_")
                    .replace("_ESC_", "__")
                    .toUpperCase(Locale.ROOT);
            if (legacyName.equals(currentName) || (getEnv(legacyName) == null))
                { return; }
            if (warnedLegacyNames.add(legacyName))
                {
                logger.warn("environment variable "+LogSafe.text(legacyName)+" is not used for the key \""
                        +LogSafe.text(hierarchicalKey)+"\": since 0.8.17 an underscore in a key is written doubled. "
                        +"Rename the variable to "+LogSafe.text(currentName));
                }
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
         * @param hierarchicalKey key(s), with '/' as a separator for key parts.
         *                        '/' is mapped to '_', and existing '_' to '__'.
         * @return the config entry, or null if not found.
         */
        @Override
        public ConfigEntry getEntry(String hierarchicalKey)
            {
            String envVarName = prefix+encodeKey(hierarchicalKey);
            String value = getEnv(envVarName);
            if (value != null)
                {
                return ConfigEntryFactory.createEntry(hierarchicalKey, value, ConfigEntryType.STRING, null, source);
                }
            warnAboutLegacyName(hierarchicalKey, envVarName);
            return null;
            }

        @Override
        public boolean isWriteable()
            {
            return false;
            }

        /**
         * check if this layer is empty.
         * The prefix is matched the way the lookup finds variables: in UPPER CASE,
         * ignoring case only where the operating system does (Windows).
         *
         * @return true if no environment variable keys match the prefix; false otherwise
         */
        @Override
        public boolean isEmpty()
            {
            return getEnv().keySet().stream().noneMatch(this::isLayerVariable);
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
        public void erase()
            {
            if (envOverride != null)
                {
                envOverride.clear();
                }
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
                    .filter(this::isLayerVariable)
                    .map(k->decodeKey(k.substring(prefix.length())))
                    .collect(Collectors.toList()).iterator();
            }
    }
}
//___EOF___
