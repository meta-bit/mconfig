import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.logging.Slf4j2Logger;

module metabit.mconfig.modules.mConfigLoggingSlf4j
    {
    uses ConfigLoggingInterface;
    exports org.metabit.platform.support.config.logging;
    provides ConfigLoggingInterface with Slf4j2Logger;

    requires metabit.mconfig.core;
    requires org.slf4j;
    }