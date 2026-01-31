package org.metabit.platform.support.config.source.zookeeper;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

/**
 * ZooKeeper specific ConfigLocation.
 */
public class ZooKeeperConfigLocation extends ConfigLocationImpl
{
    private final String znodePath;

    public ZooKeeperConfigLocation(ConfigStorageInterface storage, ConfigScope scope, String znodePath)
    {
        super(scope, storage, null, znodePath);
        this.znodePath = znodePath;
    }

    @Override
    public String toLocationString()
    {
        return znodePath;
    }
}
