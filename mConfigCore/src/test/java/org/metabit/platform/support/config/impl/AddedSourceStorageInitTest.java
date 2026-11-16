package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.BootstrapConfigContext;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.source.core.InMemoryLayerSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * storages that come with sources added by the application are initialized by the factory
 * only if they ask for it (ConfigStorageInterface.isInitializedWhenAddedAsSource, MLIB-136).
 */
@SuppressWarnings("removal")
class AddedSourceStorageInitTest
{
    /* counts the lifecycle calls; init() deliberately does not register a search-list entry */
    static class CountingStorage extends InMemoryLayerSource
    {
        private final boolean optIn;
        boolean active = true;
        int inits;
        int postInits;
        int exits;

        CountingStorage(boolean optIn)
            {
            super(ConfigScope.APPLICATION);
            this.optIn = optIn;
            }

        @Override
        public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
            {
            return active;
            }

        @Override
        public boolean init(ConfigFactoryInstanceContext ctx)
            {
            inits++;
            return true;
            }

        @Override
        public void postInit(BootstrapConfigContext bootstrapCtx)
            {
            postInits++;
            }

        @Override
        public void exit()
            {
            exits++;
            }

        @Override
        @SuppressWarnings("removal")
        public boolean isInitializedWhenAddedAsSource()
            {
            return optIn;
            }
    }

    @Test
    void optedInStorageIsInitializedOnceAndClosed()
        {
        CountingStorage storage = new CountingStorage(true);
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "ADDED_SOURCE_TEST");
        builder.setTestMode(true);
        // two sources sharing one storage instance: initialized once
        builder.addSource(new ConfigLocationImpl(ConfigScope.APPLICATION, storage, null, "a"));
        builder.addSource(new ConfigLocationImpl(ConfigScope.APPLICATION, storage, null, "b"));
        ConfigFactory factory = builder.build();
        assertEquals(1, storage.inits);
        assertEquals(1, storage.postInits);
        assertEquals(2, factory.getSearchList().stream().filter(l->l.getStorage() == storage).count());
        factory.close();
        assertEquals(1, storage.exits);
        }

    @Test
    void storageWithoutOptInIsLeftAlone()
        {
        CountingStorage storage = new CountingStorage(false);
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "ADDED_SOURCE_TEST");
        builder.setTestMode(true);
        builder.addSource(new ConfigLocationImpl(ConfigScope.APPLICATION, storage, null, "a"));
        ConfigFactory factory = builder.build();
        factory.close();
        assertEquals(0, storage.inits);
        assertEquals(0, storage.postInits);
        assertEquals(0, storage.exits);
        }

    /* e.g. Vault or AWS in test mode without their BYPASS_TEST feature: no real endpoint reached */
    @Test
    void storageRefusingTestIsNotInitializedAndItsSourceNotUsed()
        {
        CountingStorage storage = new CountingStorage(true);
        storage.active = false;
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "ADDED_SOURCE_TEST");
        builder.setTestMode(true);
        builder.addSource(new ConfigLocationImpl(ConfigScope.APPLICATION, storage, null, "a"));
        ConfigFactory factory = builder.build();
        assertEquals(0, storage.inits);
        assertEquals(0, factory.getSearchList().stream().filter(l->l.getStorage() == storage).count());
        factory.close();
        assertEquals(0, storage.exits);
        }
}
//___EOF___
