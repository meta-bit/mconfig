package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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
}
