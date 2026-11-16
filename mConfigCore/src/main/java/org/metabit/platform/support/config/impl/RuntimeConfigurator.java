package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Handles processing of MCONFIG_RUNTIME_* environment variables.
 */
public class RuntimeConfigurator
{
    private static final String ALLOW_RUNTIME_FLAG = "ALLOW_MCONFIG_RUNTIME_SETTINGS";
    private static final String PREFIX = "MCONFIG_RUNTIME_";

    private static final String SYSPROP_PREFIX = "mconfig.runtime.";

    /**
     * applies runtime settings from environment variables and JVM system properties.
     * <p>
     * System properties are accepted in two forms: {@code -Dmconfig.runtime.kubernetes.config.dir=...}
     * (mapped to {@code MCONFIG_RUNTIME_KUBERNETES_CONFIG_DIR}), or verbatim as {@code -DMCONFIG_RUNTIME_...}.
     * On a name clash, the system property takes precedence over the environment variable.
     * Both are only evaluated if {@link ConfigFeature#ALLOW_MCONFIG_RUNTIME_SETTINGS} is set.
     * Features that only code may set ({@link ConfigFeature.SettingLevel#CODE_ONLY}) are refused.
     * <p>
     * This runs before the logger exists (runtime settings may change logging), so refusals are
     * returned for the caller to log.
     *
     * @param settings the settings to apply to
     * @return the names of the refused runtime settings; empty if none
     */
    public static List<String> applyRuntimeSettingsFromEnvVars(ConfigFactorySettings settings)
    {
        return applyRuntimeSettingsFromEnvVars(settings, mergeWithSystemProperties(System.getenv(), System.getProperties()));
    }

    /**
     * merges the runtime-prefixed system properties into a copy of the environment map.
     * Unrelated system properties are not copied.
     */
    static Map<String, String> mergeWithSystemProperties(Map<String, String> env, Properties sysProps)
    {
        Map<String, String> merged = new HashMap<>(env);
        for (String k : sysProps.stringPropertyNames()) // snapshot, safe against concurrent modification
        {
            String value = sysProps.getProperty(k);
            if (value == null)
                continue;
            if (k.toLowerCase(Locale.ROOT).startsWith(SYSPROP_PREFIX))
            {
                String converted = PREFIX + k.substring(SYSPROP_PREFIX.length()).replace('.', '_').toUpperCase(Locale.ROOT);
                merged.put(converted, value);
            }
            else if (k.startsWith(PREFIX))
            {
                merged.put(k, value);
            }
        }
        return merged;
    }

    /**
     * gets environment variables and applies them to runtime settings.
     *
     * @param settings the settings to apply to
     */
    static List<String> applyRuntimeSettingsFromEnvVars(ConfigFactorySettings settings, Map<String, String> env)
    {
        List<String> refused = new ArrayList<>();
        if (!settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS))
        {
            return refused;
        }

        for (Map.Entry<String, String> entry : env.entrySet())
        {
            String key = entry.getKey();
            if (key.startsWith(PREFIX))
            {
                if (!applySetting(settings, key.substring(PREFIX.length()), entry.getValue()))
                {
                    refused.add(key);
                }
            }
        }
        return refused;
    }

    /**
     * this is where MCONFIG_RUNTIME_* env vars are applied to the settings.
     *
     * @param settings the settings to apply to
     * @param key      the key of the env var
     * @param value    the value of the env var
     * @return false if the setting was refused, because only code may set the feature
     */
    private static boolean applySetting(ConfigFactorySettings settings, String key, String value)
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
                if (!ConfigFeature.ADDITIONAL_MODULE_PATHS.getSettingLevel().allowsDelegated()) return false;
                settings.setStrings(ConfigFeature.ADDITIONAL_MODULE_PATHS, parseList(value));
                break;
            case "TEST_MODE":
                if (!ConfigFeature.TEST_MODE.getSettingLevel().allowsDelegated()) return false;
                settings.setBoolean(ConfigFeature.TEST_MODE, "true".equalsIgnoreCase(value));
                break;
            /*
            0.8.14
            rationale for placing these in the core module (for now):
            instead of complicating the bootstrap logic, we keep it in this
            consistent location.
            Dynamic env var prefixing would introduce higher complexity.
            Kubernetes is important enough to justify the special treatment;
            should this list grow, then we might need a module-led registration
             */
            case "KUBERNETES_API_ENDPOINT":
                settings.setFeature("KUBERNETES_API_ENDPOINT", value);
                break;
            case "KUBERNETES_CONFIG_DIR":
                settings.setFeature("KUBERNETES_CONFIG_DIR_PATHS", value);
                break;
            case "KUBERNETES_SECRETS_DIR":
                settings.setFeature("KUBERNETES_SECRETS_DIR_PATHS", value);
                break;
            case "KUBERNETES_SA_TOKEN_PATH":
                settings.setFeature("KUBERNETES_SA_TOKEN_PATH", value);
                break;
            case "KUBERNETES_SA_NAMESPACE_PATH":
                settings.setFeature("KUBERNETES_SA_NAMESPACE_PATH", value);
                break;
            case "KUBERNETES_SA_CA_CERT_PATH":
                settings.setFeature("KUBERNETES_SA_CA_CERT_PATH", value);
                break;
            }
        return true;
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
//___EOF___
