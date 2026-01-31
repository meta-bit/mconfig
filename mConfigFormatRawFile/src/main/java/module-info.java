module metabit.mconfig.modules.format.raw
    {
    requires transitive metabit.mconfig.core;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface with org.metabit.platform.support.config.impl.format.raw.FileRawBinaryFormat, org.metabit.platform.support.config.impl.format.raw.FileRawTextFormat;
    }
