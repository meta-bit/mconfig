package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.mockups.MockConfigFactory;

import org.metabit.platform.support.config.source.core.InMemoryLayer;
import org.metabit.platform.support.config.source.core.InMemoryLayerSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ChangeNotificationTest
{
    @Test
    void testBasicNotification() throws InterruptedException
        {
        MockConfigFactory factory = new MockConfigFactory();
        Configuration cfg = factory.getConfig("test");
        LayeredConfiguration layeredCfg = (LayeredConfiguration) cfg;

        // Ensure we have an entry.
        cfg.put("testkey", "initial", ConfigScope.RUNTIME);
        
        // Initial check to prime the EntryChangeChecker
        layeredCfg.changeChecker.checkAndNotify(layeredCfg.configs, layeredCfg.changeNotifier);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ConfigLocation> notifiedLocation = new AtomicReference<>();
        
        cfg.subscribeToUpdates(loc -> 
            {
            notifiedLocation.set(loc);
            latch.countDown();
            });
            
        // Now change the value
        cfg.put("testkey", "changed", ConfigScope.RUNTIME);
        
        // InMemoryLayerSource requires manual trigger of change flag for this test
        InMemoryLayer layer = null;
        for (org.metabit.platform.support.config.interfaces.ConfigLayerInterface l : layeredCfg.configs)
            {
            if (l instanceof InMemoryLayer)
                {
                layer = (InMemoryLayer) l;
                break;
                }
            }
        assertNotNull(layer);
        InMemoryLayerSource source = (InMemoryLayerSource) layer.getSource().getStorage();
        source.triggerChangeCheck(null);
        
        // Now run the checker again
        layeredCfg.changeChecker.checkAndNotify(layeredCfg.configs, layeredCfg.changeNotifier);
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Notification should be received after change");
        assertEquals(layer.getSource(), notifiedLocation.get());
        }

    @Test
    void testUnsubscribe() throws InterruptedException
        {
        MockConfigFactory factory = new MockConfigFactory();
        Configuration cfg = factory.getConfig("test");
        LayeredConfiguration layeredCfg = (LayeredConfiguration) cfg;

        CountDownLatch latch = new CountDownLatch(1);
        java.util.function.Consumer<ConfigLocation> listener = loc -> latch.countDown();
        
        cfg.subscribeToUpdates(listener);
        cfg.unsubscribeFromUpdates(listener);
            
        // Trigger a change
        // MockConfigFactory manually adds an InMemoryLayerSource to search list
        ConfigLocation loc = factory.getSearchList().get(0);
        InMemoryLayerSource source = (InMemoryLayerSource) loc.getStorage();
        
        assertNotNull(source);
        source.triggerChangeCheck(null);
        layeredCfg.changeChecker.checkAndNotify(layeredCfg.configs, layeredCfg.changeNotifier);
        
        assertFalse(latch.await(1, TimeUnit.SECONDS), "Notification should NOT be received after unsubscribe");
        }
}
