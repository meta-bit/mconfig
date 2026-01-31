package org.metabit.platform.support.config.impl.core;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.BasicConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLifecycleTest
{

    @Test
    void testConfigurationClose() throws Exception
    {
        ConfigFactory configFactory = ConfigFactoryBuilder.create("metabit", "LIFECYCLE_TEST").build();
        Configuration config = configFactory.getConfig("testConfig");
        
        assertFalse(config.isClosed());
        
        config.close();
        assertTrue(config.isClosed());
        
        assertThrows(IllegalStateException.class, () -> config.getString("someKey"));
        assertThrows(IllegalStateException.class, () -> config.isEmpty());
        assertThrows(IllegalStateException.class, () -> config.flush());
    }

    @Test
    void testFactoryCloseClosesConfigs() throws Exception
    {
        ConfigFactory configFactory = ConfigFactoryBuilder.create("metabit", "LIFECYCLE_TEST").build();
        Configuration config1 = configFactory.getConfig("config1");
        Configuration config2 = configFactory.getConfig("config2");
        
        configFactory.close();
        
        assertTrue(config1.isClosed());
        assertTrue(config2.isClosed());
        
        assertThrows(ConfigException.class, () -> configFactory.getConfig("config3"));
        assertThrows(ConfigException.class, () -> configFactory.listAvailableConfigurations());
    }

    @Test
    void testConfigurationDeregistrationOnClose() throws Exception
    {
        ConfigFactory configFactory = ConfigFactoryBuilder.create("metabit", "LIFECYCLE_TEST").build();
        Configuration config1 = configFactory.getConfig("testConfig");
        
        Configuration config1_again = configFactory.getConfig("testConfig");
        assertSame(config1, config1_again);
        
        config1.close();
        
        Configuration config1_new = configFactory.getConfig("testConfig");
        assertNotSame(config1, config1_new);
        assertFalse(config1_new.isClosed());
    }
}
