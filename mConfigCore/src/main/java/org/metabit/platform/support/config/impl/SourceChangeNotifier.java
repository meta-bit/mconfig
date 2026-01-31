package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Refactored to use Consumer for JDK 8+ idiomatic approach.
 *
 * Since the check is synchronous to the caller, the decoupling should be on the notification.
 *
 * 
 * @version $Id: $Id
 */
public class SourceChangeNotifier
{
    private final ExecutorService                                    notifierExec;
    private final ScheduledExecutorService                           scheduler;
    private final Map<ConfigLocation, Set<Consumer<ConfigLocation>>> locationMap;
    private final Map<String, Set<Consumer<ConfigLocation>>>         entryMap;
    private final Map<ConfigLocation, ScheduledFuture<?>>            pendingNotifications;
    private final int                                                debounceDelayMs;

    SourceChangeNotifier(ConfigFactoryInstanceContext ctx)
        {
        notifierExec = Executors.newWorkStealingPool();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "mConfig-Notifier-Scheduler");
            t.setDaemon(true);
            return t;
        });
        locationMap = new HashMap<>();
        entryMap = new HashMap<>();
        pendingNotifications = new HashMap<>();
        Integer freq = ctx.getSettings().getInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS);
        if (freq == null || freq <= 0) freq = 50;
        // debounce delay should be small but effective. 
        // using 1/2 of frequency is a good guess, but capped at 10-100ms.
        debounceDelayMs = Math.max(10, Math.min(100, freq / 2)); 
        // We use half of the update frequency as default, but at least some small value if it's very small.
        }

    void exit()
        {
        notifierExec.shutdown();
        scheduler.shutdown();
        }

    public void subscribeToConfigLocationUpdates(ConfigLocation location, Consumer<ConfigLocation> listener)
        {
        synchronized(locationMap)
            {
            if (!locationMap.containsKey(location))
                {
                locationMap.put(location, new HashSet<>()); // Set instead of List, so duplicates are avoided implicitly
                }
            locationMap.get(location).add(listener); // if List is much more efficient, we could check here for duplicates instead
            }
        return;
        }

    void subscribeToEntryUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        synchronized(entryMap)
            {
            if (!entryMap.containsKey(fullKey))
                {
                entryMap.put(fullKey, new HashSet<>());
                }
            entryMap.get(fullKey).add(listener);
            }
        return;
        }

    void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        // iterate through all entries, remove matches.
        synchronized(locationMap)
            {
            locationMap.values().forEach(set->set.remove(listener));
            }
        synchronized(entryMap)
            {
            entryMap.values().forEach(set->set.remove(listener));
            }
        return;
        }

    public void sendNotificationsAboutChangeInConfigLocation(ConfigLocation changedLocation)
        {
        synchronized(pendingNotifications)
            {
            ScheduledFuture<?> existing = pendingNotifications.get(changedLocation);
            if (existing != null)
                {
                existing.cancel(false);
                }
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                synchronized(pendingNotifications)
                    {
                    pendingNotifications.remove(changedLocation);
                    }
                executeLocationNotifications(changedLocation);
            }, debounceDelayMs, TimeUnit.MILLISECONDS);
            pendingNotifications.put(changedLocation, future);
            }
        }

    private void executeLocationNotifications(ConfigLocation changedLocation)
        {
        Set<Consumer<ConfigLocation>> configLocationSubscribers;
        // map the location to the Configuration
        synchronized(locationMap)
            {
            configLocationSubscribers = locationMap.get(changedLocation);
            if (configLocationSubscribers == null || configLocationSubscribers.isEmpty()) // no-one requested updates about this. normal case.
                return;
            configLocationSubscribers = new HashSet<>(configLocationSubscribers);
            }
        // send the notification
        configLocationSubscribers.forEach(listener -> notifierExec.submit(() -> listener.accept(changedLocation)));
        // optional: track the notifications by storing and using the Future objects returned
        }

    void sendNotificationsAboutChangeInEntry(ConfigEntry entry)
        {
        Set<Consumer<ConfigLocation>> entrySubscribers;
        String fullKey = entry.getKey();
        ConfigLocation location = entry.getLocation();
        
        synchronized(entryMap)
            {
            entrySubscribers = entryMap.get(fullKey);
            if (entrySubscribers == null || entrySubscribers.isEmpty())
                return;
            entrySubscribers = new HashSet<>(entrySubscribers);
            }
        
        entrySubscribers.forEach(listener -> notifierExec.submit(() -> listener.accept(location)));
        }
}
