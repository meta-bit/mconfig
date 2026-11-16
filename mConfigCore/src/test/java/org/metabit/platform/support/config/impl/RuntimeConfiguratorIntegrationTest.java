package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class RuntimeConfiguratorIntegrationTest
{

    private ConfigFactorySettings getSettingsFromFactory(ConfigFactory factory) throws Exception
    {
        Field ctxField = org.metabit.platform.support.config.impl.DefaultConfigFactory.class.getDeclaredField("ctx");
        ctxField.setAccessible(true);
        ConfigFactoryInstanceContext ctx = (ConfigFactoryInstanceContext) ctxField.get(factory);
        return ctx.getSettings();
    }

    @Test
    public void testTestModeViaEnvVar() throws Exception
    {
        Map<String, String> env = new HashMap<>();
        env.put("MCONFIG_RUNTIME_TEST_MODE", "true");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "ENV_TEST");
        builder.setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true);
        
        Field field = DefaultConfigFactoryBuilder.class.getDeclaredField("configFactorySettings");
        field.setAccessible(true);
        ConfigFactorySettings configFactorySettings = (ConfigFactorySettings) field.get(builder);
        
        RuntimeConfigurator.applyRuntimeSettingsFromEnvVars(configFactorySettings, env);
        
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertTrue(settings.getBoolean(ConfigFeature.TEST_MODE), "TEST_MODE should be enabled via env var when ALLOW_MCONFIG_RUNTIME_SETTINGS is set in code");
    }

    @Test
    public void testEnvVarIgnoredIfFeatureDisabled() throws Exception
    {
        Map<String, String> env = new HashMap<>();
        env.put("MCONFIG_RUNTIME_TEST_MODE", "true");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "ENV_TEST");
        // ALLOW_MCONFIG_RUNTIME_SETTINGS is false by default
        
        Field field = DefaultConfigFactoryBuilder.class.getDeclaredField("configFactorySettings");
        field.setAccessible(true);
        ConfigFactorySettings configFactorySettings = (ConfigFactorySettings) field.get(builder);
        
        RuntimeConfigurator.applyRuntimeSettingsFromEnvVars(configFactorySettings, env);
        
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertFalse(settings.getBoolean(ConfigFeature.TEST_MODE), "TEST_MODE should NOT be enabled via env var when ALLOW_MCONFIG_RUNTIME_SETTINGS is false");
    }

    @Test
    public void testCannotEnableAllowRuntimeViaEnvVar() throws Exception
    {
        Map<String, String> env = new HashMap<>();
        env.put("ALLOW_MCONFIG_RUNTIME_SETTINGS", "true");
        env.put("MCONFIG_RUNTIME_TEST_MODE", "true");

        ConfigFactorySettings settings = new ConfigFactorySettings();
        // Initially false
        assertFalse(settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS));
        
        RuntimeConfigurator.applyRuntimeSettingsFromEnvVars(settings, env);

        assertFalse(settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS), "ALLOW_MCONFIG_RUNTIME_SETTINGS should not be enabled via env var");
        assertFalse(settings.getBoolean(ConfigFeature.TEST_MODE), "MCONFIG_RUNTIME_TEST_MODE should be ignored if ALLOW_MCONFIG_RUNTIME_SETTINGS was not already enabled");
    }

    @Test
    public void testPermitTestModeNotAllowedViaEnvVar() throws Exception
    {
        Map<String, String> env = new HashMap<>();
        env.put("MCONFIG_RUNTIME_PERMIT_TEST_MODE", "false");

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "ENV_TEST");
        builder.setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true);

        Field field = DefaultConfigFactoryBuilder.class.getDeclaredField("configFactorySettings");
        field.setAccessible(true);
        ConfigFactorySettings configFactorySettings = (ConfigFactorySettings) field.get(builder);
        
        RuntimeConfigurator.applyRuntimeSettingsFromEnvVars(configFactorySettings, env);

        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        // Should STILL be true (default), because env var should be ignored
        assertTrue(settings.getBoolean(ConfigFeature.PERMIT_TEST_MODE), "PERMIT_TEST_MODE should NOT be affected via env var");
    }

    @Test
    public void testCodeOnlyFeatureRefusedViaEnvVar()
    {
        Map<String, String> env = new HashMap<>();
        env.put("MCONFIG_RUNTIME_MODULES", "/tmp/evil-module.jar");
        env.put("MCONFIG_RUNTIME_DEBUG_LEVEL", "debug");

        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true); // delegated in code

        java.util.List<String> refused = RuntimeConfigurator.applyRuntimeSettingsFromEnvVars(settings, env);

        assertEquals(java.util.List.of("MCONFIG_RUNTIME_MODULES"), refused, "ADDITIONAL_MODULE_PATHS is code only: it loads code");
        assertTrue(settings.getStrings(ConfigFeature.ADDITIONAL_MODULE_PATHS).isEmpty());
        assertEquals(4, settings.getInteger(ConfigFeature.LOGLEVEL_NUMBER), "ordinary runtime settings still apply");
    }

    @Test
    public void testSettingLevels()
    {
        assertEquals(ConfigFeature.SettingLevel.CODE_ONLY, ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.CODE_ONLY, ConfigFeature.ENABLE_SELF_CONFIGURATION.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.CODE_ONLY, ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.CODE_ONLY, ConfigFeature.ADDITIONAL_MODULE_PATHS.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.COMPONENT, ConfigFeature.PERMIT_TEST_MODE.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.COMPONENT, ConfigFeature.TEST_MODE.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.COMPONENT, ConfigFeature.TESTMODE_DIRECTORIES.getSettingLevel());
        assertTrue(ConfigFeature.SettingLevel.COMPONENT.allowsComponentFile());
        assertFalse(ConfigFeature.SettingLevel.COMPONENT.allowsGlobalFile());
        assertFalse(ConfigFeature.SettingLevel.CODE_ONLY.allowsDelegated());
        assertEquals(ConfigFeature.SettingLevel.ANY, ConfigFeature.COMMENTS_READING.getSettingLevel());
        assertEquals(ConfigFeature.SettingLevel.ANY, new org.metabit.platform.support.config.ConfigFeatureBase("MODULE_X", ConfigFeature.ValueType.STRING).getSettingLevel(),
                "module features default to ANY");
    }

    @Test
    public void testSystemPropertiesMergedIntoRuntimeSettings()
    {
        Map<String, String> env = new HashMap<>();
        env.put("MCONFIG_RUNTIME_DEBUG_OUTPUT", "env");
        env.put("PATH", "/usr/bin");

        Properties sysProps = new Properties();
        sysProps.setProperty("mconfig.runtime.kubernetes.config.dir", "/etc/config");
        sysProps.setProperty("MCONFIG_RUNTIME_DEBUG_OUTPUT", "sysprop");
        sysProps.setProperty("java.version", "17");

        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR")); // dotless i must not break the prefix mapping
        try
        {
            Map<String, String> merged = RuntimeConfigurator.mergeWithSystemProperties(env, sysProps);
            assertEquals("/etc/config", merged.get("MCONFIG_RUNTIME_KUBERNETES_CONFIG_DIR"));
            assertEquals("sysprop", merged.get("MCONFIG_RUNTIME_DEBUG_OUTPUT"), "system property overrides env var");
            assertEquals("/usr/bin", merged.get("PATH"));
            assertFalse(merged.containsKey("java.version"), "unrelated system properties are not copied");
        }
        finally
        {
            Locale.setDefault(previous);
        }
    }
}
//___EOF___
