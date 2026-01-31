package org.metabit.platform.support.config.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * this test need the test mode, and resources in the test directories.
 * as opposed to the test creating temp files in the runtime scope,
 * it depends on the correct contents being available in the "resources"
 * folder, and the test runtime environment accessing these correctly.
 *
 * We expect
 * * PRODUCT to be the general setting, and fallback for the others
 * * APPLICATION to be the more specific application-installed settings, overruling PRODUCT
 * * USER to be the most specific of these three, overriding the others.
 *
 * TEST_MODE is activated/permitted, so we get the resource/ configs from the test
 */
class TestLayerIndividualAccessTestMode
{

    public static final String               COMPANY_NAME     = "metabit";
    public static final String               APPLICATION_NAME = "CONFIGTEST";
    private static      ConfigFactoryBuilder configFactoryBuilder;


    @BeforeAll
    static void init()
            throws ConfigCheckedException, IOException
        {
        // add temp subdirectory to search path in builder?
        configFactoryBuilder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        configFactoryBuilder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        return;
        }

    @AfterAll
    static void exit()
        {
        }

    @Test
    @DisplayName("test mode resource layer test")
    void testConfigAccessSingleStringEntriesHierarchy()
            throws ConfigCheckedException
        {
        // activate the test
        configFactoryBuilder.setFeature(ConfigFeature.TEST_MODE, true);
        // set logging to full
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);

        ConfigFactory configFactory = configFactoryBuilder.build();
        for (ConfigLocation cfgLoc : configFactory.getSearchList())
            System.out.println(cfgLoc);

        Configuration cfg = configFactory.getConfig("testconfig"); // from test-properties file

        String value = cfg.getString("testdata");
        assertEquals("user scope", value); // we expect the most specific entry here

        // we expect to get the "visible" entries from each level, and the "obscured" ones from other levels to be obscured
        value = cfg.getString("productSpecific");
        assertEquals("visible", value);

        // we may have to adjust the levels of default layer and other application layers
        value = cfg.getString("applicationSpecific");
// turning off this test for now.
        assertEquals("visible", value);

        value = cfg.getString("userSpecific");
        assertEquals("visible", value);

        // provoke entry not found exception
        ConfigException thrown = assertThrows(ConfigException.class, ()->
            {
            String dummy = cfg.getString("ThisKeyDoesNotExist");
            System.out.println(dummy);
            });
        assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, thrown.getReason());
        }

}
//___EOF___
