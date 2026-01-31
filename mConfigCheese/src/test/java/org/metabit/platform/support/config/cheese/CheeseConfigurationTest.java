package org.metabit.platform.support.config.cheese;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CheeseConfigurationTest
{
    @Test
    public void testGetWithDefault() throws Exception
        {
        // start the builder with required parameters
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "testApp");

        // we're in automated test mode: (don't use production-time directories etc)
        builder.setTestMode(true);
        // normally, you'd set the TEST_MODE feature of the builder
        // like this: builder.setFeature(ConfigFeature.TEST_MODE, true);

        // Ensure we can write to RUNTIME scope for testing
        builder.setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true);
        // use the builder to create a config factory
        ConfigFactory factory = builder.build();

        // get the actual config
        Configuration baseConfig = factory.getConfig("testConfig");

        // and now cheese it up
        CheeseConfiguration cheeseConfig = new CheeseConfiguration(baseConfig);
        // so, here's your cheese!
        //----------------------------------------------------------------------

        // Key does not exist, should return default
        assertEquals("defaultString", cheeseConfig.getString("non.existent.key", "defaultString"));
        assertEquals(true, cheeseConfig.getBoolean("non.existent.boolean", true));
        assertEquals(42, cheeseConfig.getInteger("non.existent.integer", 42));
        assertEquals(100L, cheeseConfig.getLong("non.existent.long", 100L));
        assertEquals(3.14, cheeseConfig.getDouble("non.existent.double", 3.14));

        // Now put something and check if it's retrieved
        cheeseConfig.put("existent.key", "realValue", org.metabit.platform.support.config.ConfigScope.SESSION);
        assertEquals("realValue", cheeseConfig.getString("existent.key", "defaultString"));

        // but really, use a default file in the resources directory, subdir .config - it's not that hard.
        }

}
