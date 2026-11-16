package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.source.core.InMemoryLayerSource;

import static org.junit.jupiter.api.Assertions.*;

class PolicyLocationGuardTest
{
    @Test
    void nonPolicyScopesPass()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        for (ConfigScope scope : ConfigScope.values())
            {
            if (scope != ConfigScope.POLICY)
                {
                assertDoesNotThrow(()->PolicyLocationGuard.checkApplicationLocation(settings, scope, "x"), scope.name());
                }
            }
        }

    @Test
    void policyRefusedInProductiveMode()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigException e = assertThrows(ConfigException.class, ()->PolicyLocationGuard.checkApplicationLocation(settings, ConfigScope.POLICY, "test entry"));
        assertEquals(ConfigException.ConfigExceptionReason.ARGUMENT_REFUSED, e.getReason());
        assertTrue(e.getMessage().contains("ALLOW_APPLICATION_POLICY_LOCATIONS"), e.getMessage());
        }

    @Test
    void policyAllowedInTestMode()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setBoolean(ConfigFeature.TEST_MODE, true);
        assertDoesNotThrow(()->PolicyLocationGuard.checkApplicationLocation(settings, ConfigScope.POLICY, "x"));
        }

    @Test
    void policyAllowedWithOptIn()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setBoolean(ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS, true);
        assertDoesNotThrow(()->PolicyLocationGuard.checkApplicationLocation(settings, ConfigScope.POLICY, "x"));
        }

    @Test
    void builderAddSourceWithPolicyFailsInProductiveMode()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "POLICY_GUARD_TEST");
        builder.addSource(new ConfigLocationImpl(ConfigScope.POLICY, new InMemoryLayerSource(), null, null));
        assertThrows(ConfigException.class, builder::build);
        }

    @Test
    void builderAddSourceWithPolicyAllowedWithOptIn()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "POLICY_GUARD_TEST");
        builder.setFeature(ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS, true);
        builder.addSource(new ConfigLocationImpl(ConfigScope.POLICY, new InMemoryLayerSource(), null, null));
        assertDoesNotThrow(()->builder.build().close());
        }
}
//___EOF___
