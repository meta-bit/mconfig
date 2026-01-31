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

public class EntryChangeNotificationTest
{
    @Test
    void testEntrySpecificNotification() throws InterruptedException
        {
        MockConfigFactory factory = new MockConfigFactory();
        Configuration cfg = factory.getConfig("test");
        LayeredConfiguration layeredCfg = (LayeredConfiguration) cfg;

        // Ensure we have entries.
        cfg.put("targetKey", "initial1", ConfigScope.RUNTIME);
        cfg.put("otherKey", "initial2", ConfigScope.RUNTIME);
        
        // Initial check to prime the EntryChangeChecker
        layeredCfg.changeChecker.checkAndNotify(layeredCfg.configs, layeredCfg.changeNotifier);

        CountDownLatch targetLatch = new CountDownLatch(1);
        CountDownLatch otherLatch = new CountDownLatch(1);
        AtomicReference<ConfigLocation> notifiedLocation = new AtomicReference<>();
        
        cfg.subscribeToUpdates("targetKey", loc -> 
            {
            notifiedLocation.set(loc);
            targetLatch.countDown();
            });
            
        cfg.subscribeToUpdates("otherKey", loc -> 
            {
            otherLatch.countDown();
            });
            
        // Now change only targetKey
        cfg.put("targetKey", "changed", ConfigScope.RUNTIME);
        
        // InMemoryLayerSource requires manual trigger of change flag
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
        
        assertTrue(targetLatch.await(5, TimeUnit.SECONDS), "Notification for targetKey should be received");
        assertFalse(otherLatch.await(1, TimeUnit.SECONDS), "Notification for otherKey should NOT be received");
        assertEquals(layer.getSource(), notifiedLocation.get());
        }

    @Test
    void testNewEntryNotification() throws InterruptedException
        {
        MockConfigFactory factory = new MockConfigFactory();
        Configuration cfg = factory.getConfig("test");
        LayeredConfiguration layeredCfg = (LayeredConfiguration) cfg;

        CountDownLatch latch = new CountDownLatch(1);
        
        // Subscribe to a key that does not exist yet
        cfg.subscribeToUpdates("newKey", loc -> latch.countDown());
            
        // Create the key
        cfg.put("newKey", "firstValue", ConfigScope.RUNTIME);
        
        // Trigger change
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
        
        // Run the checker
        layeredCfg.changeChecker.checkAndNotify(layeredCfg.configs, layeredCfg.changeNotifier);
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Notification for new key creation should be received");
        }
}
