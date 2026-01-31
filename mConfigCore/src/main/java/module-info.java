import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;
import org.metabit.platform.support.config.impl.core.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.source.core.DefaultLayerStorage;
import org.metabit.platform.support.config.source.core.InMemoryLayerSource;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;


module metabit.mconfig.core
    {
    requires java.base;

    uses ConfigFactory;
    uses ConfigLoggingInterface;
    uses ConfigStorageInterface;
    uses ConfigFormatInterface;
    uses ConfigSecretsProviderInterface;
    uses org.metabit.platform.support.config.scheme.ConfigSchemeFactory;
    uses org.metabit.platform.support.config.scheme.provider.ConfigSchemeProvider;
    uses org.metabit.platform.support.config.impl.ConfigFactoryComponent;

    exports org.metabit.platform.support.config;
    exports org.metabit.platform.support.config.impl;
    exports org.metabit.platform.support.config.interfaces;
    exports org.metabit.platform.support.config.scheme;
    exports org.metabit.platform.support.config.impl.core;
    exports org.metabit.platform.support.config.impl.logging;
    exports org.metabit.platform.support.osdetection;
    exports org.metabit.platform.support.config.source.core;
    exports org.metabit.platform.support.config.impl.entry;
    exports org.metabit.platform.support.config.scheme.repository;
    exports org.metabit.platform.support.config.scheme.impl;
    exports org.metabit.platform.support.config.scheme.provider;

    provides ConfigLoggingInterface with NullLogging, ConsoleLogging;
    provides ConfigFactory with DefaultConfigFactory;
    provides ConfigStorageInterface with DefaultLayerStorage, InMemoryLayerSource;

    }
