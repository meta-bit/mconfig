package org.metabit.platform.support.config.util.loggers;


import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

/**
 * logging service instance which uses the JDK9 java.lang.System.Logger
 * <p>
 * requires RuntimePermission("loggerFinder")
 */
public class JavaSystemLogger implements ConfigLoggingInterface
    {
    private int           logLevel;
    private System.Logger logger;

    @Override
    public String getServiceModuleName()
        { return "java.System.Logger"; }

    @Override
    public int getServiceModulePriority()
        { return 2; }

    @Override
    public boolean init(final ConfigFactorySettings settings)
        {
        logger = System.getLogger(""); //@IMPROVEMENT use input from settings
        return true;
        }

    @Override
    public void exit()
        { }

    final static String PREFIX_ERROR   = "[ERROR] "; // add red colour
    final static String PREFIX_WARNING = "[WARN] "; // add yellow colour
    final static String PREFIX_INFO    = "[INFO] "; // add white colour
    final static String PREFIX_DEBUG   = "[DEBUG] "; // add (light) blue colour
    final static String PREFIX_TRACE   = "[TRACE] "; // add grey colour

    @Override
    public void error(String msg)
        { logger.log(System.Logger.Level.ERROR, msg); }

    @Override
    public void warn(String msg)
        { logger.log(System.Logger.Level.WARNING, msg); }

    @Override
    public void info(String msg)
        { logger.log(System.Logger.Level.INFO, msg); }

    @Override
    public void debug(String msg)
        { logger.log(System.Logger.Level.DEBUG, msg); }

    @Override
    public void trace(String msg)
        { logger.log(System.Logger.Level.TRACE, msg); }

    @Override
    public void error(String msg, Throwable t)
        { logger.log(System.Logger.Level.ERROR, msg, t); }

    @Override
    public void warn(String msg, Throwable t)
        { logger.log(System.Logger.Level.WARNING, msg, t); }

    @Override
    public void info(String msg, Throwable t)
        { logger.log(System.Logger.Level.INFO, msg, t); }

    @Override
    public void debug(String msg, Throwable t)
        { logger.log(System.Logger.Level.DEBUG, msg, t); }

    @Override
    public void trace(String msg, Throwable t)
        { logger.log(System.Logger.Level.TRACE, msg, t); }

    @Override
    public void error(String format, Object... arguments)
        { logger.log(System.Logger.Level.ERROR, format, arguments); }

    @Override
    public void warn(String format, Object... arguments)
        { logger.log(System.Logger.Level.WARNING, format, arguments); }

    @Override
    public void info(String format, Object... arguments)
        { logger.log(System.Logger.Level.INFO, format, arguments); }

    @Override
    public void debug(String format, Object... arguments)
        { logger.log(System.Logger.Level.DEBUG, format, arguments); }

    @Override
    public void trace(String format, Object... arguments)
        { logger.log(System.Logger.Level.TRACE, format, arguments); }

    @Override
    public boolean isErrorEnabled()
        { return (logLevel > 0); }

    @Override
    public boolean isWarnEnabled()
        { return (logLevel > 1); }

    @Override
    public boolean isInfoEnabled()
        { return (logLevel > 2); }

    @Override
    public boolean isDebugEnabled()
        { return (logLevel > 3); }

    @Override
    public boolean isTraceEnabled()
        { return (logLevel > 4); }

    public JavaSystemLogger()
        { logger = System.getLogger(""); } // default fallback

    public int getNumericalLogLevel()
        { return logLevel; }

    public void setNumericalLogLevel(final int logLevel)
        {
        if ((logLevel < 0) || (logLevel > 5))
            throw new IllegalArgumentException();
        this.logLevel = logLevel;
        }
    }
