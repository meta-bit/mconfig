package org.metabit.platform.support.config.logging;

import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * mConfig logging service instance which uses the SLF4J library.
 * - requires RuntimePermission("loggerFinder")
 * - adds dependency on slf4j
 * - SLF4j Marker associated is "CONFIGURATION".
 *
 * @see <a href="https://www.slf4j.org/">SLF4J</a>
 */
public class Slf4j2Logger implements ConfigLoggingInterface
{
    private Logger logger;
    private Marker marker; //@TODO @CHECK whether this is the right use, and useful - remove if it is not.
    // the orthogonal aspect we could sensibly add would be those cases where we *could* throw an exception, but still have a default fallback.

    public String getServiceModuleName()
        {
        return "slf4j";
        }

    public int getServiceModulePriority()
        {
        return 5;
        }

    @Override
    public boolean init(final ConfigFactorySettings settings)
        {
        logger = LoggerFactory.getLogger("mConfig");
        // marker = MarkerFactory.getMarker("CONFIG"); // or CONFIGURATION -- keep marker NULL until we figure out the best use for it.
        return true;
        }

    @Override
    public void exit()
        {
        }

    public void error(String msg) { logger.error(marker,msg); }

    public void warn(String msg) { logger.warn(marker, msg); }

    public void info(String msg) { logger.info(marker, msg); }

    public void debug(String msg) { logger.debug(marker, msg); }

    public void trace(String msg) { logger.trace(marker, msg); }

    public void error(String msg, Throwable t) { logger.error(marker, msg, t); }

    public void warn(String msg, Throwable t) { logger.warn(marker, msg, t); }

    public void info(String msg, Throwable t) { logger.info(marker, msg, t); }

    public void debug(String msg, Throwable t) { logger.debug(marker, msg, t); }

    public void trace(String msg, Throwable t) { logger.trace(marker, msg, t); }

    public void error(String format, Object... arguments) { logger.error(marker, format, arguments); }

    public void warn(String format, Object... arguments) { logger.warn(marker, format, arguments); }

    public void info(String format, Object... arguments) { logger.info(marker, format, arguments); }

    public void debug(String format, Object... arguments) { logger.debug(marker, format, arguments); }

    public void trace(String format, Object... arguments) { logger.trace(marker, format, arguments); }

    public boolean isErrorEnabled() { return (logger.isErrorEnabled(marker)); }

    public boolean isWarnEnabled()
        {
        return (logger.isWarnEnabled(marker));
        }

    public boolean isInfoEnabled() { return logger.isInfoEnabled(marker); }

    public boolean isDebugEnabled()  { return logger.isDebugEnabled(marker); }

    public boolean isTraceEnabled()
        {
        return logger.isTraceEnabled(marker);
        }

    public Slf4j2Logger()
        {
        }

}
