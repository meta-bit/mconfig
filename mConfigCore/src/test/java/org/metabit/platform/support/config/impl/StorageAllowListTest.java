package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2 (MLIB-138): STORAGE_TYPE_ALLOW_ALL_STORAGES=false restricts the storages to those covered by STORAGE_TYPE_PRIORITIES.
 */
class StorageAllowListTest
{
    @Test
    void entriesCoverIdAndPrefixedFamily()
        {
        List<String> list = Arrays.asList("files", "secrets");
        assertTrue(DefaultConfigFactoryBuilder.isStorageListed("files", list));
        assertTrue(DefaultConfigFactoryBuilder.isStorageListed("secrets.vault", list));
        assertFalse(DefaultConfigFactoryBuilder.isStorageListed("filesystem", list), "a plain prefix is not a family");
        assertFalse(DefaultConfigFactoryBuilder.isStorageListed("envvar", list));
        assertFalse(DefaultConfigFactoryBuilder.isStorageListed("envvar", null));
        }

    @Test
    void coreStoragesAreAlwaysAllowed()
        {
        List<String> none = Collections.emptyList();
        assertTrue(DefaultConfigFactoryBuilder.isStorageListed("defaults", none));
        assertTrue(DefaultConfigFactoryBuilder.isStorageListed("RAM-RUNTIME", none));
        assertTrue(DefaultConfigFactoryBuilder.isStorageListed("RAM-SESSION", none));
        }

    @Test
    void factoryKeepsOnlyListedStorages() throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "STORAGE_ALLOW_LIST_TEST")
                .setFeature(ConfigFeature.STORAGE_TYPE_ALLOW_ALL_STORAGES, false)
                .setFeature(ConfigFeature.STORAGE_TYPE_PRIORITIES, Collections.singletonList("files"));
        builder.setTestMode(true);
        try (ConfigFactory factory = builder.build())
            {
            Set<String> ids = ((DefaultConfigFactory) factory).ctx.getConfigStorages().keySet();
            for (String id : ids)
                { assertTrue(id.equals("files") || id.equals("defaults") || id.startsWith("RAM-"), "unexpected storage "+id); }
            assertTrue(ids.contains("RAM-RUNTIME"), ids.toString());
            }
        }
}
//___EOF___
