package org.metabit.platform.support.config.source.zookeeper;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;

/**
 * ZooKeeper specific ConfigSource.
 */
public class ZooKeeperConfigSource extends ConfigLocationImpl
{
    private Object manualHandle;

    public ZooKeeperConfigSource(ConfigLocation location, ConfigFormatInterface format)
    {
        super(location.getScope(), location.getStorage(), format, location.getStorageInstanceHandle());
    }

    public ZooKeeperConfigSource(ConfigLocation location, ConfigFormatInterface format, Object handle)
    {
        super(location.getScope(), location.getStorage(), format, handle);
        this.manualHandle = handle;
    }

    @Override
    public Object getStorageInstanceHandle()
    {
        if (manualHandle != null) return manualHandle;
        return super.getStorageInstanceHandle();
    }
}
