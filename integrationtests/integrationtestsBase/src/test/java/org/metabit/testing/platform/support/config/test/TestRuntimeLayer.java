package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.util.ConfigUtil;

import static org.junit.jupiter.api.Assertions.*;


public class TestRuntimeLayer
{
    //
    public static final String COMPANY_NAME = "metabit";
    public static final String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME     = "testconfig";

    @BeforeEach
    void initTestPreparedValuesAsExpected()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true) // use the resource *test* directories
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("visible", cfg.getString("userSpecific"));
            // of all the "hardcoded" test data
            assertEquals("user scope", cfg.getString("testdata"));
            }
        }

    @Test
    void testRuntimeSettings()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true) // use the resource *test* directories
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            // make sure the hardcoded settings are as expected
            String value = cfg.getString("testdata");
            assertEquals("user scope", value);

            ConfigUtil.printConfigPaths(COMPANY_NAME,APPLICATION_NAME);

//@TODO check: scope check for writing seems incomplete!
            // set some of our own
            cfg.put("testdata", "a changed value", ConfigScope.SESSION);

            // read them back
            assertEquals("a changed value", cfg.getString("testdata"));

            // change again, check again
            cfg.put("testdata", "changed again", ConfigScope.SESSION);
            assertEquals("changed again", cfg.getString("testdata"));
            }

        // re-instantiate, and we'll see that above change did not affect this one.
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true) // use the resource *test* directories
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            // make sure the hardcoded settings are as expected
            String value = cfg.getString("testdata");
            assertEquals("user scope", cfg.getString("testdata"));
            }

        return;
        }



    @AfterEach
    void checkValuesUnchanged()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true) // use the resource *test* directories
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            // "testdata" entry not changed
            assertEquals("user scope", cfg.getString("testdata"));
            }
        }

}
