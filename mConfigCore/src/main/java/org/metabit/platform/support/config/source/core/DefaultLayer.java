package org.metabit.platform.support.config.source.core;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>DefaultLayer class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class DefaultLayer implements ConfigLayerInterface
{
    private final Boolean flag1;
    private final Boolean                 flag2;
    private final ConfigLocationImpl      source;
    private final Map<String,ConfigEntry> map;

    /**
     * <p>Constructor for DefaultLayer.</p>
     *
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     */
    public DefaultLayer(ConfigFactoryInstanceContext ctx)
        {
        map = new HashMap<>();
        flag1 = ctx.getSettings().getBoolean(ConfigFeature.DEFAULT_ON_MISSING_ENTRY);
        flag2 = ctx.getSettings().getBoolean(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY);
        source = new ConfigLocationImpl(getScope(), new DefaultLayerStorage(), null, null); // no storage, no format
        }

    /** {@inheritDoc} */
    @Override
    public ConfigEntry getEntry(String hierarchicalKey)
        { return map.get(hierarchicalKey); }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty()
        { return map.isEmpty(); }

    /** {@inheritDoc} */
    @Override
    public ConfigScope getScope()
        {
        return ConfigScope.PRODUCT;
        }

    /** {@inheritDoc} */
    @Override
    public void writeEntry(ConfigEntry entryToWrite)
            throws ConfigCheckedException
        {  throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE); }

    /**
     * {@inheritDoc}
     *
     * not writeable, so no cache flush here.
     */
    @Override
    public int flush()
        { return 0; }

    /** {@inheritDoc} */
    @Override
    public ConfigSource getSource()
        {
        return source;
        }

    /** {@inheritDoc} */
    @Override
    public int compareTo(ConfigLayerInterface o)
        { return this.getScope().ordinal() - o.getScope().ordinal(); }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> tryToGetKeyIterator()
        { return map.keySet().iterator(); }

    /**
     * <p>hasChangedSincePreviousCheck.</p>
     *
     * @return a boolean
     */
    public boolean hasChangedSincePreviousCheck()
        { return false; } // the defaults don't change at runtime. They're defaults.

    /** {@inheritDoc} */
    @Override
    public boolean isWriteable() { return false; } // read-only defaults

    /**
     * <p>putEntry.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param preparedEntry a {@link org.metabit.platform.support.config.ConfigEntry} object
     */
    public void putEntry(final String fullKey, final ConfigEntry preparedEntry) {  map.put(fullKey,preparedEntry); }

    /**
     * <p>clear.</p>
     */
    public void clear()
        { map.clear(); }
}
