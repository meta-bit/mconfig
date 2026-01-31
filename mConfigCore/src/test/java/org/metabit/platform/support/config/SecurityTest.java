package org.metabit.platform.support.config;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityTest
{

    private void resetStaticState()
            throws Exception
        {
        Field permitted = org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder.class.getDeclaredField("testModePermitted");
        permitted.setAccessible(true);
        permitted.set(null, true);
        }

    private ConfigFactorySettings getSettingsFromFactory(ConfigFactory factory)
            throws Exception
        {
        Field ctxField = org.metabit.platform.support.config.impl.DefaultConfigFactory.class.getDeclaredField("ctx");
        ctxField.setAccessible(true);
        ConfigFactoryInstanceContext ctx = (ConfigFactoryInstanceContext) ctxField.get(factory);
        return ctx.getSettings();
        }

    /**
     * Verifies test mode is disabled when forbidden
     */
    @Test
    void testForbidTestModePreventsTestMode()
            throws Exception
        {
        resetStaticState();

        ConfigFactoryBuilder.forbidTestMode();

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "SEC_TEST");
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true); // Even if we permit it in feature
        builder.setFeature(ConfigFeature.TEST_MODE, true);

        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertFalse(settings.getBoolean(ConfigFeature.TEST_MODE), "TEST_MODE should be false when forbidden, even if attempted to set to true");
        }

    /**
     * Verifies test mode enablement after explicit forbidding
     */
    @Test
    void testPermitTestModeWorksAfterForbid()
            throws Exception
        {
        resetStaticState();
        ConfigFactoryBuilder.forbidTestMode();
        ConfigFactoryBuilder.permitTestMode();

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "SEC_TEST");
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertTrue(settings.getBoolean(ConfigFeature.TEST_MODE), "TEST_MODE should be true when permitted and active");
        }

    @Test
    void testFeaturePermitTestModeCanBlockTestMode()
            throws Exception
        {
        resetStaticState();
        ConfigFactoryBuilder.permitTestMode();

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "SEC_TEST");
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, false); // Explicitly disable permission
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = getSettingsFromFactory(factory);
        assertFalse(settings.getBoolean(ConfigFeature.TEST_MODE), "TEST_MODE should be false when PERMIT_TEST_MODE feature is explicitly set to false");
        }
}
