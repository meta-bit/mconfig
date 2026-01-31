package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;

import static org.junit.jupiter.api.Assertions.*;

public class TestExceptionFeatures
{
    //
    public static final String COMPANY_NAME = "metabit";
    public static final String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME     = "testconfig";

    @Test
    void testNullInsteadOfExceptionFeatureForEntries()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false) // return null instead of throwing an exception
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value1 = cfg.getString("undefinedEntry");
            assertNull(value1);
            }
        }

    @Test
    void testExceptionWantedFeatureForEntries()
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, true) // return null instead of throwing an exception
                .build())
        {
        Configuration cfg = factory.getConfig(CONFIG_NAME);

            Assertions.assertThrows(ConfigException.class, () ->
            { String value1 = cfg.getString("undefinedEntry");});

        return;
        }
    }

    @Test
    void testExceptionWantedFeatureForConfigurationsOff()
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND, false) // return null instead of throwing an exception
                .build())
        {
        Configuration cfg = factory.getConfig("this configuration does not exist");
            assertTrue(cfg.isEmpty());
            return;
        }
    }


    @Test
    void testExceptionWantedFeatureForConfigurationsOn()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND, true) // return null instead of throwing an exception
                .build())
            {
            ConfigException ex = Assertions.assertThrows(ConfigException.class, ()->
                {
                Configuration cfg = factory.getConfig("this configuration does not exist");
                });
            assertEquals(ex.getReason(), ConfigException.ConfigExceptionReason.NO_CONFIGURATION_FOUND);
            }
        }
}
