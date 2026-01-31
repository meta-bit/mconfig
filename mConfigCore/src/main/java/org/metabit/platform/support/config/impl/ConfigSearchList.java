package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulated collection of ConfigLocation entries.
 *
 * @version $Id: $Id
 */
public class ConfigSearchList
    {
    private final List<ConfigLocation> entries = new ArrayList<>();

    public List<ConfigLocation> getEntries()
        {
        return entries;
        }

    public boolean isEmpty()
        {
        return entries.isEmpty();
        }

    public int size()
        {
        return entries.size();
        }

    /**
     * Inserts an entry at the beginning of its scope's section in the search list.
     * This ensures that the newly added entry takes precedence over any existing
     * entries within the same scope (Last-In-First-Win).
     *
     * @param entry the {@link org.metabit.platform.support.config.ConfigLocation} to add
     * @param scope the {@link org.metabit.platform.support.config.ConfigScope} of the entry
     */
    public synchronized void insertAtScopeStart(final ConfigLocation entry, final ConfigScope scope)
        {
        int max = entries.size();
        for (int i = 0; i < max; i++)
            {
            if (entries.get(i).getScope().ordinal() <= scope.ordinal())
                {
                entries.add(i, entry);
                return;
                }
            }
        entries.add(entry);
        }

    /**
     * Inserts an entry at the end of its scope's section in the search list.
     * This means the newly added entry will have lower precedence than any existing
     * entries within the same scope (First-In-First-Win).
     *
     * @param entry the {@link org.metabit.platform.support.config.ConfigLocation} to add
     * @param scope the {@link org.metabit.platform.support.config.ConfigScope} of the entry
     */
    public synchronized void insertAtScopeEnd(final ConfigLocation entry, final ConfigScope scope)
        {
        int max = entries.size();
        for (int i = 0; i < max; i++)
            {
            if (entries.get(i).getScope().ordinal() < scope.ordinal())
                {
                entries.add(i, entry);
                return;
                }
            }
        entries.add(entry);
        }
    }
//___EOF___
