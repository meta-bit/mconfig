package org.metabit.platform.support.config.util.loggers;


import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

public class StdErrLogger implements ConfigLoggingInterface
{

private int logLevel;

public String getServiceModuleName()
    {
    return "stderr-logger";
    }

public int getServiceModulePriority()
    {
    return 1;
    }

@Override
public boolean init(final ConfigFactorySettings settings)
    {
    logLevel = settings.getInteger(ConfigFeature.LOGLEVEL_NUMBER);
    if ((logLevel < 0)||(logLevel > 9))
        return false;
    if (logLevel > 5)
        logLevel = 5; // upper limit here.
    return true;
    }

@Override
public void exit()
    { }

final static String PREFIX_ERROR  ="[ERROR] "; // add red colour
final static String PREFIX_WARNING="[WARN] "; // add yellow colour
final static String PREFIX_INFO   ="[INFO] "; // add white colour
final static String PREFIX_DEBUG  ="[DEBUG] "; // add (light) blue colour
final static String PREFIX_TRACE  ="[TRACE] "; // add grey colour


public void error(String msg)
    {
    System.err.println(PREFIX_ERROR+msg);
    }

public void error(String msg, Throwable t)
    {
    System.err.println(PREFIX_ERROR+t.getMessage()+" : "+msg);
    }

public void error(String format, Object... arguments)
    {
    System.err.print(PREFIX_ERROR);
    System.err.format(format, arguments);
    }

public void warn(String msg)
    {
    System.err.println(PREFIX_WARNING+msg);
    }

public void warn(String msg, Throwable t)
    {
    System.err.println(PREFIX_WARNING+t.getMessage()+" : "+msg);
    }

public void warn(String format, Object... arguments)
    {
    System.err.print(PREFIX_WARNING);
    System.err.format(format, arguments);
    }

public void info(String msg)
    {
    System.err.println(PREFIX_INFO+msg);
    }

public void info(String msg, Throwable t)
    {
    System.err.println(PREFIX_INFO+t.getMessage()+" : "+msg);
    }

public void info(String format, Object... arguments)
    {
    System.err.print(PREFIX_INFO);
    System.err.format(format, arguments);
    }

public void debug(String msg)
    {
    System.err.println(PREFIX_DEBUG+msg);
    }

public void debug(String msg, Throwable t)
    {
    System.err.println(PREFIX_DEBUG+t.getMessage()+" : "+msg);
    }

public void debug(String format, Object... arguments)
    {
    System.err.print(PREFIX_DEBUG);
    System.err.format(format, arguments);
    }

public void trace(String msg)
    {
    System.err.println(PREFIX_TRACE+msg);
    }

public void trace(String msg, Throwable t)
    {
    System.err.println(PREFIX_TRACE+t.getMessage()+" : "+msg);
    }

public void trace(String format, Object... arguments)
    {
    System.err.print(PREFIX_TRACE);
    System.err.format(format, arguments);
    }

public boolean isErrorEnabled()
    {
    return (logLevel>0);
    }

public boolean isWarnEnabled()
    {
    return (logLevel>1);
    }

public boolean isInfoEnabled()
    {
    return (logLevel>2);
    }

public boolean isDebugEnabled()
    {
    return (logLevel>3);
    }

public boolean isTraceEnabled()
    {
    return (logLevel>4);
    }

public StdErrLogger()
    {
    logLevel=4; // default
    }

public int getNumericalLogLevel()
    {
    return logLevel;
    }

public void setNumericalLogLevel(final int logLevel)
    {
    if ((logLevel<0) || (logLevel>5))
        throw new IllegalArgumentException();
    this.logLevel=logLevel;
    }

}
