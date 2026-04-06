package org.metabit.platform.support.config.impl.format.toml.jackson;

import tools.jackson.databind.JsonNode;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.format.json.jackson.JSONJacksonConfigLayer;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.format.json.jackson.JSONwithJacksonFormat;

import java.util.Stack;

/**
 * Custom layer implementation for TOML that handles hierarchical key writing correctly
 * when used with ConfigMapper's recursive cursor traversal.
 */
public class TOMLJacksonConfigLayer extends JSONJacksonConfigLayer
{
    private final Stack<String> pathStack = new Stack<>();

    public TOMLJacksonConfigLayer(ConfigFactorySettings settings, ConfigLoggingInterface logger, ConfigLocation configLocation, JSONwithJacksonFormat format, JsonNode rootNode, Object storageInstanceHandle)
        {
        super(settings, logger, configLocation, format, rootNode, storageInstanceHandle);
        }

    @Override
    public void writeEntry(ConfigEntry configEntry) throws ConfigCheckedException
        {
        if (!isWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        super.writeEntry(configEntry);
        }
}
