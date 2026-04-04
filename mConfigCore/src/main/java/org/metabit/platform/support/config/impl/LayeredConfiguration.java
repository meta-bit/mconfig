package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryFactory;
import org.metabit.platform.support.config.impl.entry.SpecifiedConfigEntryWrapper;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.SecretConfigEntry;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.schema.NullConfigSchema;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * facade combining hierarchical configuration layers into a combined configuration view.
 * It is the primary Configuration implementation type for mConfig.
 * 
 * @version $Id: $Id
 */
public class LayeredConfiguration extends AbstractConfiguration implements LayeredConfigurationInterface
{
    final String                         configName; // the name this goes by
    ConfigSchema configSchema; // the scheme the contents are to be validated by
    final List<ConfigLayerInterface>     configs; // the actual config data. sorted list, sort on insertion.
    final SourceChangeNotifier           changeNotifier;
    final SourceChangeChecker            changeChecker;
    final         ConfigFactory          configFactory; // parent, producing this
    final         ConfigLoggingInterface logger; // logging
    private final                        ConfigFactoryInstanceContext ctx;
    private final DefaultLayer           defaultLayer; // permanent default
    private final                        ConfigEventList events;
    private final                        boolean descriptionOnCreateFlag;
    private final                        boolean writeCommentsFlag;
    private       boolean                closed = false;
    private final Consumer<ConfigLocation> internalUpdateListener;

    /**
     * <p>Constructor for LayeredConfiguration.</p>
     *
     * @param sanitizedConfigName a {@link java.lang.String} object
     * @param configSchema a {@link ConfigSchema} object
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     * @param configFactory a {@link org.metabit.platform.support.config.ConfigFactory} object
     */
    public LayeredConfiguration(String sanitizedConfigName, final ConfigSchema configSchema,
                                ConfigFactoryInstanceContext ctx, ConfigFactory configFactory)
        {
        this.logger = ctx.getLogger();
        // this.events = new ConfigEventList();
        this.exceptionOnNullFlag = ctx.getSettings().getBoolean(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY);
        this.descriptionOnCreateFlag = ctx.getSettings().getBoolean(ConfigFeature.DESCRIPTION_ON_CREATE);
        this.writeCommentsFlag = ctx.getSettings().getBoolean(ConfigFeature.COMMENTS_WRITING);
        this.configName = sanitizedConfigName;
        this.defaultLayer = new DefaultLayer(ctx);
        this.configFactory = configFactory;
        
        if (configSchema != null)
            {
            this.configSchema = configSchema;
            // @CHECK: who calls the scheme.init() ?
            // init defaults, if possible - transfer scheme entries to default layer
            this.setConfigSchema(configSchema);
            }
        else
            {
            this.configSchema = NullConfigSchema.INSTANCE;
            }
        configs = new ArrayList<>();
        configs.add(defaultLayer);    // needs to go at the lowest priority.
        int maxEvents = ctx.getSettings().getInteger(ConfigFeature.EVENTS_MAX_CONFIGURATION);
        int dedupLimit = ctx.getSettings().getInteger(ConfigFeature.EVENTS_DEDUP_RECENT_LIMIT);
        this.events = new ConfigEventList(maxEvents, dedupLimit);
        this.changeNotifier = new SourceChangeNotifier(ctx);
        this.changeChecker = new SourceChangeChecker(ctx, changeNotifier);
        this.ctx = ctx;

        this.internalUpdateListener = this::handleInternalUpdate;
        if (configFactory != null && ctx.getSourceChangeNotifier() != null)
            {
            for (ConfigLocation location : configFactory.getSearchList())
                {
                ctx.getSourceChangeNotifier().subscribeToConfigLocationUpdates(location, internalUpdateListener);
                }
            }

        return;
        }

    private void handleInternalUpdate(ConfigLocation location)
        {
        if (closed)
            {
            return;
            }
        logger.debug("LayeredConfiguration notified about update in location: " + location);
        synchronized (configs)
            {
            // 1. remove old layers from this location
            configs.removeIf(layer -> location.equals(layer.getSource()));

            // 2. try to read new layers from this location
            location.getStorage().updateConfigurationLayers(configName, location, this);
            }
        }

    @Override
    public ConfigEventList getEvents()
        {
        return this.events;
        }

    /** {@inheritDoc} */
    @Override
    public void add(ConfigLayerInterface singleConfig, ConfigLocation location)
        {
        checkClosed();
        synchronized(configs)
            {
            ConfigScope targetScope = location.getScope();
            if (targetScope != singleConfig.getScope())
                logger.warn("insertion of configuration with scope "+singleConfig.getScope()+" at scope "+targetScope);
            // they are to be added ordered by scope.
            // higher priority (more specific scope) = lower index, lower priority (more generic scope) = higher index.
            int indexToInsertAt = configs.size();
            for (int i = 0; i < configs.size(); i++)
                {
                if (singleConfig.getScope().ordinal() >= configs.get(i).getScope().ordinal())
                    {
                    indexToInsertAt = i;
                    break;
                    }
                }
            configs.add(indexToInsertAt, singleConfig);
            }

        changeChecker.updateConfigList(configs);
        return;
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        checkClosed();
        // config locations, including default layer.
        synchronized(configs)
            {
            return configs.stream().map(ConfigLayerInterface::getSource).collect(Collectors.toList());
            }
        }

    /**
     * @return a list of all current configuration layers.
     */
    public List<ConfigLayerInterface> getLayers()
        {
        checkClosed();
        synchronized(configs)
            {
            return new ArrayList<>(configs);
            }
        }

    /**
     * {@inheritDoc}
     *
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     *
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     *
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     *
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     *
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     */
    @Override
    public void close()
            throws Exception
        {
        if (closed)
            {
            return;
            }
        flush();
        closed = true;

        if (configFactory != null)
            {
            configFactory.notifyConfigurationClosed(configName);
            }
        if (ctx.getSourceChangeNotifier() != null)
            {
            ctx.getSourceChangeNotifier().unsubscribeFromUpdates(internalUpdateListener);
            }
        }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed()
        {
        return closed;
        }

    private void checkClosed()
        {
        if (closed)
            {
            throw new IllegalStateException("Configuration " + configName + " is closed");
            }
        }

    /** {@inheritDoc} */
    @Override
    public int flush()
            throws ConfigCheckedException
        {
        checkClosed();
        int collectedNumberOfChangesFlushed = 0;

        // iterate through all layers, no matter the scope, and try to flush() each.
        for (ConfigLayerInterface configLayer : configs)
            {
            int flushed = configLayer.flush();

            if (flushed > 0)
                {
                collectedNumberOfChangesFlushed += flushed;
                logger.trace("flushed "+flushed+" entries from "+configLayer);
                }
            }

        return collectedNumberOfChangesFlushed;
        }

    /** {@inheritDoc} */
    @Override
    public boolean reload()
            throws ConfigCheckedException
        {
        checkClosed();
        return false;
        }

    @Override
    public ConfigFactoryInstanceContext getContext()
    {
        return ctx;
    }

    /**
     * try to find a writeable config at the given scope.
     * If not possible, then - depending on flags - try to create a new one.
     *
     * @param scope   scope to look in
     * @param fullKey key of the entry
     * @return a writable config entry, or null if not possible
     * @throws org.metabit.platform.support.config.ConfigCheckedException on errors worse than "not possible".
     */
    @Deprecated
    public ConfigEntry getConfigEntryForWriting(ConfigScope scope, String fullKey)
            throws ConfigCheckedException
        {
        // iterate through all config layers, starting with the most specific
        for (ConfigLayerInterface configLayer : configs)
            {
            if (scope != configLayer.getScope())
                {
                continue;                                         // skip scopes outside of.. scope.
                }

            if (!configLayer.isWriteable())
                {
                continue;                                         // skip read-only layers.
                }

            ConfigEntry entry = configLayer.getEntry(fullKey);

            if (entry != null)
                {
                return entry;
                }
            }

        return null;
        }

    @Override
    public String getConfigName()
        { return this.configName; }

    /**
     * {@inheritDoc}
     *
     * iterate through the list from most specific to most generic layer.
     * try to find a config which provides us with an entry matching the requested key.
     */
    @Override
    public ConfigEntry getConfigEntryFromFullKey(final String fullKey, EnumSet<ConfigScope> scopes)
        {
        checkClosed();
        ConfigEntrySpecification spec = configSchema.getSpecification(fullKey);
        if (scopes == null)
            { scopes = EnumSet.allOf(ConfigScope.class); }   // may be limited by feature settings in future.

        // check for changes (periodically)
        changeChecker.updateConfigList(configs);
        // checks occurring async in the background via SourceChangeChecker thread.
        // Synchronous check only if requested or if we are not using the background thread.
        long now = System.currentTimeMillis();
        if (changeChecker.considerCheck(now))
            { changeChecker.checkAndNotify(configs, changeNotifier); }

        // iterate through all config layers, starting with the most specific
        for (ConfigLayerInterface configLayer : configs)
            {
            if (scopes != null && !scopes.contains(configLayer.getScope()))
                { continue; } // skip scopes outside of... scope.

            ConfigEntry entry = configLayer.getEntry(fullKey, spec);

            // returns first entry found across config layers
            if (entry != null)
                {
                logger.info(entry.getKey()+" found in ["+entry.getScope()+"] at location "+configLayer.getSource().toLocationString());
                if (!(spec instanceof NullConfigEntrySpecification))
                    { entry = new SpecifiedConfigEntryWrapper(entry, spec); }

                if (!configSchema.checkConfigEntryValidity(fullKey, entry))
                    { continue; } // skip
                // else:  if there is no Scheme, then there is no check.
                return entry;
                }
            }

        // if provided, the defaults exist as a layer, so they'll be checked before the list ends.
        return null;    // no matching entry found, at all.
        }

    /** {@inheritDoc} */
    @Override
    public void setConfigSchema(final ConfigSchema schema)
        {
        if (schema != null)
            {
            this.configSchema = schema;
            // Cleaning handled by schema impl (SCHEMA_RESETS_DEFAULTS flag)
            schema.transferDefaults(defaultLayer);    // defaultLayer exists by ... default.
            }
        else
            { this.configSchema = NullConfigSchema.INSTANCE; }
        return;
        }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty()
        {
        checkClosed();
        for (ConfigLayerInterface cfg : configs)
            {
            if (!cfg.isEmpty())
                { return false; }
            }
        return true;
        }


    /**
     * {@inheritDoc}
     *
     * is this writeable
     */
    @Override
    public boolean isWriteable()
        {
        checkClosed();
        // Runtime check: any existing writable layer?
        synchronized(configs)
            {
            for (ConfigLayerInterface configLayer : configs)
                {
                if (configLayer.isWriteable())
                    { return true; }
                }
            }
        // Potential to create writable layer
        for (ConfigLocation location : configFactory.getSearchList())
            {
            if (location.getStorage().isGenerallyWriteable())
                { return true; }
            }
        return false;
        }

    /**
     * <p>tryToCreateConfigLayer.</p>
     *
     * @param configName           the name of the configuration
     * @param scopeToCreateLayerAt the scope to create the layer at
     * @param configSchema         the config scheme to apply, if any.
     * @return the instance of the created layer
     * @throws org.metabit.platform.support.config.ConfigCheckedException on failure to create any such layer
     */
    protected ConfigLayerInterface tryToCreateConfigLayer(String configName, ConfigScope scopeToCreateLayerAt, ConfigSchema configSchema)
            throws ConfigCheckedException
        {
        return tryToCreateConfigLayer(configName, scopeToCreateLayerAt, configSchema, false);
        }

    protected ConfigLayerInterface tryToCreateConfigLayer(String configName, ConfigScope scopeToCreateLayerAt, ConfigSchema configSchema, boolean strictTestMode)
            throws ConfigCheckedException
        {
        // Exact scope only; fallback scopes planned for future.
        /*
        we have tried the layers for existing configs already before going here.
        now that we are trying to create one.
        No need to look for existing layers here - we look for writeable storages.
         */
        for (ConfigLocation location : configFactory.getSearchList())
            {
            if (location.getScope() != scopeToCreateLayerAt)
                continue; // skip the scopes which we don't want

            if (strictTestMode)
                {
                if (!location.getStorage().isPreferredTestLocation(location))
                    continue;
                }

            ConfigStorageInterface storage = location.getStorage();
            if (!storage.isGenerallyWriteable())
                {
                logger.debug("config creation: skipped non-writeable ["+storage.getStorageID()+"] "+storage.getStorageName());
                continue; // skip
                }

            ConfigLayerInterface instance = storage.createConfigurationLayer(configName, location, configSchema, this);
            if (instance != null)
                {
                // check if the storage already added the instance to our list
                boolean alreadyAdded = false;
                for (ConfigLayerInterface existing : configs)
                    {
                    if (existing == instance)
                        {
                        alreadyAdded = true;
                        break;
                        }
                    }
                if (!alreadyAdded)
                    this.add(instance, location);
                return instance;
                }
            // else try next
            }
        // OK, we tried all existing storages.
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE); // can't create any such config.
        }


    @Override
    public void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope) throws ConfigCheckedException
        {
        // Validate against scheme before any write attempt (only when a real spec exists).
        ConfigEntrySpecification spec = (configSchema != null) ? configSchema.getSpecification(fullKey) : null;
        if (spec != null && !(spec instanceof org.metabit.platform.support.config.schema.NullConfigEntrySpecification))
            {
            ConfigEntry entryToValidate = ConfigEntryFactory.createEntry(fullKey, value, type, this.configSchema, null);
            if (!spec.validateEntry(entryToValidate))
                {
                throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INPUT_INVALID);
                }
            }

        // Special handling in TEST_MODE with explicit directories: prefer creating a new layer in the designated test dir
        boolean preferCreateInTestMode = false;
        try {
            ConfigFactorySettings settings = ctx.getSettings();
            preferCreateInTestMode = settings.getBoolean(org.metabit.platform.support.config.ConfigFeature.TEST_MODE)
                    && settings.isSet(org.metabit.platform.support.config.ConfigFeature.TESTMODE_DIRECTORIES);
        } catch (Throwable t) {
            // ignore, fallback to normal flow
        }
        if (preferCreateInTestMode)
            {
            // Try to create the config layer at the intended test location first.
            // If creation is not possible (e.g., directory missing and CREATE_MISSING_PATHS is false),
            // fail fast with NOT_WRITEABLE to avoid accidentally writing into default test resources.
            try
                {
                ConfigLayerInterface created = tryToCreateConfigLayer(this.configName, scope, this.configSchema, true);
                if (created != null)
                    {
                    // proceed to normal write flow which will now hit the just-created highest-priority layer
                    }
                }
            catch (ConfigCheckedException e)
                {
                if (e.getReason() == ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE)
                    { throw e; }
                // other reasons fall through to normal flow
                }
            }

        // Priority 1: Update existing entry in the scope (highest priority writeable layer first)
        synchronized(configs)
            {
            for (ConfigLayerInterface configLayer : configs)
                {
                if (configLayer.getScope() != scope)
                    { continue; }
                if (!configLayer.isWriteable())
                    { continue; }

                ConfigEntry entry = configLayer.getEntry(fullKey);
                if (entry != null)
                    {
                    // Update: do not auto-add description on update
                    ConfigEntry updatedEntry = ConfigEntryFactory.createEntry(fullKey, value, type, this.configSchema, configLayer.getSource());
                    configLayer.writeEntry(updatedEntry);
                    return;
                    }
                }

            // Priority 2: Add to an existing writeable layer in the scope (highest priority first)
            for (ConfigLayerInterface configLayer : configs)
                {
                if (configLayer.getScope() != scope)
                    { continue; }
                if (!configLayer.isWriteable())
                    { continue; }

                createAndWriteEntry(configLayer, fullKey, value, type, spec);
                return;
                }
            }

        // Priority 3: Create a new configuration layer instance
        ConfigLayerInterface newLayer = null;
        try
            {
            newLayer = tryToCreateConfigLayer(this.configName, scope, this.configSchema);
            }
        catch (ConfigCheckedException e)
            {
            if (e.getReason() == ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE)
                {
                ConfigEventImpl event = ConfigEventImpl.builder()
                        .severity(ConfigEvent.Severity.WARNING)
                        .domain(ConfigEvent.Domain.WRITE)
                        .kind(ConfigEvent.Kind.REFUSED_NOT_WRITEABLE)
                        .detailCode("WRITE_REFUSED_NOT_WRITEABLE")
                        .message("write refused: no writeable layer found for scope " + scope)
                        .configName(this.configName)
                        .keyPath(fullKey)
                        .scope(scope)
                        .remediation(ConfigEvent.Remediation.ADJUST_SCOPE)
                        .remediationMessage("Try writing to a different scope or check if the configuration storage is writeable.")
                        .build();
                EventRecorder.record(event, this, ctx);
                }
            throw e;
            }
        createAndWriteEntry(newLayer, fullKey, value, type, spec);
        }

    private void createAndWriteEntry(ConfigLayerInterface layer, String fullKey, Object value, ConfigEntryType type, ConfigEntrySpecification spec)
            throws ConfigCheckedException
        {
        ConfigEntry newEntry = ConfigEntryFactory.createEntry(fullKey, value, type, this.configSchema, layer.getSource());
        if (descriptionOnCreateFlag && spec != null)
            {
            String desc = null;
            try { desc = spec.getDescription(java.util.Locale.getDefault()); } catch (Throwable t) { desc = spec.getDescription(); }
            if (desc == null || desc.isEmpty()) { desc = spec.getDescription(); }
            if (desc != null && !desc.isEmpty())
                {
                String existingComment = newEntry.getComment();
                newEntry.setComment(mergeDescriptionBeforeComment(desc, existingComment));
                }
            }
        layer.writeEntry(newEntry);
        }


    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        ConfigEntry entry = this.getConfigEntryFromFullKey(fullKey, EnumSet.allOf(ConfigScope.class));
        if (entry == null)
            {
            if (exceptionOnNullFlag)
                throw new ConfigException(ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND);
            return null;
            }
        if (entry instanceof SecretConfigEntry)
            {
            return ((SecretConfigEntry) entry).getSecretValue();
            }
        if (entry.isSecret())
            {
            try
                {
                final byte[] value;
                if (entry.getType() == ConfigEntryType.BYTES)
                    value = entry.getValueAsBytes();
                else
                    value = entry.getValueAsString().getBytes(StandardCharsets.UTF_8);

                return new BasicSecretValue(value, SecretType.PLAIN_TEXT);
                }
            catch (ConfigCheckedException e)
                {
                throw new ConfigException(e);
                }
            }
        throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE);
        }

    @Override
    public ConfigSchema getConfigSchema()
        {
        return configSchema;
        }


    @Override
    public ConfigCursor getConfigCursor()
        {
        checkClosed();
        return new ConfigCursorImpl(this);
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        return getAllConfigurationKeysFlattened(EnumSet.allOf(ConfigScope.class)).iterator();
        }

    private String mergeDescriptionBeforeComment(String description, String comment)
        {
        if (comment == null || comment.isEmpty())
            { return description; }
        if (description == null || description.isEmpty())
            { return comment; }
        // Avoid duplicate lines exactly matching either set
        java.util.LinkedHashSet<String> lines = new java.util.LinkedHashSet<>();
        for (String dline : description.split("\n", -1))
            { lines.add(dline); }
        for (String cline : comment.split("\n", -1))
            { lines.add(cline); }
        return String.join("\n", lines);
        }

    /**
     * Retrieves all configuration keys from all configuration layers,
     * combining them into a single set with keys flattened into a string format.
     * The method collects keys recursively from each configuration layer.
     *
     * @return a {@link Set} containing all flattened configuration keys
     *         across all configuration layers.
     */
    @Override
    public List<String> getListOfStrings(String fullKey)
            throws ConfigException
        {
        ConfigEntry entry = this.getConfigEntryFromFullKey(fullKey, EnumSet.allOf(ConfigScope.class));
        if (entry == null)
            {
            return null;
            }
        try
            {
            return entry.getValueAsStringList();
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException(e);
            }
        }

    /**
     * Retrieves all configuration keys from all configuration layers,
     * combining them into a single set with keys flattened into a string format.
     * The method collects keys recursively from each configuration layer.
     *
     * @return a {@link Set} containing all flattened configuration keys
     *         across all configuration layers.
     */
    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        checkClosed();
        Set<String> allKeys = new HashSet<>();
        synchronized(configs)
            {
            for (ConfigLayerInterface configLayer : configs)
                {
                if (scopes.contains(configLayer.getScope()))
                    collectKeysRecursive(configLayer, "", allKeys);
                }
            }
        return allKeys;
        }

    /**
     * Retrieves all configuration keys from all configuration layers,
     * along with their corresponding scheme entries if they exist.
     *
     * @param scopes the scopes to retrieve keys from.
     * @return a {@link Map} with flattened keys as keys and their {@link ConfigEntrySpecification} as values.
     *         Values can be null if no matching scheme entry exists.
     */
    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        {
        checkClosed();
        Set<String> allKeys = getAllConfigurationKeysFlattened(scopes);
        Map<String, ConfigEntrySpecification> result = new HashMap<>();

        for (String key : allKeys)
            {
            ConfigEntrySpecification spec = configSchema.getSpecification(key);
            if (!(spec instanceof NullConfigEntrySpecification))
                { result.put(key, spec); }
            }
        return result;
        }

    /* we may want to make something like this public: get all keys, to a set */
    private void collectKeysRecursive(ConfigLayerInterface layer, String prefix, Set<String> keys)
        {
        Iterator<String> layerKeys = layer.tryToGetKeyIterator();
        if (layerKeys != null)
            { layerKeys.forEachRemaining(keys::add); }
        }


    /**
     * get notified when the configuration changes.
     * Note: The scopes for which notifications are sent exclude RUNTIME by default.
     * You can set the scope filter globally with the parameter UPDATE_CHECK_SCOPES
     *
     * @param listener the listener to be executed upon an update.
     */
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        checkClosed();
        // Subscribe to all current layers
        synchronized(configs)
            {
            for (ConfigLayerInterface config : configs)
                { changeNotifier.subscribeToConfigLocationUpdates(config.getSource(), listener); }
            }
        // Planned: subscribe to dynamically added layers
        }

    /**
     * subscribe to updates for individual entries
     *
     * @param fullKey  full key of the configuration entry to which receive update notifications for
     * @param listener the listener to be executed upon an update.
     */
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        checkClosed();
        changeNotifier.subscribeToEntryUpdates(fullKey, listener);
        }

    /**
     * Remove subscription for all updates where this listener might be called
     *
     * @param listener listener to unsubscribe.
     */
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        checkClosed();
        changeNotifier.unsubscribeFromUpdates(listener);
        }
}

//___EOF___
