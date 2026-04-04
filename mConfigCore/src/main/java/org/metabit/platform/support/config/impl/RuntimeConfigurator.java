package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigFeature;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles processing of MCONFIG_RUNTIME_* environment variables.
 */
public class RuntimeConfigurator
{
    private static final String ALLOW_RUNTIME_FLAG = "ALLOW_MCONFIG_RUNTIME_SETTINGS";
    private static final String PREFIX = "MCONFIG_RUNTIME_";

    public static void applyRuntimeSettingsFromEnvVars(ConfigFactorySettings settings)
    {
        applyRuntimeSettingsFromEnvVars(settings, System.getenv());
    }

    /**
     * gets environment variables and applies them to runtime settings.
     *
     * @param settings the settings to apply to
     */
    static void applyRuntimeSettingsFromEnvVars(ConfigFactorySettings settings, Map<String, String> env)
    {
        if (!settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS))
        {
            return;
        }

        for (Map.Entry<String, String> entry : env.entrySet())
        {
            String key = entry.getKey();
            if (key.startsWith(PREFIX))
            {
                applySetting(settings, key.substring(PREFIX.length()), entry.getValue());
            }
        }
    }

    /**
     * this is where MCONFIG_RUNTIME_* env vars are applied to the settings.
     *
     * @param settings the settings to apply to
     * @param key      the key of the env var
     * @param value    the value of the env var
     */
    private static void applySetting(ConfigFactorySettings settings, String key, String value)
        {
        switch (key.toUpperCase())
            {
            case "DEBUG_LEVEL":
                settings.setInteger(ConfigFeature.LOGLEVEL_NUMBER, parseLogLevel(value));
                break;
            case "DEBUG_OUTPUT":
                settings.setString(ConfigFeature.LOGGING_REDIRECT_TARGET, value);
                break;
            case "DISABLE_MODULES":
                settings.setStrings(ConfigFeature.DISABLED_MODULE_IDS, parseList(value));
                break;
            case "MODULES":
                settings.setStrings(ConfigFeature.ADDITIONAL_MODULE_PATHS, parseList(value));
                break;
            case "TEST_MODE":
                settings.setBoolean(ConfigFeature.TEST_MODE, "true".equalsIgnoreCase(value));
                break;
            }
        }

    private static Integer parseLogLevel(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            switch (value.toLowerCase())
            {
                case "off": return 0;
                case "error": return 1;
                case "warn": return 2;
                case "info": return 3;
                case "debug": return 4;
                case "trace": return 5;
                default: return 2; // Default to warn
            }
        }
    }

    private static List<String> parseList(String value)
    {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
