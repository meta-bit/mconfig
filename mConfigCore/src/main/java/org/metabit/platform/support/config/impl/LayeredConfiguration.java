package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryFactory;
import org.metabit.platform.support.config.impl.entry.SpecifiedConfigEntryWrapper;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.NullConfigEntrySpecification;
import org.metabit.platform.support.config.scheme.NullConfigScheme;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * a facade combining configuration layers into one configuration view.
 * It is the primary Configuration implementation type for mConfig.
 * 
 * @version $Id: $Id
 */
public class LayeredConfiguration extends AbstractConfiguration implements LayeredConfigurationInterface
{
    final String                         configName; // the name this goes by
    ConfigScheme                         configScheme; // the scheme the contents are to be validated by
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

    /**
     * <p>Constructor for LayeredConfiguration.</p>
     *
     * @param sanitizedConfigName a {@link java.lang.String} object
     * @param configScheme a {@link org.metabit.platform.support.config.scheme.ConfigScheme} object
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     * @param configFactory a {@link org.metabit.platform.support.config.ConfigFactory} object
     */
    public LayeredConfiguration(String sanitizedConfigName, final ConfigScheme configScheme,
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
        
        if (configScheme != null)
            {
            this.configScheme = configScheme;
            // @CHECK: who calls the scheme.init() ?
            // init defaults, if possible - transfer scheme entries to default layer
            this.setConfigScheme(configScheme);
            }
        else
            {
            this.configScheme = NullConfigScheme.INSTANCE;
            }
        configs = new ArrayList<>();
        configs.add(defaultLayer);    // needs to go at the lowest priority.
        this.events = new ConfigEventList();
        this.changeNotifier = new SourceChangeNotifier(ctx);
        this.changeChecker = new SourceChangeChecker(ctx, changeNotifier);
        this.ctx = ctx;
        return;
        }

    /*
     *   @Override
     *   public ConfigEventList getEvents()
     *       {
     *       return this.events;
     *       }
     *
     *    set config scheme from an initialized scheme.
     *   @Override
     */
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
        ConfigEntrySpecification spec = configScheme.getSpecification(fullKey);
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

                if (!configScheme.checkConfigEntryValidity(fullKey, entry))
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
    public void setConfigScheme(final ConfigScheme scheme)
        {
        if (scheme != null)
            {
            this.configScheme = scheme;
            // Cleaning handled by scheme impl (SCHEME_RESETS_DEFAULTS flag)
            scheme.transferDefaults(defaultLayer);    // defaultLayer exists by ... default.
            }
        else
            { this.configScheme = NullConfigScheme.INSTANCE; }
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
     * @param configScheme         the config scheme to apply, if any.
     * @return the instance of the created layer
     * @throws org.metabit.platform.support.config.ConfigCheckedException on failure to create any such layer
     */
    protected ConfigLayerInterface tryToCreateConfigLayer(String configName, ConfigScope scopeToCreateLayerAt, ConfigScheme configScheme)
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
            ConfigStorageInterface storage = location.getStorage();
            if (!storage.isGenerallyWriteable())
                {
                logger.debug("config creation: skipped non-writeable ["+storage.getStorageID()+"] "+storage.getStorageName());
                continue; // skip
                }

            ConfigLayerInterface instance = storage.tryToCreateConfiguration(configName, location, configScheme, this);
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
        ConfigEntrySpecification spec = (configScheme != null) ? configScheme.getSpecification(fullKey) : null;
        if (spec != null && !(spec instanceof org.metabit.platform.support.config.scheme.NullConfigEntrySpecification))
            {
            ConfigEntry entryToValidate = ConfigEntryFactory.createEntry(fullKey, value, type, this.configScheme, null);
            if (!spec.validateEntry(entryToValidate))
                {
                throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INPUT_INVALID);
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
                    ConfigEntry updatedEntry = ConfigEntryFactory.createEntry(fullKey, value, type, this.configScheme, configLayer.getSource());
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

                ConfigEntry newEntry = ConfigEntryFactory.createEntry(fullKey, value, type, this.configScheme, configLayer.getSource());
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
                configLayer.writeEntry(newEntry);
                return;
                }
            }

        // Priority 3: Create a new configuration layer instance
        ConfigLayerInterface newLayer = tryToCreateConfigLayer(this.configName, scope, this.configScheme);
        ConfigEntry newEntry = ConfigEntryFactory.createEntry(fullKey, value, type, this.configScheme, newLayer.getSource());
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
        newLayer.writeEntry(newEntry);
        }

    @Override
    public void put(String fullKey, String value, ConfigScope scope) throws ConfigException
        {
        try
            { putGeneric(fullKey, value, ConfigEntryType.STRING, scope); }
        catch (ConfigCheckedException e)
            { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope) throws ConfigException
        {
        try
            { putGeneric(fullKey, value, ConfigEntryType.BYTES, scope); }
        catch (ConfigCheckedException e)
            { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope) throws ConfigException
        {
        try
            { putGeneric(fullKey, value, ConfigEntryType.MULTIPLE_STRINGS, scope); }
        catch (ConfigCheckedException e)
            { throw new ConfigException(e); }
        }

    @Override
    public org.metabit.platform.support.config.interfaces.SecretValue getSecret(String fullKey) throws ConfigException
        {
        throw new UnsupportedOperationException("LayeredConfiguration does not support secrets directly; use ConfigFacadeImpl");
        }

    @Override
    public ConfigScheme getConfigScheme()
        {
        return configScheme;
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
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemesFlattened(EnumSet<ConfigScope> scopes)
        {
        checkClosed();
        Set<String> allKeys = getAllConfigurationKeysFlattened(scopes);
        Map<String, ConfigEntrySpecification> result = new HashMap<>();

        for (String key : allKeys)
            {
            ConfigEntrySpecification spec = configScheme.getSpecification(key);
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
