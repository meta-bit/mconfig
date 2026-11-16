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

/**
 * self-configuration per component (MLIB-134).
 * Test resources:
 * <ul>
 * <li>component file .config/metabit/SELF_CONFIG_TEST/mconfig.properties: COMMENTS_READING=true, DESCRIPTION_ON_CREATE=false,
 *     TEST_MODE=true (allowed), ALLOW_APPLICATION_POLICY_LOCATIONS=true (refused, code only)</li>
 * <li>deprecated global file .config/metabit/mConfig/mconfig.properties: COMMENTS_WRITING=true, DESCRIPTION_ON_CREATE=true,
 *     TEST_MODE=true and ALLOW_MCONFIG_RUNTIME_SETTINGS=true (both refused for the global file)</li>
 * </ul>
 */
public class SelfConfigurationIntegrationTest
{
    private static final String COMPONENT       = "SELF_CONFIG_TEST";
    private static final String OTHER_COMPONENT = "OTHER_COMPONENT";

    @Test
    public void componentFileAppliesToItsOwnFactory() throws Exception
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", COMPONENT).build())
            {
            ConfigFactorySettings settings = getSettingsFromFactory(factory);
            assertTrue(settings.getBoolean(ConfigFeature.COMMENTS_READING));
            assertTrue(settings.getBoolean(ConfigFeature.TEST_MODE), "a component may set its own test mode in its file");
            }
    }

    @Test
    public void componentFileDoesNotAffectOtherComponents() throws Exception
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", OTHER_COMPONENT).build())
            {
            ConfigFactorySettings settings = getSettingsFromFactory(factory);
            assertFalse(settings.getBoolean(ConfigFeature.COMMENTS_READING), "another component's file must not apply");
            assertFalse(settings.getBoolean(ConfigFeature.TEST_MODE), "another component's file must not apply; the global file may not set it");
            }
    }

    @Test
    public void deprecatedGlobalFileStillAppliesOrdinaryFeatures() throws Exception
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", OTHER_COMPONENT).build())
            {
            ConfigFactorySettings settings = getSettingsFromFactory(factory);
            assertTrue(settings.getBoolean(ConfigFeature.COMMENTS_WRITING));
            assertTrue(settings.getBoolean(ConfigFeature.DESCRIPTION_ON_CREATE));
            assertFalse(settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS), "code only");
            }
    }

    @Test
    public void componentFileWinsOverGlobalFile() throws Exception
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", COMPONENT).build())
            {
            assertFalse(getSettingsFromFactory(factory).getBoolean(ConfigFeature.DESCRIPTION_ON_CREATE));
            }
    }

    @Test
    public void codeWinsOverFiles() throws Exception
    {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", COMPONENT);
        builder.setFeature(ConfigFeature.COMMENTS_READING, false);
        builder.setFeature(ConfigFeature.COMMENTS_WRITING, false);
        try (ConfigFactory factory = builder.build())
            {
            ConfigFactorySettings settings = getSettingsFromFactory(factory);
            assertFalse(settings.getBoolean(ConfigFeature.COMMENTS_READING), "code wins over the component file");
            assertFalse(settings.getBoolean(ConfigFeature.COMMENTS_WRITING), "code wins over the global file");
            }
    }

    @Test
    public void codeOnlyFeaturesRefused() throws Exception
    {
        try (ConfigFactory factory = ConfigFactoryBuilder.create("metabit", COMPONENT).build())
            {
            ConfigFactorySettings settings = getSettingsFromFactory(factory);
            assertFalse(settings.getBoolean(ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS));
            assertFalse(settings.getBoolean(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS));
            }
    }

    @Test
    public void testModeFromFileIsSubjectToTheGate() throws Exception
    {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", COMPONENT);
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, false); // lockdown in code
        try (ConfigFactory factory = builder.build())
            {
            assertFalse(getSettingsFromFactory(factory).getBoolean(ConfigFeature.TEST_MODE),
                    "self-configuration runs before the test-mode gates");
            }
    }

    @Test
    public void testDisableSelfConfiguration() throws Exception
    {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", COMPONENT);
        builder.setFeature(ConfigFeature.ENABLE_SELF_CONFIGURATION, false);
        try (ConfigFactory factory = builder.build())
            {
            ConfigFactorySettings settings = getSettingsFromFactory(factory);
            assertFalse(settings.getBoolean(ConfigFeature.COMMENTS_READING), "component file not read");
            assertFalse(settings.getBoolean(ConfigFeature.COMMENTS_WRITING), "global file not read");
            assertFalse(settings.getBoolean(ConfigFeature.TEST_MODE));
            }
    }

    private ConfigFactorySettings getSettingsFromFactory(ConfigFactory factory) throws Exception
    {
        Field ctxField = DefaultConfigFactory.class.getDeclaredField("ctx");
        ctxField.setAccessible(true);
        ConfigFactoryInstanceContext ctx = (ConfigFactoryInstanceContext) ctxField.get(factory);
        return ctx.getSettings();
    }
}
//___EOF___
