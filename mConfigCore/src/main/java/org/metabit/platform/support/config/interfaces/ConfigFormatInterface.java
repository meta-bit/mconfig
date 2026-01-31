package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.impl.ConfigFactorySettings;

/**
 * <p>ConfigFormatInterface interface.</p>
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigFormatInterface
{
 /**
  * <p>getFormatID.</p>
  *
  * @return a {@link java.lang.String} object
  */
 String getFormatID();

 /**
  * <p>testComponent.</p>
  *
  * @param configFactorySettings a {@link org.metabit.platform.support.config.impl.ConfigFactorySettings} object
  * @param logger a {@link org.metabit.platform.support.config.interfaces.ConfigLoggingInterface} object
  * @return a boolean
  */
 boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger);
}
