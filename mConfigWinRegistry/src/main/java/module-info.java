module metabit.mconfig.winregistry
    {
    requires metabit.mconfig.core;
    requires org.jnrproject.ffi;
    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface with org.metabit.platform.support.config.source.windowsregistry.WindowsRegistryConfigSource;
    }