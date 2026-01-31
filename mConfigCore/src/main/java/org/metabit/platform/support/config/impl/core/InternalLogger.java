package org.metabit.platform.support.config.impl.core;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Internal logger for mConfig that can redirect to stdout, stderr, or a file.
 */
public class InternalLogger implements ConfigLoggingInterface
{
    private int logLevel;
    private final String target;
    private OutputStream out;
    private PrintWriter writer;
    private boolean shouldClose;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public InternalLogger(String target)
    {
        this.target = target;
        this.logLevel = 2; // Default to warn
    }

    @Override
    public String getServiceModuleName()
    {
        return "universal-logger";
    }

    @Override
    public int getServiceModulePriority()
    {
        return 100; // High priority when explicitly requested
    }

    @Override
    public boolean init(ConfigFactorySettings settings)
    {
        this.logLevel = settings.getInteger(ConfigFeature.LOGLEVEL_NUMBER);
        
        try
        {
            if ("stdout".equalsIgnoreCase(target))
            {
                out = System.out;
                shouldClose = false;
            }
            else if ("stderr".equalsIgnoreCase(target))
            {
                out = System.err;
                shouldClose = false;
            }
            else if ("quiet".equalsIgnoreCase(target))
            {
                logLevel = 0;
                out = new OutputStream() { @Override public void write(int b) {} };
                shouldClose = false;
            }
            else if (target.startsWith("file:"))
            {
                String filename = target.substring(5);
                out = new FileOutputStream(filename, true);
                shouldClose = true;
            }
            else
            {
                // Fallback to stderr
                out = System.err;
                shouldClose = false;
            }
            writer = new PrintWriter(out, true);
        }
        catch (IOException e)
        {
            // Silent failure as per README
            return false;
        }
        return true;
    }

    @Override
    public void exit()
    {
        if (shouldClose && out != null)
        {
            try { out.close(); } catch (IOException ignored) {}
        }
    }

    private void log(String level, String msg, Throwable t)
    {
        if (writer == null) return;
        String timestamp = LocalDateTime.now().format(dtf);
        writer.printf("%s [%s] %s%n", timestamp, level, msg);
        if (t != null)
        {
            t.printStackTrace(writer);
        }
    }

    @Override public void error(String msg) { if (isErrorEnabled()) log("ERROR", msg, null); }
    @Override public void error(String msg, Throwable t) { if (isErrorEnabled()) log("ERROR", msg, t); }
    @Override public void error(String format, Object... args) { if (isErrorEnabled()) log("ERROR", String.format(format, args), null); }
    
    @Override public void warn(String msg) { if (isWarnEnabled()) log("WARN", msg, null); }
    @Override public void warn(String msg, Throwable t) { if (isWarnEnabled()) log("WARN", msg, t); }
    @Override public void warn(String format, Object... args) { if (isWarnEnabled()) log("WARN", String.format(format, args), null); }

    @Override public void info(String msg) { if (isInfoEnabled()) log("INFO", msg, null); }
    @Override public void info(String msg, Throwable t) { if (isInfoEnabled()) log("INFO", msg, t); }
    @Override public void info(String format, Object... args) { if (isInfoEnabled()) log("INFO", String.format(format, args), null); }

    @Override public void debug(String msg) { if (isDebugEnabled()) log("DEBUG", msg, null); }
    @Override public void debug(String msg, Throwable t) { if (isDebugEnabled()) log("DEBUG", msg, t); }
    @Override public void debug(String format, Object... args) { if (isDebugEnabled()) log("DEBUG", String.format(format, args), null); }

    @Override public void trace(String msg) { if (isTraceEnabled()) log("TRACE", msg, null); }
    @Override public void trace(String msg, Throwable t) { if (isTraceEnabled()) log("TRACE", msg, t); }
    @Override public void trace(String format, Object... args) { if (isTraceEnabled()) log("TRACE", String.format(format, args), null); }

    @Override public boolean isErrorEnabled() { return logLevel >= 1; }
    @Override public boolean isWarnEnabled() { return logLevel >= 2; }
    @Override public boolean isInfoEnabled() { return logLevel >= 3; }
    @Override public boolean isDebugEnabled() { return logLevel >= 4; }
    @Override public boolean isTraceEnabled() { return logLevel >= 5; }
}
