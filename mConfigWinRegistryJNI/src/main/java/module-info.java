module metabit.mconfig.winregistry.jni
    {
    requires metabit.mconfig.core;
    exports org.metabit.platform.support.config.source.windowsregistry.jni;
    uses org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface with org.metabit.platform.support.config.source.windowsregistry.jni.WindowsRegistryJNISource;
    }