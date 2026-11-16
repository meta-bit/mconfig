package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.impl.entry.BooleanLiterals;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureRegistry;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * self-configuration of a ConfigFactory from {@code mconfig.properties} files on the classpath.
 * <p>
 * Two files are read, in this order:
 * <ol>
 * <li>the component file {@code .config/<company>/<application>/mconfig.properties}
 *     (without a company: {@code .config/<application>/mconfig.properties}). It applies only to factories
 *     created for that company and application, so each component that uses mConfig configures only itself.</li>
 * <li>the deprecated global file {@code .config/metabit/mConfig/mconfig.properties}. It applies to every
 *     factory in the process; it is still read in 0.8.x, with a warning, and will be removed before 0.9.0.</li>
 * </ol>
 * Files only fill in features that are not set yet: code (and runtime settings delegated by code) always win,
 * the component file wins over the global file. Each file may only set features its channel allows
 * (see {@link ConfigFeature.SettingLevel}).
 * <p>
 * Runs during {@code ConfigFactoryBuilder.build()}, before the test-mode gates are evaluated.
 * Internal implementation class.
 */
public final class SelfConfigurator
{
    static final String CONFIG_RESOURCE_PATH_PREFIX = ".config/";
    static final String SELF_CONFIG_FILE_NAME       = "mconfig.properties";
    static final String GLOBAL_SELF_CONFIG_FILE     = ".config/metabit/mConfig/mconfig.properties";

    private enum Channel { COMPONENT, GLOBAL }

    private SelfConfigurator() { }

    /**
     * applies the self-configuration files to the settings.
     *
     * @param settings    the factory settings, already holding the values set by code and runtime settings
     * @param classLoader the class loader to search
     * @param logger      the logger
     */
    public static void apply(ConfigFactorySettings settings, ClassLoader classLoader, ConfigLoggingInterface logger)
        {
        if (!settings.getBoolean(ConfigFeature.ENABLE_SELF_CONFIGURATION))
            {
            logger.debug("Self-configuration is disabled via ConfigFeature.ENABLE_SELF_CONFIGURATION.");
            return;
            }
        String componentFile = componentFilePath(settings.getString(ConfigFeature.COMPANY_NAME), settings.getString(ConfigFeature.APPLICATION_NAME));
        applyFiles(settings, classLoader, logger, componentFile, Channel.COMPONENT);
        if (!GLOBAL_SELF_CONFIG_FILE.equals(componentFile)) // a factory for metabit/mConfig itself reads it as its component file
            {
            applyFiles(settings, classLoader, logger, GLOBAL_SELF_CONFIG_FILE, Channel.GLOBAL);
            }
        }

    static String componentFilePath(String companyName, String applicationName)
        {
        if (companyName == null || companyName.trim().isEmpty())
            {
            return CONFIG_RESOURCE_PATH_PREFIX+applicationName+"/"+SELF_CONFIG_FILE_NAME;
            }
        return CONFIG_RESOURCE_PATH_PREFIX+companyName+"/"+applicationName+"/"+SELF_CONFIG_FILE_NAME;
        }

    private static void applyFiles(ConfigFactorySettings settings, ClassLoader classLoader, ConfigLoggingInterface logger, String resourcePath, Channel channel)
        {
        Enumeration<URL> resources;
        try
            {
            resources = classLoader.getResources(resourcePath);
            }
        catch (IOException e)
            {
            logger.warn("Error during self-configuration discovery of "+resourcePath+": "+e.getMessage());
            return;
            }
        while (resources.hasMoreElements())
            {
            URL url = resources.nextElement();
            if (channel == Channel.GLOBAL)
                {
                logger.warn("Self-configuration: "+url+" is deprecated and will be ignored from 0.9.0 on; it applies to every component in the process. "
                        +"Move the settings to .config/<company>/<application>/"+SELF_CONFIG_FILE_NAME+".");
                }
            else
                {
                logger.debug("Found self-configuration file: "+url);
                }
            try (InputStream is = url.openStream())
                {
                Properties props = new Properties();
                props.load(is);
                applyProperties(settings, props, logger, url, channel);
                }
            catch (IOException e)
                {
                logger.error("Failed to read self-configuration from "+url, e);
                }
            }
        }

    private static void applyProperties(ConfigFactorySettings settings, Properties props, ConfigLoggingInterface logger, URL url, Channel channel)
        {
        for (String key : props.stringPropertyNames())
            {
            String value = props.getProperty(key);
            ConfigFeatureInterface feature = ConfigFeatureRegistry.get(key);
            if (feature != null && !isAllowed(feature.getSettingLevel(), channel))
                {
                logger.warn("Self-configuration: ignoring "+key+" from "+url+"; "+describeAllowed(feature.getSettingLevel())+" may set this feature.");
                continue;
                }
            if (settings.isExplicitlySet(key))
                {
                logger.debug("Self-configuration: "+key+" from "+url+" not applied; already set (code and runtime settings win, the component file wins over the global file).");
                continue;
                }
            try
                {
                if (feature != null)
                    {
                    applyValue(settings, feature, value);
                    logger.debug("Applied self-configuration: "+key+" = "+value);
                    if (feature == ConfigFeature.TEST_MODE && Boolean.TRUE.equals(BooleanLiterals.parse(value)))
                        {
                        // make an accidentally shipped file visible at once
                        logger.warn("Self-configuration: test mode requested by "+url+" (still subject to PERMIT_TEST_MODE and forbidTestMode()).");
                        }
                    }
                else
                    {
                    settings.setFeature(key, value);
                    logger.debug("Applied custom self-configuration: "+key+" = "+value);
                    }
                }
            catch (Exception e)
                {
                logger.error("Failed to apply self-configuration for "+key+" from "+url, e);
                }
            }
        }

    private static boolean isAllowed(ConfigFeature.SettingLevel level, Channel channel)
        {
        return (channel == Channel.COMPONENT) ? level.allowsComponentFile() : level.allowsGlobalFile();
        }

    private static String describeAllowed(ConfigFeature.SettingLevel level)
        {
        switch (level)
            {
            case CODE_ONLY:
                return "only code";
            case COMPONENT:
                return "only code, runtime settings or the component's own .config/<company>/<application>/"+SELF_CONFIG_FILE_NAME;
            default:
                return "any channel";
            }
        }

    /** strict, as for configuration values: a typing error must not silently switch a feature off. */
    static boolean parseFeatureBoolean(String value)
        {
        Boolean result = BooleanLiterals.parse(value);
        if (result == null)
            { throw new IllegalArgumentException("not a Boolean value; accepted are true/false, yes/no, on/off, 1/0"); }
        return result;
        }

    private static void applyValue(ConfigFactorySettings settings, ConfigFeatureInterface feature, String value)
        {
        switch (feature.getValueType())
            {
            case BOOLEAN:
                settings.setBoolean(feature, parseFeatureBoolean(value));
                break;
            case STRING:
                settings.setString(feature, value);
                break;
            case NUMBER:
                settings.setInteger(feature, Integer.parseInt(value.trim()));
                break;
            case STRINGLIST:
                List<String> list = new ArrayList<>();
                for (String part : value.split(","))
                    {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty())
                        {
                        list.add(trimmed);
                        }
                    }
                settings.setStrings(feature, list);
                break;
            case SPECIAL_CLASS:
                // Special classes (Map, EnumMap, etc.) are currently not supported via simple properties
                break;
            }
        }
}
//___EOF___
