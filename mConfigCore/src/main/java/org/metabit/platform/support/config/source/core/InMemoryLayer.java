package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.util.HashMap;
import java.util.Iterator;

///
/// InMemoryLayer class.
/// provides an ConfigLayer for in-memory storage.
///
/// @version $Id: $Id
public class InMemoryLayer implements ConfigLayerInterface
{
    private final HashMap<String, ConfigEntry> map;
    private final ConfigScope                  scope;
    private final ConfigSource                 source;
    private final ConfigLoggingInterface       logger;
    private final Object lock;

    public InMemoryLayer(ConfigFactoryInstanceContext ctx, ConfigSource source, ConfigScope scopeToUse)
        {
        this.source = source;
        if (source instanceof ConfigLocationImpl)
            {
            ((ConfigLocationImpl) source).setLayer(this);
            }
        scope = scopeToUse;
        map = new HashMap<>();
        logger = ctx.getLogger();
        lock = map; // using the map itself as the lock, as it is thread-safe.
        }

    public InMemoryLayer(ConfigFactoryInstanceContext ctx, ConfigLocation location, ConfigScope scopeToUse)
        {
        this.source = new ConfigLocationImpl(location, this, null, location.getStorageInstanceHandle());
        scope = scopeToUse;
        map = new HashMap<>();
        logger = ctx.getLogger();
        lock = map; // using the map itself as the lock, as it is thread-safe.
        }

    @Override
    public ConfigEntry getEntry(String hierarchicalKey)
        {
        synchronized(lock)
            {
            return map.get(hierarchicalKey);
            }
        }

    @Override
    public boolean isEmpty()
        {
        synchronized(lock)
            {
            return map.isEmpty();
            }
        }

    @Override
    public ConfigScope getScope()
        { return scope; }

    @Override
    public void writeEntry(ConfigEntry entryToWrite)
            throws ConfigCheckedException
        {
        synchronized(lock)
            {
            if (entryToWrite == null)
                {
                logger.error("memory storage write for null entry");
                throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INPUT_INVALID);
                }
            this.map.put(entryToWrite.getKey(), entryToWrite);
            }
        }

    @Override
    public int flush()
        {
        synchronized(lock)
            {
            map.clear();
            logger.debug("inMemory layer: cleared");
            return 0;
            }
        }

    @Override
    public ConfigSource getSource()
        { return source; }

    @Override
    public int compareTo(ConfigLayerInterface o)
        { return this.getScope().compareTo(o.getScope()); }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        synchronized(lock)
            {
            return new HashMap<>(map).keySet().iterator();
            }
        }

    @Override
    public boolean isWriteable() { return true; }

    public void putEntry(final String fullKey, final ConfigEntry preparedEntry)
        {
        synchronized(lock)
            {
            map.put(fullKey, preparedEntry);
            }
        }

    public void clear()
        {
        synchronized(lock)
            {
            map.clear();
            }
        }
}
