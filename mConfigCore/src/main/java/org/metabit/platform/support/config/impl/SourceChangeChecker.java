package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p>SourceChangeChecker class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class SourceChangeChecker implements Runnable
{
    private final ConfigLoggingInterface     logger;
    private final ScheduledExecutorService   executor;
    private final SourceChangeNotifier       notifier;
    private       long                       nextCheck;
    private       long                       delta;
    private       List<ConfigLayerInterface> localConfigList;
    private       EnumSet<ConfigScope>       checkedScopes;

    private final EntryChangeChecker         entryChangeChecker;

    /**
     * <p>Constructor for SourceChangeChecker.</p>
     *
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     * @param changeNotifier a {@link org.metabit.platform.support.config.impl.SourceChangeNotifier} object
     */
    public SourceChangeChecker(ConfigFactoryInstanceContext ctx, SourceChangeNotifier changeNotifier)
        {
        this.logger = ctx.getLogger();
        this.localConfigList = new ArrayList<>();
        this.entryChangeChecker = new EntryChangeChecker();
        this.delta = ctx.getSettings().getInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS);
        if (delta < 0) // sanitize
            delta = 0;
        // this.nextCheck = System.currentTimeMillis()+delta;
        // we're using a threaded approach instead of sync calls.
        this.executor = Executors.newSingleThreadScheduledExecutor(); // JDK1.5
        if (delta > 0) // delta 0 means: off
            { this.executor.scheduleAtFixedRate(this, delta, delta, TimeUnit.MILLISECONDS); } // JDK1.5
        else
            { logger.info("automatic update checks were turned off"); }
        this.notifier = changeNotifier;
        // scopes: convert UPDATE_CHECK_SCOPES to EnumSet.
        checkedScopes = EnumSet.noneOf(ConfigScope.class);
        List<String> scopesAsStrings = ctx.getSettings().getStrings(ConfigFeature.UPDATE_CHECK_SCOPES);
        try
            {
            scopesAsStrings.forEach(s->checkedScopes.add(ConfigScope.valueOf(s)));
            }
        catch (IllegalArgumentException ex)
            {
            logger.error("invalid scope for UPDATE_CHECK_SCOPES: " + ex.getMessage());
            // considered a severe error because the behaviour would be significantly different from what's intended
            throw new ConfigException(ConfigException.ConfigExceptionReason.CONFIG_FEATURE_VALUE_INVALID);
            }
        return;
        }

    // or cleanup? destructor, really
    /**
     * <p>exit.</p>
     */
    public void exit()
        {
        this.executor.shutdown(); // no new ones started
        //JDK17 this.taskHandle.cancel(false); // stop the existing one if it is running, but allow completiong
        return;
        }

    public void updateConfigList(List<ConfigLayerInterface> configs)
        {
        synchronized(localConfigList)
            {
            // get a local copy, to avoid synchronizing the input parameter one.
            // this.localConfigList = new ArrayList<ConfigLayerInterface>(configs);
            this.localConfigList = Collections.unmodifiableList(new ArrayList<>(configs));
            // NB: this is a shallow copy; its elements will likely be the same instances.
            // But as long as they are used read-only, this should be no issue.
            // also consider an CopyOnWriteArrayList from java.util.concurrent JDK1.5
            }
        return;
        }

    // the action the timer task performs.
    /** {@inheritDoc} */
    @Override
    public void run()
        {
        checkAndNotify(this.localConfigList, this.notifier);
        }

    public void checkAndNotify(List<ConfigLayerInterface> configs, SourceChangeNotifier changeNotifier)
        {
        // get a stable copy of the configs to iterate over
        List<ConfigLayerInterface> stableConfigs;
        synchronized(localConfigList)
            {
            stableConfigs = new ArrayList<>(configs);
            }

        // go through the configs, ask for changes.
        for (int i = stableConfigs.size() - 1; i >= 0; i--)
            {
            ConfigLayerInterface configLayer = stableConfigs.get(i);
            if (!checkedScopes.contains(configLayer.getScope()))
                {
                continue; // skip scopes outside of… scope.
                }
            
            if (configLayer.getSource().hasChangedSincePreviousCheck())
                {
                System.out.println("[DEBUG_LOG] SourceChangeChecker: CHANGE detected in config layer " + configLayer);
                logger.info("CHANGE detected in config layer " + configLayer);
                
                // if we have an iterator, check for individual entry changes
                Iterator<String> keyIter = configLayer.tryToGetKeyIterator();
                if (keyIter != null)
                    {
                    boolean entryChanged = false;
                    while (keyIter.hasNext())
                        {
                        String key = keyIter.next();
                        ConfigEntry entry = configLayer.getEntry(key);
                        if (entryChangeChecker.hasChanged(entry))
                            {
                            entryChanged = true;
                            // trigger specific entry notification
                            changeNotifier.sendNotificationsAboutChangeInEntry(entry);
                            }
                        }
                    // trigger notification for the location if any entry changed
                    if (entryChanged)
                        {
                        changeNotifier.sendNotificationsAboutChangeInConfigLocation(configLayer.getSource());
                        }
                    }
                else
                    {
                    // source says it changed, but we can't check entries (non-iterable)
                    // trigger notification for the whole location
                    changeNotifier.sendNotificationsAboutChangeInConfigLocation(configLayer.getSource());
                    }
                }
            }
        return;
        }
    //------------------------------------------------------------------------------------------------------------------


    /**
     * <p>considerCheck.</p>
     *
     * @param now a long
     * @return a boolean
     */
    public boolean considerCheck(long now)
        {
// using a thread instead in the future.
        if (now < nextCheck)
            return false;
        // else
        this.nextCheck = now+delta;
        return true;
        }
}
//___EOF___
