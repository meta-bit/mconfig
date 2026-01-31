import org.metabit.platform.support.config.impl.source.filesystem.FileConfigStorage;

module metabit.mconfig.modules.filesystem
    {
    requires transitive metabit.mconfig.core;
    requires java.logging;

    uses org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface with FileConfigStorage;
    }