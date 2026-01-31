package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.impl.ConfigFactorySettings;

/**
 * an wrapper for external logging functions.
 * if no service provider for this is found, nothing is logged at all.
 * Copyright metabit
 * Created by jw on 2020-04-13, 2021-04-26, 2023-06-13
 * <p>
 * resembling an traditional logging API, so mConfig modules can use some simple logging.
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigLoggingInterface
{
/**
 * <p>getServiceModuleName.</p>
 *
 * @return service name to identify the service module by. Stick with ASCII.
 *         <p>
 *         should be static, but java interfacing do not allow static.
 */
String getServiceModuleName(); //

/**
 * <p>getServiceModulePriority.</p>
 *
 * @return self-assigned service module priority as integer. The higher the value,
 *         the more this module thinks itself important.
 *         <p>
 *         should be static, but java interfacing do not allow static.
 *
 * logging levels and numbers mapping
 * 0    off/quiet
 * 1    error
 * 2    warn
 * 3    info
 * 4    debug
 * 5    trace
 *
 * Usually, one wants runtime to run at warn level; sometimes info.
 */
int getServiceModulePriority();

/**
 * <p>init.</p>
 *
 * @param settings a {@link org.metabit.platform.support.config.impl.ConfigFactorySettings} object
 * @return a boolean
 */
boolean init(final ConfigFactorySettings settings);

/**
 * <p>exit.</p>
 */
void exit();

/**
 * <p>error.</p>
 *
 * @param msg a {@link java.lang.String} object
 */
void error(final String msg);

/**
 * <p>error.</p>
 *
 * @param msg a {@link java.lang.String} object
 * @param t a {@link java.lang.Throwable} object
 */
void error(final String msg, final Throwable t);

/**
 * <p>error.</p>
 *
 * @param format a {@link java.lang.String} object
 * @param arguments a {@link java.lang.Object} object
 */
void error(final String format, final Object... arguments);

/**
 * <p>warn.</p>
 *
 * @param msg a {@link java.lang.String} object
 */
void warn(final String msg);

/**
 * <p>warn.</p>
 *
 * @param msg a {@link java.lang.String} object
 * @param t a {@link java.lang.Throwable} object
 */
void warn(final String msg, final Throwable t);

/**
 * <p>warn.</p>
 *
 * @param format a {@link java.lang.String} object
 * @param arguments a {@link java.lang.Object} object
 */
void warn(final String format, final Object... arguments);

/**
 * <p>info.</p>
 *
 * @param msg a {@link java.lang.String} object
 */
void info(final String msg);

/**
 * <p>info.</p>
 *
 * @param msg a {@link java.lang.String} object
 * @param t a {@link java.lang.Throwable} object
 */
void info(final String msg, final Throwable t);

/**
 * <p>info.</p>
 *
 * @param format a {@link java.lang.String} object
 * @param arguments a {@link java.lang.Object} object
 */
void info(final String format, final Object... arguments);

/**
 * <p>debug.</p>
 *
 * @param msg a {@link java.lang.String} object
 */
void debug(final String msg);

/**
 * <p>debug.</p>
 *
 * @param msg a {@link java.lang.String} object
 * @param t a {@link java.lang.Throwable} object
 */
void debug(final String msg, final Throwable t);

/**
 * <p>debug.</p>
 *
 * @param format a {@link java.lang.String} object
 * @param arguments a {@link java.lang.Object} object
 */
void debug(final String format, final Object... arguments);

/**
 * <p>trace.</p>
 *
 * @param msg a {@link java.lang.String} object
 */
void trace(final String msg);

/**
 * <p>trace.</p>
 *
 * @param msg a {@link java.lang.String} object
 * @param t a {@link java.lang.Throwable} object
 */
void trace(final String msg, final Throwable t);

/**
 * <p>trace.</p>
 *
 * @param format a {@link java.lang.String} object
 * @param arguments a {@link java.lang.Object} object
 */
void trace(final String format, final Object... arguments);

/**
 * <p>isErrorEnabled.</p>
 *
 * @return a boolean
 */
boolean isErrorEnabled();

/**
 * <p>isWarnEnabled.</p>
 *
 * @return a boolean
 */
boolean isWarnEnabled();

/**
 * <p>isInfoEnabled.</p>
 *
 * @return a boolean
 */
boolean isInfoEnabled();

/**
 * <p>isDebugEnabled.</p>
 *
 * @return a boolean
 */
boolean isDebugEnabled();

/**
 * <p>isTraceEnabled.</p>
 *
 * @return a boolean
 */
boolean isTraceEnabled();
}
