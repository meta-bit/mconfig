import org.metabit.platform.support.config.interfaces.ConfigFactoryComponent;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.schema.provider.ConfigSchemaProvider;
import org.metabit.platform.support.config.source.hardcoded.JARConfigSource;
import org.metabit.platform.support.config.source.hardcoded.provider.ClasspathConfigSchemaProvider;
import org.metabit.platform.support.config.source.hardcoded.provider.SelfConfigurationComponent;

module metabit.mconfig.modules.jar
    {
    uses ConfigFormatInterface;
    uses ConfigFileFormatInterface;
    uses ConfigLayerInterface;

    exports org.metabit.platform.support.config.source.hardcoded;
    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface with JARConfigSource;
    provides ConfigSchemaProvider with ClasspathConfigSchemaProvider;
    provides ConfigFactoryComponent with SelfConfigurationComponent;
    requires metabit.mconfig.core;
    uses ConfigFactoryComponent;
    }