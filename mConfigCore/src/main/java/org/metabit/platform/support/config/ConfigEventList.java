package org.metabit.platform.support.config;

import org.metabit.platform.support.config.impl.ConfigEventListImpl;

/**
 * the list of {ConfigEvents} which occurred,
 * related to a specific configuration, or the {ConfigFactory}.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigEventList
        extends ConfigEventListImpl
{
    public ConfigEventList(int maxSize)
        {
        super(maxSize);
        }

    public ConfigEventList(int maxSize, int dedupLimit)
        {
        super(maxSize, dedupLimit);
        }

    /**
     * clear all events from the list.
     */
    public void clean()
        {
        this.clear();
        }
}
