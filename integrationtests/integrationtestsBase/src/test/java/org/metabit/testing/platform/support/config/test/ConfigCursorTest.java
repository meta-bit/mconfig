package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigCursorTest
{

    @Test
    void testTestEnvironmentItself()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
                .build())
            {
            System.out.println("searching for ConfigCursorTest in these locations:");
            for (ConfigLocation location : factory.getSearchList())
                {
                System.out.println("\t"+location);
                }

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            if (cfg == null)
                {
                System.err.println("No configuration for "+CONFIG_NAME+" found");
                return;
                }
            System.out.println("Found configuration: "+cfg+" in ");
            for (ConfigLocation location : cfg.getSourceLocations())
                {
                System.out.println("\t"+location);
                }
            }
        return;
        }

    @Test
    void getCursor()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
                .build())
            {
            Configuration cfg = factory.getConfig("doesntexist");
            ConfigCursor cursor = cfg.getConfigCursor();
            assertNotNull(cursor);
            }
        return;
        }


    /*
     * @TODO for any further testing, we need a defined configuration this can work on.
     */
    @Test
    void testCursorBasics()
        {
        // this is supposed to read from config:[APPLICATION]:FileConfigStorage:/./src/test/resources/config/APPLICATION/cursorTest0.properties
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
/*
            assertFalse(cfg.isEmpty());
            ConfigCursor cursor = cfg.getConfigCursor();
            assertNotNull(cursor);

            assertEquals("value",cfg.getString("dummy"));
            // checking cursor basics
            assertFalse(cursor.isEmpty());
            */
/*
            List<ConfigLocation> searchList = factory.getSearchList();
            assertNotNull(searchList);
            assertFalse(searchList.isEmpty());
            for (ConfigLocation location : searchList)
                System.out.println(location.getURI("entry", null));
*/
            }
        return;
        }


    public static final  String COMPANY_NAME     = "metabit";
    public static final  String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME      = "cursorTest0";
}
