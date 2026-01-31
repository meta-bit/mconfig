package org.metabit.platform.support.config.source.hardcoded.provider;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.ConfigFactoryComponent;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Component that handles self-configuration of mConfig by looking for mconfig.properties in the classpath.
 * It searches for resources in ".config/metabit/mConfig/mconfig.properties".
 */
public class SelfConfigurationComponent implements ConfigFactoryComponent
{
    private static final String SELF_CONFIG_FILE = ".config/metabit/mConfig/mconfig.properties";

    @Override
    public String getComponentID()
    {
        return "mconfig-jar-self-config";
    }

    @Override
    public boolean initialize(ConfigFactoryInstanceContext ctx)
    {
        ConfigLoggingInterface logger = ctx.getLogger();
        ConfigFactorySettings settings = ctx.getSettings();

        if (!settings.getBoolean(ConfigFeature.ENABLE_SELF_CONFIGURATION))
        {
            logger.debug("Self-configuration is disabled via ConfigFeature.ENABLE_SELF_CONFIGURATION.");
            return true;
        }

        ClassLoader classLoader = ctx.getClassLoader();

        try
        {
            Enumeration<URL> resources = classLoader.getResources(SELF_CONFIG_FILE);
            while (resources.hasMoreElements())
            {
                URL url = resources.nextElement();
                logger.debug("Found self-configuration file: " + url);
                try (InputStream is = url.openStream())
                {
                    Properties props = new Properties();
                    props.load(is);
                    applyProperties(settings, props, logger);
                }
                catch (IOException e)
                {
                    logger.error("Failed to read self-configuration from " + url, e);
                }
            }
        }
        catch (IOException e)
        {
            logger.warn("Error during self-configuration discovery: " + e.getMessage());
        }

        return true;
    }

    private void applyProperties(ConfigFactorySettings settings, Properties props, ConfigLoggingInterface logger)
    {
        for (String key : props.stringPropertyNames())
        {
            try
            {
                ConfigFeature feature = ConfigFeature.valueOf(key.toUpperCase());
                String value = props.getProperty(key);
                applyValue(settings, feature, value);
                logger.debug("Applied self-configuration: " + feature + " = " + value);
            }
            catch (IllegalArgumentException e)
            {
                logger.warn("Unknown ConfigFeature in self-configuration: " + key);
            }
            catch (Exception e)
            {
                logger.error("Failed to apply self-configuration for " + key, e);
            }
        }
    }

    private void applyValue(ConfigFactorySettings settings, ConfigFeature feature, String value)
    {
        if (feature.isBooleanType())
        {
            settings.setBoolean(feature, Boolean.parseBoolean(value));
        }
        else if (feature.isStringType())
        {
            settings.setString(feature, value);
        }
        else if (feature.isNumberType())
        {
            settings.setInteger(feature, Integer.parseInt(value));
        }
        else if (feature.isStringListType())
        {
            String[] parts = value.split(",");
            java.util.List<String> list = new java.util.ArrayList<>();
            for (String part : parts)
            {
                String trimmed = part.trim();
                if (!trimmed.isEmpty())
                {
                    list.add(trimmed);
                }
            }
            settings.setStrings(feature, list);
        }
        // Special classes (Map, EnumMap, etc.) are currently not supported via simple properties
    }
}
