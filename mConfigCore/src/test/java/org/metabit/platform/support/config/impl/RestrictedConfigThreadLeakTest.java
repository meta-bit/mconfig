package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.interfaces.BootstrapConfigContext;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bootstrap lookups via getRestrictedConfig() are neither tracked nor closed by the factory,
 * so they must not start a change checker thread; storages call this once per postInit().
 */
public class RestrictedConfigThreadLeakTest
{
    private static long countChangeCheckerThreads()
        {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && "mconfig-change-checker".equals(t.getName()))
                .count();
        }

    @Test
    public void restrictedConfigStartsNoChangeCheckerThread() throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "RESTRICTED_LEAK_TEST");
        builder.setTestMode(true);
        try (ConfigFactory factory = builder.build())
            {
            long before = countChangeCheckerThreads();
            for (int i = 0; i < 5; i++)
                {
                ((BootstrapConfigContext) factory).getRestrictedConfig("bootstrap", EnumSet.of(ConfigScope.PRODUCT, ConfigScope.HOST, ConfigScope.USER, ConfigScope.APPLICATION));
                }
            assertEquals(before, countChangeCheckerThreads());
            }
        }
}
//___EOF___
