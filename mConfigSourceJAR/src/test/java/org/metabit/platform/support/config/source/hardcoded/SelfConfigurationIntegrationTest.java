package org.metabit.platform.support.config.source.hardcoded;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelfConfigurationIntegrationTest
{
    @Test
    public void testSelfConfigurationFromJar() throws Exception
    {
        // The test resource .config/metabit/mConfig/mconfig.properties should be picked up
        // It contains: ALLOW_MCONFIG_RUNTIME_SETTINGS=true

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "SELF_CONFIG_TEST");
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertTrue(settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS),
                "ALLOW_MCONFIG_RUNTIME_SETTINGS should be enabled via self-configuration properties file");
    }

    @Test
    public void testDisableSelfConfiguration() throws Exception
    {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "SELF_CONFIG_TEST");
        builder.setFeature(ConfigFeature.ENABLE_SELF_CONFIGURATION, false);
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertFalse(settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS),
                "ALLOW_MCONFIG_RUNTIME_SETTINGS should NOT be enabled if self-configuration is disabled");
    }

    private ConfigFactorySettings getSettingsFromFactory(ConfigFactory factory) throws Exception
    {
        Field ctxField = DefaultConfigFactory.class.getDeclaredField("ctx");
        ctxField.setAccessible(true);
        ConfigFactoryInstanceContext ctx = (ConfigFactoryInstanceContext) ctxField.get(factory);
        return ctx.getSettings();
    }
}
