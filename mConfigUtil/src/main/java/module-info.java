import org.metabit.platform.support.config.util.loggers.StdErrLogger;
import org.metabit.platform.support.config.util.loggers.JavaSystemLogger;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

module metabit.mconfig.util
    {
    requires transitive metabit.mconfig.core;
    requires java.logging;

    exports org.metabit.platform.support.config.util;

    uses ConfigLoggingInterface;

    provides ConfigLoggingInterface with StdErrLogger, JavaSystemLogger;
    }