package org.metabit.platform.support.config.impl.core;

import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

/**
 * log-to-/dev/null instance: no output at all.
 *
 * 
 * @version $Id: $Id
 */
public class NullLogging implements ConfigLoggingInterface
    {
    /** {@inheritDoc} */
    @Override
    public final String getServiceModuleName()
        { return "null"; }

    /** {@inheritDoc} */
    @Override
    public int getServiceModulePriority()
        { return 0; } // lowest

    /** {@inheritDoc} */
    @Override
    public boolean init(ConfigFactorySettings settings)
        { return true; }

    /** {@inheritDoc} */
    @Override
    public void exit()
        { }

    /** {@inheritDoc} */
    @Override
    public void error(final String msg)
        { }

    /** {@inheritDoc} */
    @Override
    public void error(final String msg, final Throwable t)
        { }

    /** {@inheritDoc} */
    @Override
    public void error(final String format, final Object... arguments)
        { }

    /** {@inheritDoc} */
    @Override
    public void warn(final String msg)
        { }

    /** {@inheritDoc} */
    @Override
    public void warn(final String msg, final Throwable t)
        { }

    /** {@inheritDoc} */
    @Override
    public void warn(final String format, final Object... arguments)
        { }

    /** {@inheritDoc} */
    @Override
    public void info(final String msg)
        { }

    /** {@inheritDoc} */
    @Override
    public void info(final String msg, final Throwable t)
        { }

    /** {@inheritDoc} */
    @Override
    public void info(final String format, final Object... arguments)
        { }

    /** {@inheritDoc} */
    @Override
    public void debug(final String msg)
        { }

    /** {@inheritDoc} */
    @Override
    public void debug(final String msg, final Throwable t)
        { }

    /** {@inheritDoc} */
    @Override
    public void debug(final String format, final Object... arguments)
        { }

    /** {@inheritDoc} */
    @Override
    public void trace(final String msg)
        { }

    /** {@inheritDoc} */
    @Override
    public void trace(final String msg, final Throwable t)
        { }

    /** {@inheritDoc} */
    @Override
    public void trace(final String format, final Object... arguments)
        { }

    /** {@inheritDoc} */
    @Override
    public boolean isErrorEnabled()
        { return false; }

    /** {@inheritDoc} */
    @Override
    public boolean isWarnEnabled()
        { return false; }

    /** {@inheritDoc} */
    @Override
    public boolean isInfoEnabled()
        { return false; }

    /** {@inheritDoc} */
    @Override
    public boolean isDebugEnabled()
        { return false; }

    /** {@inheritDoc} */
    @Override
    public boolean isTraceEnabled()
        { return false; }

    // simple singleton suitable for this use.
    private static final ConfigLoggingInterface instance = new NullLogging();

    /**
     * <p>getSingletonInstance.</p>
     *
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigLoggingInterface} object
     */
    public static ConfigLoggingInterface getSingletonInstance()
        {
        return instance;
        }
    }
//___EOF___
