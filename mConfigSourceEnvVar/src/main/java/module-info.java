module metabit.mconfig.modules.envvar
    {
    requires metabit.mconfig.core;
    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface with org.metabit.platform.support.config.source.envvar.EnvVarConfigStorage;
    }