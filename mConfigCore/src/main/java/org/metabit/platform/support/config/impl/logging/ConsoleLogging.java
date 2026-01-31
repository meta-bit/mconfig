package org.metabit.platform.support.config.impl.logging;

import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.PrintStream;

/**
 * Console logging implementation for development and testing.
 * Outputs to stdout/stderr. Opt-in via `ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB = "console"`.
 * Not intended for production use; prefer SLF4J or NullLogging.
 */
public class ConsoleLogging implements ConfigLoggingInterface
{
    private enum Level
        {
        TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);
        final int value;
        Level(int value) { this.value = value; }
        }

    private Level currentLevel = Level.INFO;
    private String moduleName = "mConfig";

    public ConsoleLogging()
        { }

    public ConsoleLogging(String moduleName)
        {
        this.moduleName = moduleName;
        }

    public void setLevel(String levelName)
        {
        try
            { this.currentLevel = Level.valueOf(levelName.toUpperCase()); }
        catch (IllegalArgumentException ignore)
            { } // ignore or default
        }

    @Override
    public String getServiceModuleName()
        {
        return "console";
        }

    @Override
    public int getServiceModulePriority()
        {
        return 1;
        }

    @Override
    public boolean init(ConfigFactorySettings settings)
        {
        return true;
        }

    @Override
    public void exit()
        { }

    private void log(Level level, String msg, Throwable t, Object... args)
        {
        if (level.value < currentLevel.value)
            { return; }

        PrintStream out = (level.value >= Level.WARN.value) ? System.err : System.out;
        String formattedMsg = msg;
        if (args != null && args.length > 0)
            {
            // Simple string replacement for {} if any
            for (Object arg : args)
                {
                formattedMsg = formattedMsg.replaceFirst("\\{}", String.valueOf(arg));
                }
            }

        out.printf("[%s] [%s] %s%n", level.name(), moduleName, formattedMsg);
        if (t != null)
            { t.printStackTrace(out);  }
        }

    @Override public void error(String msg) { log(Level.ERROR, msg, null); }
    @Override public void error(String msg, Throwable t) { log(Level.ERROR, msg, t); }
    @Override public void error(String format, Object... arguments) { log(Level.ERROR, format, null, arguments); }

    @Override public void warn(String msg) { log(Level.WARN, msg, null); }
    @Override public void warn(String msg, Throwable t) { log(Level.WARN, msg, t); }
    @Override public void warn(String format, Object... arguments) { log(Level.WARN, format, null, arguments); }

    @Override public void info(String msg) { log(Level.INFO, msg, null); }
    @Override public void info(String msg, Throwable t) { log(Level.INFO, msg, t); }
    @Override public void info(String format, Object... arguments) { log(Level.INFO, format, null, arguments); }

    @Override public void debug(String msg) { log(Level.DEBUG, msg, null); }
    @Override public void debug(String msg, Throwable t) { log(Level.DEBUG, msg, t); }
    @Override public void debug(String format, Object... arguments) { log(Level.DEBUG, format, null, arguments); }

    @Override public void trace(String msg) { log(Level.TRACE, msg, null); }
    @Override public void trace(String msg, Throwable t) { log(Level.TRACE, msg, t); }
    @Override public void trace(String format, Object... arguments) { log(Level.TRACE, format, null, arguments); }

    @Override public boolean isErrorEnabled() { return currentLevel.value <= Level.ERROR.value; }
    @Override public boolean isWarnEnabled() { return currentLevel.value <= Level.WARN.value; }
    @Override public boolean isInfoEnabled() { return currentLevel.value <= Level.INFO.value; }
    @Override public boolean isDebugEnabled() { return currentLevel.value <= Level.DEBUG.value; }
    @Override public boolean isTraceEnabled() { return currentLevel.value <= Level.TRACE.value; }
}