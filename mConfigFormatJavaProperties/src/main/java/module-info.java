import org.metabit.platform.support.config.impl.format.javaproperties.FileJavaPropertiesFormat;

module metabit.mconfig.format.javaproperties
    {
    requires transitive metabit.mconfig.core;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface with FileJavaPropertiesFormat;
    }
