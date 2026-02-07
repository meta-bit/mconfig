import org.metabit.platform.support.config.impl.format.toml.TOMLFileFormat;

module metabit.mconfig.format.toml
    {
    requires transitive metabit.mconfig.core;

    exports org.metabit.platform.support.config.impl.format.toml;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with TOMLFileFormat;
    }
