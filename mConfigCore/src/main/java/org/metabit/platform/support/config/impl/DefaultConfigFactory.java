package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigFactoryComponent;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.provider.ConfigSchemaProvider;
import org.metabit.platform.support.osdetection.OperatingSystem;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * <p>DefaultConfigFactory class.</p>
 *
 * @version $Id: $Id
 */
public class DefaultConfigFactory implements ConfigFactory
{
    public     ConfigFactoryInstanceContext                ctx;
    private       ConfigEventList                             events; // can be re-initialized at init time.
    private       Boolean                                     throwExceptionOnEmptyConfigsFlag;
    private final Map<String, WeakReference<Configuration>>   instantiatedConfigs;
    private       ConfigLoggingInterface                      logger;
    private       Map<String, ConfigSecretsProviderInterface> activeSecretsProviders;
    private       boolean                                     closed;


    /**
     * <p>Constructor for DefaultConfigFactory.</p>
     */
    public DefaultConfigFactory()
        {
        this.events = new ConfigEventList(1000);
        this.instantiatedConfigs = new HashMap<>();
        this.activeSecretsProviders = new HashMap<>();
        this.closed = false;
        }

    /** {@inheritDoc} */
    @Override
    public int evaluateRequirements(ConfigFactorySettings configFactorySettings)
        {
        throwExceptionOnEmptyConfigsFlag = configFactorySettings.getBoolean(ConfigFeature.EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND);
        return 0;
        }

    /** {@inheritDoc} */
    @Override
    public boolean initialize(ConfigFactoryInstanceContext ctx)
        {
        this.ctx = ctx;
        ctx.setFactory(this);
        this.logger = ctx.getLogger();

        // re-initialize events with correct size from settings
        int maxEvents = ctx.getSettings().getInteger(ConfigFeature.EVENTS_MAX_FACTORY);
        int dedupLimit = ctx.getSettings().getInteger(ConfigFeature.EVENTS_DEDUP_RECENT_LIMIT);
        
        // Preserve any events that occurred before re-init if they still fit
        ConfigEventList oldEvents = this.events;
        this.events = new ConfigEventList(maxEvents, dedupLimit);
        if (oldEvents != null && !oldEvents.isEmpty())
            {
            this.events.addAll(oldEvents);
            }
        
        if (ctx.getSourceChangeNotifier() == null)
            {
            ctx.setSourceChangeNotifier(new SourceChangeNotifier(ctx));
            }

        // check sources and storages are OK.
        if (ctx.getConfigStorages() == null || ctx.getConfigStorages().isEmpty())
            {
            logger.warn("No config storages provided during initialization");
            }

        // work our way through the settings we handle
        final ConfigFactorySettings settings = ctx.getSettings();

        // init.
        // we need a pass-by-reference here. java.lang.ref is concerned with GC; AtomicReference with atomicity. We need neither
        // AtomicReference<ConfigFactoryInstanceContext> ctxref= new AtomicReference<>(ctx);
        // ConfigFactoryInstanceContext[] ctxref = new ConfigFactoryInstanceContext[1];
        // ctxref[0] = ctx;

        // ----- the loop for the search path init went missing ----


        // Initialize all discovered storages.
        // Post-initialization, we will sort the search list to respect STORAGE_TYPE_PRIORITIES.
        Map<String, ConfigStorageInterface> discoveredStorages = ctx.getConfigStorages();
        List<String> priorityList = settings.getStrings(ConfigFeature.STORAGE_TYPE_PRIORITIES);
        boolean allowAll = settings.getBoolean(ConfigFeature.STORAGE_TYPE_ALLOW_ALL_STORAGES);

        // Initialize all discovered storages.
        // We sort them by priority so that bootstrap sources (like files/env) are initialized before networked ones (like zookeeper).
        List<ConfigStorageInterface> sortedDiscoveredStorages = new ArrayList<>(discoveredStorages.values());
        Collections.sort(sortedDiscoveredStorages, (a, b) -> {
            int pA = priorityList.indexOf(a.getStorageID());
            int pB = priorityList.indexOf(b.getStorageID());
            if (pA == -1) pA = Integer.MAX_VALUE;
            if (pB == -1) pB = Integer.MAX_VALUE;
            return pA - pB; // Lower index (higher priority) first
        });

        // Initialize all discovered storages.
        for (ConfigStorageInterface storage : sortedDiscoveredStorages)
            {
            storage.init(ctx);
            }

        // Initialize optional components
        ServiceLoader<ConfigFactoryComponent> factoryComponents = ServiceLoader.load(ConfigFactoryComponent.class, ctx.getClassLoader());
        for (ConfigFactoryComponent component : factoryComponents)
            {
            component.initialize(ctx);
            }

        // Post-initialization: sort the search list to respect STORAGE_TYPE_PRIORITIES.
        // Higher scope ordinal comes first.
        // Within the same scope, later-added layers in LayeredConfiguration have higher priority.
        // Since getConfig loads from start to end of searchList, we want higher priority storages LATER in the list.
        List<ConfigLocation> searchListEntries = ctx.getSearchList().getEntries();
        Collections.sort(searchListEntries, (a, b)->
            {
            if (a.getScope() != b.getScope())
                {
                return b.getScope().ordinal()-a.getScope().ordinal(); // Higher scope first
                }

            // If we don't have a priority list, maintain stable order.
            if (priorityList == null) return 0;

            int pA = priorityList.indexOf(a.getStorage().getStorageID());
            int pB = priorityList.indexOf(b.getStorage().getStorageID());

            // If not in list, use a very large index (lowest priority)
            if (pA == -1) pA = Integer.MAX_VALUE;
            if (pB == -1) pB = Integer.MAX_VALUE;

            // Higher priority (lower index in priorityList) should be LATER in the searchList
            return pB-pA;
            });

        // debug output
        if (ctx.getSettings().getBoolean(ConfigFeature.QUIET) == Boolean.FALSE)
            {
            logger = ctx.getLogger();
            logger.info("mConfig initialised, using logger "+logger.getServiceModuleName());
            // debug-dump the settings here @DEBUG during development
            logger.debug("settings");
            ctx.getSettings().forEach((key, value)->
                    logger.debug(String.format("\t%s\t%s\t: %s", key, value.getClass().getCanonicalName(), value)));
            logger.debug("we're on "+ctx.getSettings().getObject(ConfigFeature.CURRENT_PLATFORM_OS, OperatingSystem.class));
            logger.debug("config storages");
            ctx.getConfigStorages().forEach((key, value)->
                    logger.debug(String.format("\t%s\t%s", key, value.getClass().getSimpleName())));
            logger.debug("config formats");
            ctx.getConfigFormats().forEach((key, value)->
                    logger.debug(String.format("\t%s\t%s", key, value.getClass().getSimpleName())));
            logger.debug("config secrets providers");
            ctx.getConfigSecretsProviders().forEach((key, value)->
                    logger.debug(String.format("\t%s\t%s", key, value.getClass().getSimpleName())));
            logger.debug("search list");
            ctx.getSearchList().getEntries().forEach((value)->
                    logger.debug("\t"+value.toLocationString()));
            }

        // process discovered schemas
        try {
            java.util.ServiceLoader<ConfigSchemaProvider> schemaProviders = java.util.ServiceLoader.load(ConfigSchemaProvider.class, ctx.getClassLoader());
            for (ConfigSchemaProvider provider : schemaProviders)
                {
                Map<String, ConfigSchema> discovered = provider.discoverSchemas(ctx);
                discovered.forEach((name, schema) -> ctx.getSchemaRepository().registerSchema(name, schema));
                }
        } catch (Error | Exception t) {
            // If the provider package or service loader fails, we continue without discovered schemas.
            if (logger != null) logger.debug("Discovery of schemas skipped or failed.");
        }

        // process manually provided schemas from settings
        Map<String, ConfigSchema> manualSchemas = (Map<String, ConfigSchema>) settings.getObject(ConfigFeature.CONFIG_SCHEMA_LIST, Map.class);
        if (manualSchemas != null)
            {
            manualSchemas.forEach((name, schema) -> ctx.getSchemaRepository().registerSchema(name, schema));
            }

        return true;
        }

    @Override
    public void addConfigSchema(String configName, String jsonFormattedConfigSchema)
            throws ConfigCheckedException
        {
        if (closed)
            { throw new ConfigException("ConfigFactory was closed."); }
        Map<String, ConfigSchema> schemas = ConfigSchema.fromJSON(jsonFormattedConfigSchema, this.ctx);
        if ((configName == null) || (configName.isEmpty()))
            {
            schemas.forEach((name, schema) -> ctx.getSchemaRepository().registerSchema(name, schema));
            }
        else
            {
            // normal case where we expect a single schema.
            if (schemas.size() != 1)
                throw new ConfigCheckedException(new IllegalArgumentException("content must be exactly one schema for a given name"));
            String sanitizedName = ConfigNameSanitizer.sanitize(configName);
            ctx.getSchemaRepository().registerSchema(sanitizedName, schemas.values().iterator().next());
            }
        return;
        }

    @Override
    public java.util.Set<ConfigDiscoveryInfo> listAvailableConfigurations()
        {
        if (closed)
            { throw new ConfigException("ConfigFactory was closed."); }
        java.util.Set<ConfigDiscoveryInfo> allConfigs = new java.util.HashSet<>();
        for (ConfigLocation location : this.ctx.getSearchList().getEntries())
            {
            allConfigs.addAll(location.getStorage().listAvailableConfigurations(location));
            }
        return allConfigs;
        }

    @Override
    public List<ConfigLocation> getSearchList()
        {
        if (closed)
            { throw new ConfigException("ConfigFactory was closed."); }
        return Collections.unmodifiableList(this.ctx.getSearchList().getEntries());
        }

    /** {@inheritDoc} */
    @Override
    public ConfigEventList getEvents()
        {
        return this.events;
        }


    @Override
    public void close()
        {
        if (closed)
            {
            return;
            }
        
        List<Configuration> toClose = new ArrayList<>();
        instantiatedConfigs.values().forEach(v -> {
            Configuration config = v.get();
            if (config != null) {
                toClose.add(config);
            }
        });

        for (Configuration config : toClose)
            {
            try
                {
                config.close();
                }
            catch (Exception e)
                {
                if (logger != null) logger.warn("close() failed for config instance of "+config.getConfigName());
                }
            }
        instantiatedConfigs.clear();

        if (ctx.getSourceChangeNotifier() != null)
            {
            ctx.getSourceChangeNotifier().exit();
            }

        if (ctx != null && ctx.getConfigStorages() != null)
            {
            for (ConfigStorageInterface storage : ctx.getConfigStorages().values())
                {
                try
                    {
                    storage.exit();
                    }
                catch (Exception e)
                    {
                    if (logger != null)
                        {
                        logger.warn("close() failed for storage "+storage.getStorageID());
                        }
                    }
                }
            }

        this.closed = true;
        return;
        }

    @Override
    public void notifyConfigurationClosed(String sanitizedConfigName)
        {
        instantiatedConfigs.remove(sanitizedConfigName);
        }

    /** {@inheritDoc} */
    @Override
    public Configuration getConfig(final String configName)
            throws ConfigException
        {
        // find associated schema
        final ConfigSchema schema = this.ctx.getSchemaRepository().getSchema(ConfigNameSanitizer.sanitize(configName));
        return this.getConfig(configName, schema);
        }


    /** {@inheritDoc} */
    @Override
    public Configuration getConfig(final String configName, final ConfigSchema configSchema)
            throws ConfigException
        {
        return this.getConfig(configName, configSchema, EnumSet.allOf(ConfigScope.class));
        }

    /** {@inheritDoc} */
    @Override
    public Configuration getConfigSpecial(final String configName, final EnumSet<ConfigScope> scopes, final Map<String, String> wanted)
            throws ConfigCheckedException
        {
        if (configName == null)
            throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
        if (closed)
            throw new ConfigException("ConfigFactory was closed.");
        String sanitizedConfigName = ConfigNameSanitizer.sanitize(configName);

        if (wanted.isEmpty()) return null;
        if (!wanted.containsKey("type")) return null;
        // only type this function currently supports is "blob".
        if (!wanted.get("type").equalsIgnoreCase("blob")) return null;

        // 2. prepare
        BlobConfiguration blobConfig = new BlobConfiguration(sanitizedConfigName, ctx, this, scopes);

        // 3. go through the search list. For each entry, check with the respective ConfigSource whether the combination SearchListEntry + configName yields results.
        // this is for finding existing BLOBs.
        for (ConfigLocation searchLocation : ctx.getSearchList().getEntries())
            {
//            if (!scopes.contains(location.getScope()))
//                continue; // skip the scopes which we don't want
            ConfigStorageInterface storage = searchLocation.getStorage();
            storage.updateBlobConfigurations(sanitizedConfigName, searchLocation, blobConfig);
            }
        /*
        // 4. check whether an exception is wanted for empty configs
        if (throwExceptionOnEmptyConfigsFlag)
            {
            if (layeredCfg.isEmpty())
                throw new ConfigException(ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND);
            }
        // 5. keep an instance in a map for future lookup, and for future cleanup!
        instantiatedConfigs.put(sanitizedConfigName, layeredCfg);
         */
        // 6. return it.
        return new BlobConfigurationFacade(blobConfig);
        }

    /*
    the actual configuration finding.
     */
    private Configuration getConfig(String configName, ConfigSchema configSchema, EnumSet<ConfigScope> scopes)
        {
        if (configName == null)
            { throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID); }
        if (closed)
            { throw new ConfigException("ConfigFactory was closed."); }
        String sanitizedConfigName = ConfigNameSanitizer.sanitize(configName);

        // check our map whether we have this instantiated already, and return the same instance if we do.
        WeakReference<Configuration> ref = instantiatedConfigs.get(sanitizedConfigName);
        if (ref != null)
            {
            Configuration config = ref.get();
            if (config != null)
                {
                return config;
                }
            else
                {
                instantiatedConfigs.remove(sanitizedConfigName);
                }
            }

        LayeredConfiguration layeredCfg = new LayeredConfiguration(sanitizedConfigName, configSchema, ctx, this); // which is a facade to the LayeredConfiguration.
        // 3. go through the search list. For each entry, check with the respective ConfigSource whether the combination SearchListEntry + configName yields results.
        List<ConfigLocation> searchList = ctx.getSearchList().getEntries();

        for (ConfigLocation location : searchList)
            {
            ConfigStorageInterface storage = location.getStorage();
            storage.updateConfigurationLayers(sanitizedConfigName, location, layeredCfg);
            storage.provideAdditionalLayers(sanitizedConfigName, layeredCfg);
            }

        // 4. check whether an exception is wanted for empty configs
        if (throwExceptionOnEmptyConfigsFlag)
            {
            if (layeredCfg.isEmpty())
                throw new ConfigException(ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND);
            }
        // 5. keep an instance in a map for future lookup, and for future cleanup!
        instantiatedConfigs.put(sanitizedConfigName, new WeakReference<>(layeredCfg));
        // 6. return it.
        return layeredCfg;
        }


    // Planned: explicit creation improvements (derive scope/format from scheme, return writable layer).

    /**
     * <p>createConfig.</p>
     *
     * @param configName   a {@link java.lang.String} object
     * @param scope        a {@link org.metabit.platform.support.config.ConfigScope} object
     * @param configSchema a {@link ConfigSchema} object
     * @return a {@link org.metabit.platform.support.config.Configuration} object
     *
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    @Deprecated
    public Configuration createConfig(final String configName, final ConfigScope scope, final ConfigSchema configSchema)
            throws ConfigCheckedException
        {
        throw new ConfigCheckedException(new UnsupportedOperationException("not implemented yet"));
        }


    // Planned extensions here.

    /** {@inheritDoc} */
    @Override
    public void prependSearchEntry(ConfigLocation location, ConfigScope scope)
        {
        this.ctx.getSearchList().insertAtScopeStart(location, scope);
        }

    /** {@inheritDoc} */
    @Override
    public void appendSearchEntry(ConfigLocation location, ConfigScope scope)
        {
        this.ctx.getSearchList().insertAtScopeEnd(location, scope);
        }
}
//___EOF___
