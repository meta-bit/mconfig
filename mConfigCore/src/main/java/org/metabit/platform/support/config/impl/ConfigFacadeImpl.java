package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.SecretConfigEntry;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import org.metabit.platform.support.config.impl.util.ConfigIOUtil;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <p>ConfigFacadeImpl class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class ConfigFacadeImpl extends LayeredConfiguration implements Configuration
{
    /**
     * <p>Constructor for ConfigFacadeImpl.</p>
     *
     * @param sanitizedConfigName a {@link java.lang.String} object
     * @param configScheme a {@link org.metabit.platform.support.config.scheme.ConfigScheme} object
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     * @param configFactory a {@link org.metabit.platform.support.config.impl.DefaultConfigFactory} object
     */
    public ConfigFacadeImpl(String sanitizedConfigName, ConfigScheme configScheme, ConfigFactoryInstanceContext ctx, ConfigFactory configFactory)
        {
        super(sanitizedConfigName, configScheme, ctx, configFactory);
        }


    // typed access
    //@TODO lacks scope limitation
    // and the fallback setting needs to be set in constructor etc.

    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        ConfigEntry entry = this.getConfigEntryFromFullKey(fullKey, EnumSet.allOf(ConfigScope.class));
        if (entry == null)
            {
            if (exceptionOnNullFlag)
                throw new ConfigException(ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND);
            return null;
            }
        if (entry instanceof SecretConfigEntry)
            {
            return ((SecretConfigEntry) entry).getSecretValue();
            }
        if (entry.isSecret())
            {
            try
                {
                final byte[] value;
                if (entry.getType() == ConfigEntryType.BYTES)
                    value = entry.getValueAsBytes();
                else
                    value = entry.getValueAsString().getBytes(StandardCharsets.UTF_8);

                return new BasicSecretValue(value, SecretType.PLAIN_TEXT);
                }
            catch (ConfigCheckedException e)
                {
                throw new ConfigException(e);
                }
            }
        throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE);
        }

    /**
     * <p>hexDecode.</p>
     *
     * @param tmpString a {@link java.lang.String} object
     * @return an array of {@link byte} objects
     */
    public static byte[] hexDecode(String tmpString)
        {
        return ConfigIOUtil.hexDecode(tmpString);
        }

    
    
    //TODO this is missing the List<String> getListOfStrings()
    
    
    @Override
    public ConfigCursor getConfigCursor()
        {
        return new ConfigCursorImpl(this);
        }
}
//___EOF___
