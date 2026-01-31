package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLoggingSelection
{
    public static final String COMPANY_NAME = "metabit";
    public static final String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME     = "testconfig";
    @Test
    void setStdErrLogger()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB, "stderr-logger") // return null instead of throwing an exception
                .build())
            {
            Configuration cfg = factory.getConfig("this configuration does not exist");
            assertTrue(cfg.isEmpty());

            // no exception, by default.
            ConfigEntry entry = cfg.getConfigEntryFromFullKey("invalidEntry", null);
            assertNull(entry);
            return;
            }
        }


}
