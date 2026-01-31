package org.metabit.platform.support.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigScopeTest
{
 // for safety - this is supposed to be always true, but for debugging purposes,
 // we want certainty. (Also, when renaming this needs to be checked.)
    // we perform transitive order checks, so the ones below should cover all cases.

 @Test void validateConfigScopeOrdinalOrder()
     {
     // transitive order check; if all pass, all other combinations are OK, too.
     assert(ConfigScope.PRODUCT.ordinal() < ConfigScope.ORGANIZATION.ordinal());
     assert(ConfigScope.ORGANIZATION.ordinal() < ConfigScope.CLUSTER.ordinal());
     assert(ConfigScope.CLUSTER.ordinal() < ConfigScope.HOST.ordinal());
     assert(ConfigScope.HOST.ordinal() < ConfigScope.APPLICATION.ordinal());
     assert(ConfigScope.APPLICATION.ordinal() < ConfigScope.USER.ordinal());
     assert(ConfigScope.USER.ordinal() < ConfigScope.SESSION.ordinal());
     assert(ConfigScope.SESSION.ordinal() < ConfigScope.RUNTIME.ordinal());
     }


    @Test
    void validateConfigScopeComparisonOrder()
        {
        assertTrue(ConfigScope.PRODUCT.compareTo(ConfigScope.ORGANIZATION) < 0);
        assertTrue(ConfigScope.ORGANIZATION.compareTo(ConfigScope.CLUSTER) < 0);
        assertTrue(ConfigScope.CLUSTER.compareTo(ConfigScope.HOST) < 0);
        assertTrue(ConfigScope.HOST.compareTo(ConfigScope.APPLICATION) < 0);
        assertTrue(ConfigScope.APPLICATION.compareTo(ConfigScope.USER) < 0);
        assertTrue(ConfigScope.USER.compareTo(ConfigScope.SESSION) < 0);
        assertTrue(ConfigScope.SESSION.compareTo(ConfigScope.RUNTIME) < 0);
        }
}