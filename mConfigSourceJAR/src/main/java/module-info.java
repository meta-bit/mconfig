import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.source.hardcoded.JARConfigSource;

module metabit.mconfig.modules.jar
    {
    uses ConfigFormatInterface;
    uses ConfigFileFormatInterface;
    uses ConfigLayerInterface;

    exports org.metabit.platform.support.config.source.hardcoded;
    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface with JARConfigSource;
    provides org.metabit.platform.support.config.scheme.provider.ConfigSchemeProvider with org.metabit.platform.support.config.source.hardcoded.provider.ClasspathConfigSchemeProvider;
    provides org.metabit.platform.support.config.impl.ConfigFactoryComponent with org.metabit.platform.support.config.source.hardcoded.provider.SelfConfigurationComponent;
    requires metabit.mconfig.core;
    uses org.metabit.platform.support.config.impl.ConfigFactoryComponent;
    }