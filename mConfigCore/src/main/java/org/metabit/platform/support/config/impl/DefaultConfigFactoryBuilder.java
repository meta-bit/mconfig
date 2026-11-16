package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.impl.entry.BooleanLiterals;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.logging.InternalLogger;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.osdetection.PlatformDetector;

import java.util.*;
import java.util.regex.Pattern;

import static org.metabit.platform.support.config.ConfigFeature.*;

/**
 * Default implementation of ConfigFactoryBuilderInterface.
 * internal component of mconfig core implementation
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
public class DefaultConfigFactoryBuilder implements ConfigFactoryBuilder
{
    private static final Pattern                            validIDpattern       = Pattern.compile("([A-Za-z0-9_-])+");
    private static final String                             inversePatternString = "";
    private static       boolean                            testModePermitted    = true;
    private              boolean                            testModeActive       = false;
    private              EnumMap<ConfigScope, List<String>> testModeDirectories;
    private final        ConfigFactorySettings              configFactorySettings;
    private final        List<String>                       schemaJsons          = new ArrayList<>();
    private final        List<ConfigLocation>               manualSources        = new ArrayList<>();
    private              Map<String, ConfigSchema>          manualSchemas;
    private              ClassLoader                        classLoader;

    public DefaultConfigFactoryBuilder(final Map<String, String> configFactoryParameters)
            throws ConfigCheckedException
        {
        configFactorySettings = initDefaultSettings();
        parseAndApplyConfigParameters(configFactorySettings, configFactoryParameters);
        constructorInit(configFactorySettings);
        }

    public DefaultConfigFactoryBuilder(final String companyName, final String applicationName)
            throws ConfigException
        {
        configFactorySettings = initDefaultSettings();
        configFactorySettings.setString(COMPANY_NAME, companyName);
        configFactorySettings.setString(APPLICATION_NAME, applicationName);
        constructorInit(configFactorySettings);
        }

    public DefaultConfigFactoryBuilder(final String companyName, final String applicationName, final String subPath)
            throws ConfigException
        {
        configFactorySettings = initDefaultSettings();
        configFactorySettings.setString(COMPANY_NAME, companyName);
        configFactorySettings.setString(APPLICATION_NAME, applicationName);
        configFactorySettings.setString(SUB_PATH, subPath);
        constructorInit(configFactorySettings);
        }

    private void parseAndApplyConfigParameters(ConfigFactorySettings targetSettings, Map<String, String> parameters)
            throws ConfigException
        {
        HashMap<String, ConfigFeature> featureLookup = new HashMap<String, ConfigFeature>();
        for (ConfigFeature f : ConfigFeature.values())
            {
            featureLookup.put(f.name().toUpperCase(), f);
            featureLookup.put(f.name().toUpperCase().replace("_", ""), f);
            }
        for (Map.Entry<String, String> entry : parameters.entrySet())
            {
            String key = entry.getKey();
            String value = entry.getValue();
            if (featureLookup.containsKey(key.toUpperCase()))
                {
                ConfigFeature currentFeature = featureLookup.get(key.toUpperCase());
                if (currentFeature.isBooleanType())
                    {
                    Boolean flag = BooleanLiterals.parse(value);
                    if (flag == null)
                        throw new ConfigException("feature \""+currentFeature.name()+"\" takes a Boolean value: true/false, yes/no, on/off, 1/0");
                    targetSettings.setBoolean(currentFeature, flag);
                    }
                else if (currentFeature.isStringType())
                    targetSettings.setString(currentFeature, value);
                else if (currentFeature.isNumberType())
                    targetSettings.setInteger(currentFeature, Integer.valueOf(value));
                else
                    throw new UnsupportedOperationException("type of feature "+currentFeature.name()+" not handled in mapping function");
                }
            else
                throw new ConfigException("no feature named \""+key+'\"');
            }
        }

    private static ConfigLoggingInterface findLoggerForConfigUse(final ClassLoader classLoader, final String specificLogger, ConfigFactorySettings configFactorySettings)
            throws ConfigException
        {
        ConfigLoggingInterface logger = null;

        String redirectTarget = configFactorySettings.getString(LOGGING_REDIRECT_TARGET);
        if (redirectTarget != null)
            {
            logger = new InternalLogger(redirectTarget);
            if (logger.init(configFactorySettings))
                {
                return logger;
                }
            }

        try
            {
            int highestPriorityEncountered = 0;
            ServiceLoader<ConfigLoggingInterface> slclis = ServiceLoader.load(ConfigLoggingInterface.class, classLoader);
            for (ConfigLoggingInterface current : slclis)
                {
                if (specificLogger.equalsIgnoreCase(current.getServiceModuleName()))
                    return current;
                int priority = current.getServiceModulePriority();
                if (priority > highestPriorityEncountered)
                    {
                    if (current.init(configFactorySettings) == true)
                        {
                        highestPriorityEncountered = priority;
                        logger = current;
                        }
                    }
                }
            }
        catch (ServiceConfigurationError sce)
            {
            throw new ConfigException(sce);
            }
        return (logger != null) ? logger : NullLogging.getSingletonInstance();
        }

    private ConfigFactorySettings initDefaultSettings()
        {
        ConfigFactorySettings configFactorySettings = new ConfigFactorySettings();
        ConfigFactorySettings.initDefaults(configFactorySettings);
        return configFactorySettings;
        }

    private void constructorInit(ConfigFactorySettings configFactorySettings)
            throws ConfigException
        {
        try
            {
            for (ConfigFeature feature : new ConfigFeature[] {COMPANY_NAME, APPLICATION_NAME, SUB_PATH})
                {
                String value = configFactorySettings.getString(feature);
                if (value != null && !value.isEmpty())
                    configFactorySettings.setString(feature, value);
                }

            PlatformDetector platformDetector = new PlatformDetector();
            configFactorySettings.setObject(CURRENT_PLATFORM_OS, platformDetector.getOs());
            String userName = System.getProperty("user.name");
            if (userName != null) // may be missing in minimal environments; not worth failing the start
                configFactorySettings.setString(CURRENT_USER_ID, userName);
            // HOSTNAME is determined on first read (ConfigFactorySettings): the lookup may fail or wait for DNS
            }
        catch (Exception t)
            {
            throw new ConfigException(t);
            }
        testModeDirectories = new EnumMap<ConfigScope, List<String>>(ConfigScope.class);
        manualSchemas = new HashMap<>();
        }

    @Override
    @SuppressWarnings("removal") // ConfigStorageInterface.isInitializedWhenAddedAsSource(), until MLIB-155
    public ConfigFactory build()
            throws ConfigException
        {
        List<String> refusedRuntimeSettings = org.metabit.platform.support.config.impl.RuntimeConfigurator.applyRuntimeSettingsFromEnvVars(configFactorySettings);

        classLoader = this.getClass().getClassLoader();
        if (configFactorySettings.getBoolean(USE_SYSTEM_CLASS_LOADER))
            {
            classLoader = ClassLoader.getSystemClassLoader();
            }
        if (configFactorySettings.getBoolean(USE_CONTEXT_CLASS_LOADER))
            {
            classLoader = Thread.currentThread().getContextClassLoader();
            }
        String specificLogger = configFactorySettings.getString(LOGGING_TO_USE_IN_CONFIGLIB);
        ConfigLoggingInterface logger = findLoggerForConfigUse(classLoader, specificLogger, configFactorySettings);
        logger.trace("logging started");
        for (String refused : refusedRuntimeSettings)
            {
            logger.warn("runtime setting "+refused+" ignored: only code may set this feature");
            }
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(configFactorySettings);
        ctx.setLogger(logger);
        ctx.setClassLoader(classLoader);
        ConfigFactory configFactory = findConfigFactory(classLoader, configFactorySettings, logger);
        if (configFactory == null)
            throw new ConfigException("basic module loading failed");

        // self-configuration from mconfig.properties: after code and runtime settings (which win),
        // and before the test-mode gates, so that forbidTestMode() and PERMIT_TEST_MODE also cover file-set test mode.
        SelfConfigurator.apply(configFactorySettings, classLoader, logger);

        // enable or disable test mode based on settings. security relevant
        if (testModePermitted && configFactorySettings.getBoolean(PERMIT_TEST_MODE))
            {
            if (testModeActive || configFactorySettings.getBoolean(TEST_MODE))
                {
                configFactorySettings.setBoolean(TEST_MODE, true);
                if (testModeDirectories != null && !testModeDirectories.isEmpty())
                    {
                    List<String> unparsedDirs = new ArrayList<>();
                    for (Map.Entry<ConfigScope, List<String>> entry : testModeDirectories.entrySet())
                        {
                        for (String path : entry.getValue())
                            {
                            unparsedDirs.add(entry.getKey().name()+":"+path);
                            }
                        }
                    configFactorySettings.setStrings(TESTMODE_DIRECTORIES, unparsedDirs);
                    }
                else if (configFactorySettings.isSet(TESTMODE_DIRECTORIES))
                    {
                    // keep existing TESTMODE_DIRECTORIES if they were set via setFeature
                    }
                logger.info("test mode enabled");
                }
            }
        else
            {
            configFactorySettings.setBoolean(TEST_MODE, false);
            }

        // mConfig modules present without service registration: broken packaging (MLIB-156). After self-configuration,
        // so a component can accept it in its own mconfig.properties.
        ServiceRegistrationCheck.check(classLoader, configFactorySettings, logger);

        // collect the providers
        Map<String, ConfigStorageInterface> configStorages;
        Map<String, ConfigFormatInterface> configFormats;
        Map<String, ConfigSecretsProviderInterface> configSecretsProviders;

        List<String> disabledModules = configFactorySettings.getStrings(DISABLED_MODULE_IDS);

        configStorages = findConfigStorages(classLoader, configFactorySettings, logger);
        if (disabledModules != null && !disabledModules.isEmpty())
            {
            for (String id : disabledModules)
                { configStorages.remove(id); }
            }
        if (!configFactorySettings.getBoolean(STORAGE_TYPE_ALLOW_ALL_STORAGES))
            {
            List<String> priorities = configFactorySettings.getStrings(STORAGE_TYPE_PRIORITIES);
            configStorages.keySet().removeIf(id ->
                {
                boolean excluded = !isStorageListed(id, priorities);
                if (excluded)
                    { logger.debug("storage "+id+" not in STORAGE_TYPE_PRIORITIES; excluded by STORAGE_TYPE_ALLOW_ALL_STORAGES=false"); }
                return excluded;
                });
            }

        configFormats = findConfigFormats(classLoader, configFactorySettings, logger);
        if (disabledModules != null && !disabledModules.isEmpty())
            {
            for (String id : disabledModules)
                { configFormats.remove(id); }
            }

        configSecretsProviders = findConfigSecretsProviders(classLoader, configFactorySettings, logger);
        if (disabledModules != null && !disabledModules.isEmpty())
            {
            for (String id : disabledModules)
                { configSecretsProviders.remove(id); }
            }

        // guard clauses, with lengthy messages. Problems of this kind are rare and indicate misconfiguration.
        if (configStorages.isEmpty())
            {
            logger.error("*** No config storages discovered. ***\nHINTS: \n- Enable DEBUG logging (`mConfigLoggingSlf4j` + <logger name=\"metabit.config\" level=\"DEBUG\"/>)\n- Use factory.listAvailableConfigurations()\n- Run `mconfig <company>:<app> list` CLI\n- Verify paths/formats per [Getting Started](https://github.com/meta-bit/mconfig/blob/master/documentation/src/site/markdown/13_getting_started.md)\n- See FAQ: [Config values always defaults/null]");
            }
        if (configFormats.isEmpty())
            {
            logger.error("*** No config formats discovered. ***\nHINTS: Add format modules (e.g., `mConfigFormatJavaProperties`, `mConfigFormatYAMLwithJackson`) matching your file extensions (.properties, .yaml, etc.).");
            }

        if (configFactorySettings.getBoolean(TEST_MODE) && (testModeDirectories == null || testModeDirectories.isEmpty()) && !configFactorySettings.isSet(TESTMODE_DIRECTORIES))
            {
            logger.warn("*** Test mode active but no test directories configured. ***\nHINTS: \n- Place files in `src/test/resources/.config/<company>/<app>/`\n- Or `ConfigFactoryBuilder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of(\"USER:src/test/configs\"));`\n- See [Test Mode docs](https://github.com/meta-bit/mconfig/blob/master/documentation/src/site/markdown/15_test_mode.md)");
            }

        // provide full context to formats so they can emit events, etc.
        for (ConfigFormatInterface fmt : configFormats.values())
            {
            try
                {
                fmt.initialize(ctx);
                logger.trace("initialized format "+fmt.getFormatID()+" with context");
                }
            catch (Exception e)
                {
                logger.warn("failed to initialize format "+fmt.getFormatID(), e);
                }
            }

        // store providers found
        ctx.setConfigFormats(configFormats);
        ctx.setConfigStorages(configStorages);
        ctx.setConfigSecretsProviders(configSecretsProviders);

        // merge schemes into immutable map
        if (!schemaJsons.isEmpty() || (manualSchemas != null && !manualSchemas.isEmpty()))
            {
            Map allSchemes = new HashMap();
            if (manualSchemas != null && !manualSchemas.isEmpty())
                { allSchemes.putAll(manualSchemas); }
            for (String json : schemaJsons)
                {
                try
                    {
                    Map parsed = ConfigSchema.fromJSON(json, ctx);
                    allSchemes.putAll(parsed);
                    }
                catch (ConfigCheckedException e)
                    {
                    throw new ConfigException(e);
                    }
                }
            configFactorySettings.setObject(CONFIG_SCHEMA_LIST, Collections.unmodifiableMap(allSchemes));
            }

        // init factory with the providers found
        if (!configFactory.initialize(ctx))
            throw new ConfigException(ConfigException.ConfigExceptionReason.FACTORY_INIT_FAILED);

        // register manual sources. A storage instance created by the application (e.g. by a source builder)
        // is initialized here if it asks for it; discovered instances were initialized by the factory.
        // Its test() is asked first, as for discovered storages: in test mode, Vault and AWS refuse
        // unless their BYPASS_TEST feature is set, so tests reach real endpoints only when intended.
        Set<ConfigStorageInterface> discoveredStorages = Collections.newSetFromMap(new IdentityHashMap<>());
        discoveredStorages.addAll(configStorages.values());
        Set<ConfigStorageInterface> addedStorages = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ConfigLocation source : manualSources)
            {
            ConfigStorageInterface storage = source.getStorage();
            if (storage != null && !discoveredStorages.contains(storage) && storage.isInitializedWhenAddedAsSource()
                    && !addedStorages.contains(storage))
                {
                if (!storage.test(configFactorySettings, logger))
                    {
                    logger.info("added source "+source.toLocationString()+" not used: storage "+storage.getStorageID()
                            +" is not active here (in test mode, set its BYPASS_TEST feature to use it)");
                    continue;
                    }
                if (!storage.init(ctx))
                    {
                    logger.warn("added source "+source.toLocationString()+" not used: its storage "+storage.getStorageID()+" failed to initialize");
                    continue;
                    }
                addedStorages.add(storage);
                }
            configFactory.appendSearchEntry(source, source.getScope());
            }

        // perform post-initialization for all providers found,
        // providing the now fully functional configFactory for self-configuration.
        // order:
        // 1. storage
        // 2. secret provider
        // 3. factory component
        // 4. logger
        // Each of which may want to use/build upon functionality by the previous ones.
        // A component may have multiple roles, which would result in multiple post-init calls.
        // hence defensive programming: avoid duplicate initialization.
        final BootstrapConfigContext bootstrapCtx = (BootstrapConfigContext) configFactory;
        Set<Object> postInitialized = new HashSet<>();
        for (ConfigStorageInterface storage : configStorages.values())
            {
            if (postInitialized.add(storage))
                {
                storage.postInit(bootstrapCtx);
                }
            }
        for (ConfigStorageInterface storage : addedStorages)
            {
            if (postInitialized.add(storage))
                {
                storage.postInit(bootstrapCtx);
                }
            }
        ctx.setAddedSourceStorages(addedStorages);
        for (ConfigSecretsProviderInterface secretsProvider : configSecretsProviders.values())
            {
            if (postInitialized.add(secretsProvider))
                {
                secretsProvider.postInit(configFactory);
                }
            }

        // perform post-initialization for all components found,
        // providing the now fully functional configFactory for self-configuration.
        ServiceLoader<ConfigFactoryComponent> factoryComponents = ServiceLoader.load(ConfigFactoryComponent.class, ctx.getClassLoader());
        for (ConfigFactoryComponent component : factoryComponents)
            {
            try
                {
                if (component.initialize(ctx))
                    {
                    if (postInitialized.add(component))
                        {
                        component.postInit(configFactory);
                        }
                    }
                else
                    {
                    logger.warn("component "+component.getComponentID()+" initialization failed.");
                    }
                }
            catch (Exception e)
                {
                logger.error("failed to initialize component "+component.getComponentID(), e);
                }
            }

        // finally, allow the logger to refine its configuration using the now fully functional factory
        if (postInitialized.add(logger))
            {
            logger.postInit(configFactory);
            }

        // the host name lookup is lazy; only debug logging makes it run at start
        if (logger.isDebugEnabled())
            {
            logger.debug("host "+LogSafe.text(configFactorySettings.getString(HOSTNAME))
                    +", user "+LogSafe.text(configFactorySettings.getString(CURRENT_USER_ID))
                    +", OS "+configFactorySettings.get(CURRENT_PLATFORM_OS));
            }
        return configFactory;
        }


    public static void forbidTestMode()
        { testModePermitted = false; }

    public static void permitTestMode()
        { testModePermitted = true; }

    /**
     * Sets the test mode for the configuration factory instance.
     *
     * @param testMode true to activate test mode, false to deactivate.
     */
    @Override
    public void setTestMode(boolean testMode)
        {
        if (testMode && !testModePermitted)
            {
            return;
            }
        testModeActive = testMode;
        configFactorySettings.setBoolean(TEST_MODE, testMode);
        }

    @Override
    public void setTestConfigPaths(ConfigScope scopeToSetFor, List<String> directories)
            throws ConfigCheckedException
        {
        testModeDirectories.put(scopeToSetFor, Collections.unmodifiableList(directories));
        }

    @Override
    public void setTestParameters(Map<String, String> testParameters)
            throws ConfigException, IllegalArgumentException
        {
        try
            {
            configFactorySettings.setObject(TESTMODE_PARAMETERS, Collections.unmodifiableMap(testParameters));
            }
        catch (IllegalArgumentException iae)
            {
            throw new ConfigException(iae);
            }
        }

    private ConfigFactory findConfigFactory(final ClassLoader classLoader, final ConfigFactorySettings configFactorySettings, final ConfigLoggingInterface logger)
            throws ConfigException
        {
        ConfigFactory configFactory = null;
        try
            {
            int bestQualityEncountered = -1;
            ServiceLoader<ConfigFactory> slclis = ServiceLoader.load(ConfigFactory.class, classLoader);
            for (ConfigFactory current : slclis)
                {
                logger.trace("potential ConfigFactory "+current.getClass().getCanonicalName());
                int matchingQuality = current.evaluateRequirements(configFactorySettings);
                if (matchingQuality > bestQualityEncountered)
                    {
                    bestQualityEncountered = matchingQuality;
                    configFactory = current;
                    }
                }
            }
        catch (ServiceConfigurationError sce)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.JAVA_SERVICE_CONFIGURATION_ERROR, sce);
            }
        return configFactory;
        }

    /** core's own storages: schema defaults and the in-memory RUNTIME/SESSION layers. Never excluded. */
    private static final List<String> ALWAYS_ALLOWED_STORAGES = Arrays.asList("defaults", "RAM");

    /**
     * whether a storage ID is covered by a STORAGE_TYPE_PRIORITIES list: an entry matches the ID itself, or a family of
     * IDs it is the prefix of, separated by "-" or "." ("RAM" covers "RAM-SESSION", "secrets" covers "secrets.vault").
     */
    static boolean isStorageListed(String storageID, List<String> priorities)
        {
        for (List<String> list : Arrays.asList(ALWAYS_ALLOWED_STORAGES, priorities))
            {
            if (list == null)
                { continue; }
            for (String entry : list)
                {
                if (storageID.equals(entry) || storageID.startsWith(entry+"-") || storageID.startsWith(entry+"."))
                    { return true; }
                }
            }
        return false;
        }

    private Map<String, ConfigStorageInterface> findConfigStorages(ClassLoader classLoader, ConfigFactorySettings settings, ConfigLoggingInterface logger)
            throws ConfigException
        {
        Map<String, ConfigStorageInterface> storages = new HashMap();
        try
            {
            ServiceLoader<ConfigStorageInterface> slcsi = ServiceLoader.load(ConfigStorageInterface.class, classLoader);
            // checks storages, adds valid + accepted storages to map
            for (ConfigStorageInterface current : slcsi)
                {
                String id = current.getStorageID();
                // Ignores storage with invalid ID. sanitize ID before logging.
                if ((id == null) || (validIDpattern.matcher(id).matches() == false))
                    {
                    String loggableVariant = (id == null) ? "[null]" : id.replaceAll(inversePatternString, "*");
                    logger.warn("config storage ignored because of invalid ID : \""+loggableVariant+"\"");
                    continue;
                    }
                logger.trace("found config storage with ID "+id);
                if (current.test(configFactorySettings, logger) == true)
                    {
                    logger.debug("config storage "+id+" accepted");
                    storages.put(id, current);
                    }
                else
                    { logger.debug("config storage "+id+" refused to run with current settings"); }
                }
            }
        catch (ServiceConfigurationError sce)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.JAVA_SERVICE_CONFIGURATION_ERROR, sce);
            }
        return storages;
        }

    private Map<String, ConfigFormatInterface> findConfigFormats(ClassLoader classLoader, ConfigFactorySettings settings, ConfigLoggingInterface logger)
            throws ConfigException
        {
        Map<String, ConfigFormatInterface> formats = new HashMap();
        try
            {
            ServiceLoader<ConfigFormatInterface> slcsi = ServiceLoader.load(ConfigFormatInterface.class, classLoader);
            for (ConfigFormatInterface current : slcsi)
                {
                String id = current.getFormatID();
                if (validIDpattern.matcher(id).matches() == false)
                    {
                    String loggableVariant = id.replaceAll(inversePatternString, "*");
                    logger.warn("config format ignored because of invalid ID : \""+loggableVariant+"\"");
                    continue;
                    }
                logger.trace("found config format with ID "+id);
                if (current.testComponent(configFactorySettings, logger) == true)
                    { formats.put(id, current); }
                else
                    { logger.debug("config format "+id+" refused "); }
                }
            }
        catch (ServiceConfigurationError sce)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.JAVA_SERVICE_CONFIGURATION_ERROR, sce);
            }
        return formats;
        }

    private Map<String, ConfigSecretsProviderInterface> findConfigSecretsProviders(ClassLoader classLoader, ConfigFactorySettings settings, ConfigLoggingInterface logger)
            throws ConfigException
        {
        Map<String, ConfigSecretsProviderInterface> secretsProviders = new HashMap();
        try
            {
            ServiceLoader<ConfigSecretsProviderInterface> slcspi = ServiceLoader.load(ConfigSecretsProviderInterface.class, classLoader);
            // registers valid secrets providers
            for (ConfigSecretsProviderInterface current : slcspi)
                {
                String id = current.getProviderID();
                if (validIDpattern.matcher(id).matches() == false)
                    {
                    String loggableVariant = id.replaceAll(inversePatternString, "*");
                    logger.warn("config secrets provider ignored because of invalid ID : \""+loggableVariant+"\"");
                    continue;
                    }
                logger.trace("found config secrets provider with ID "+id);
                secretsProviders.put(id, current);
                }
            }
        catch (ServiceConfigurationError sce)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.JAVA_SERVICE_CONFIGURATION_ERROR, sce);
            }
        return secretsProviders;
        }

    @Override
    public ConfigFactoryBuilder setFeature(ConfigFeature feature, Integer i)
        {
        try
            {
            configFactorySettings.setInteger(feature, i);
            return this;
            }
        catch (IllegalArgumentException iae)
            {
            throw new ConfigException(iae);
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ConfigFactoryBuilder setFeature(ConfigFeatureInterface feature, T value)
        {
        try
            {
            if (feature instanceof ConfigFeature)
                {
                ConfigFeature cf = (ConfigFeature) feature;
                if (cf.isBooleanType()) return setFeature(cf, (Boolean) value);
                if (cf.isStringType()) return setFeature(cf, (String) value);
                if (cf.isNumberType()) return setFeature(cf, (Integer) value);
                if (cf.isStringListType()) return setFeature(cf, (List<String>) value);
                // else it's SPECIAL_CLASS
                configFactorySettings.setObject(cf, value);
                return this;
                }

            switch (feature.getValueType())
                {
                case BOOLEAN:
                    configFactorySettings.setBoolean(feature, (Boolean) value);
                    break;
                case STRING:
                    configFactorySettings.setString(feature, (String) value);
                    break;
                case NUMBER:
                    configFactorySettings.setInteger(feature, (Integer) value);
                    break;
                case STRINGLIST:
                    configFactorySettings.setStrings(feature, (List<String>) value);
                    break;
                case SPECIAL_CLASS:
                    configFactorySettings.setObject(feature, value);
                    break;
                }
            return this;
            }
        catch (Exception e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public ConfigFactoryBuilder setFeature(String featureName, Object value)
        {
        try
            {
            configFactorySettings.setFeature(featureName, value);
            return this;
            }
        catch (Exception e)
            {
            throw new ConfigException(e);
            }
        }

    @Override
    public ConfigFactoryBuilder setFeature(ConfigFeature feature, Boolean b)
        {
        try
            {
            if (feature == TEST_MODE && Boolean.TRUE.equals(b) && !testModePermitted)
                {
                return this;
                }
            configFactorySettings.setBoolean(feature, b);
            return this;
            }
        catch (IllegalArgumentException iae)
            {
            throw new ConfigException(iae);
            }
        }

    @Override
    public ConfigFactoryBuilder setFeature(ConfigFeature feature, String s)
        {
        try
            {
            configFactorySettings.setString(feature, s);
            return this;
            }
        catch (IllegalArgumentException iae)
            {
            throw new ConfigException(iae);
            }
        }

    @Override
    public ConfigFactoryBuilder setFeature(ConfigFeature feature, List<String> strs)
        {
        try
            {
            configFactorySettings.setStrings(feature, strs);
            return this;
            }
        catch (IllegalArgumentException iae)
            {
            throw new ConfigException(iae);
            }
        }

    @Override
    public ConfigFactoryBuilder setSpecialFeature(ConfigFeature feature, List<Object> param)
            throws ConfigCheckedException
        {
        configFactorySettings.setObject(feature, param);
        return this;
        }

    @Override
    public ConfigFactoryBuilder setSchemas(Map<String, ConfigSchema> schemas)
            throws ConfigCheckedException
        {
        manualSchemas.clear();
        manualSchemas.putAll(schemas);
        return this;
        }

    @Override
    public ConfigFactoryBuilder setSecretsProviderConfig(Map<String, Object> config)
        {
        configFactorySettings.setObject(SECRETS_PROVIDER_CONFIG, config);
        return this;
        }

    @Override
    public ConfigFactoryBuilder addSecretsProvider(String providerID, Map<String, Object> config, ConfigScope scope)
        {
        List<Map<String, Object>> providers = (List<Map<String, Object>>) configFactorySettings.getObject(ADDITIONAL_SECRETS_PROVIDERS, List.class);
        if (providers == null)
            {
            providers = new ArrayList<>();
            configFactorySettings.setObject(ADDITIONAL_SECRETS_PROVIDERS, providers);
            }
        Map<String, Object> entry = new HashMap<>();
        entry.put("provider", providerID);
        entry.put("config", config);
        if (scope != null) entry.put("scope", scope.name());
        providers.add(entry);
        return this;
        }

    @Override
    public ConfigFactoryBuilder addSource(ConfigLocation location)
        {
        if (location != null)
            {
            manualSources.add(location);
            }
        return this;
        }

    @Override
    public ConfigFactoryBuilder addSchemaJson(String jsonScheme)
        {
        schemaJsons.add(jsonScheme);
        return this;
        }
}
//___EOF___
