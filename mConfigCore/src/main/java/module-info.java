import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.schema.ConfigSchemaFactory;
import org.metabit.platform.support.config.schema.provider.ConfigSchemaProvider;
import org.metabit.platform.support.config.source.core.DefaultLayerStorage;
import org.metabit.platform.support.config.source.core.RuntimeInMemoryLayerSource;
import org.metabit.platform.support.config.source.core.SessionInMemoryLayerSource;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;


module metabit.mconfig.core
    {
    requires java.base;

    uses ConfigFactory;
    uses ConfigLoggingInterface;
    uses ConfigStorageInterface;
    uses ConfigFormatInterface;
    uses ConfigSecretsProviderInterface;
    uses ConfigSchemaFactory;
    uses ConfigSchemaProvider;
    uses ConfigFactoryComponent;

    exports org.metabit.platform.support.config;
    exports org.metabit.platform.support.config.interfaces;
    exports org.metabit.platform.support.config.schema;
    exports org.metabit.platform.support.osdetection;
    exports org.metabit.platform.support.config.source.core;
    exports org.metabit.platform.support.config.impl.logging;
    exports org.metabit.platform.support.config.impl.entry;
    exports org.metabit.platform.support.config.impl;
    exports org.metabit.platform.support.config.schema.repository;
    exports org.metabit.platform.support.config.schema.impl;
    exports org.metabit.platform.support.config.schema.provider;

    provides ConfigLoggingInterface with NullLogging, ConsoleLogging;
    provides ConfigFactory with DefaultConfigFactory;
    provides ConfigStorageInterface with DefaultLayerStorage, RuntimeInMemoryLayerSource, SessionInMemoryLayerSource;

    }
