package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * a POLICY: prefix in the additional-directories settings needs the code-only opt-in in productive mode (MLIB-134).
 */
class PolicyDirectoryGuardTest
{
    private static final String POLICY_DIR = "POLICY:/nonexistent/mconfig-policy-guard-test";

    @Test
    void policyPrefixRefusedInProductiveMode()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "POLICY_DIR_GUARD_TEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(POLICY_DIR));
        assertThrows(ConfigException.class, builder::build);
        }

    @Test
    void policyPrefixAllowedWithOptIn()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "POLICY_DIR_GUARD_TEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(POLICY_DIR));
        builder.setFeature(ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS, true);
        assertDoesNotThrow(()->builder.build().close());
        }

    @Test
    void otherScopePrefixesUnaffected()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "POLICY_DIR_GUARD_TEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of("HOST:/nonexistent/mconfig-policy-guard-test"));
        assertDoesNotThrow(()->builder.build().close());
        }
}
//___EOF___
