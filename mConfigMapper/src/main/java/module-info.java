module metabit.mconfig.mapper
{
    requires transitive metabit.mconfig.core;
    requires java.logging;

    exports org.metabit.platform.support.config.mapper;

    uses org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
}
