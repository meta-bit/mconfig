import org.metabit.platform.support.config.impl.format.ini.INIFileFormat;

module metabit.mconfig.format.ini
    {
    requires transitive metabit.mconfig.core;

    exports org.metabit.platform.support.config.impl.format.ini;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with INIFileFormat;
    }
