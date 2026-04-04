package org.metabit.platform.support.config.impl;

/*
  not sure yet this is permanent; but checking entries for changes is different from checking sources.
  when it is triggered, we have to compare the previous *value* / entry contents against the new ones,
  and only if they differ, that merits a notification.

  The problem that will not have a perfect solution is matching contents; but we'd rather err
  slightly on the over-notify side. The notification was actively requested, so...

  If whitespace was to something which is read as Integer, that may mean a
  notification is triggered, even though the Integer value itself wasn't changed.
  This is not intended behaviour, though, and may disappear by code improvements.


  ------
  Implementation-wise: looks like we have to cache the previous entries,
  and then compare with the new instance - maybe trigger notification,
  but always replacing the entry object on the check with the new one.
 */
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks entries for changes by comparing previous contents.
 *
 * @version $Id: $Id
 */
public class EntryChangeChecker
{
    private final Map<String, Object> entryCache = new ConcurrentHashMap<>();

    /**
     * Checks if the entry has changed since the last check.
     *
     * @param entry the entry to check
     * @return true if changed, false otherwise
     */
    public boolean hasChanged(ConfigEntry entry)
        {
        if (entry == null) return false;
        String key = entry.getKey() + "@" + entry.getLocation().toLocationString();
        Object newValue = getEntryValue(entry);
        Object oldValue = entryCache.get(key);

        if (oldValue == null)
            {
            entryCache.put(key, newValue);
            return newValue != null; // Transition from null to value IS considered a change
            }

        boolean changed;
        if (newValue instanceof byte[])
            {
            changed = !Arrays.equals((byte[]) oldValue, (byte[]) newValue);
            }
        else
            {
            changed = !oldValue.equals(newValue);
            }

        if (changed)
            {
            entryCache.put(key, newValue);
            }
        return changed;
        }

    private Object getEntryValue(ConfigEntry entry)
        {
        try
            {
            if (entry.getType() == ConfigEntryType.BYTES)
                {
                return entry.getValueAsBytes();
                }
            return entry.getValueAsString();
            }
        catch (ConfigCheckedException e)
            {
            return null;
            }
        }
}
