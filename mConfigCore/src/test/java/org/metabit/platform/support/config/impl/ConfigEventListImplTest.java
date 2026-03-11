package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEvent;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigEventListImplTest
{
    @Test
    public void testCapping()
        {
        ConfigEventListImpl list = new ConfigEventListImpl(2, 0); // cap=2, dedup=off
        
        ConfigEvent e1 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("e1")
                .build();
        ConfigEvent e2 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("e2")
                .build();
        ConfigEvent e3 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("e3")
                .build();

        list.add(e1);
        list.add(e2);
        assertEquals(2, list.size());
        
        list.add(e3);
        assertEquals(2, list.size());
        assertEquals("e2", list.get(0).getMessage());
        assertEquals("e3", list.get(1).getMessage());
        }

    @Test
    public void testRingDedup()
        {
        ConfigEventListImpl list = new ConfigEventListImpl(100, 2); // dedup ring size 2
        
        ConfigEvent e1 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("msg")
                .build();
        ConfigEvent e2 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("other")
                .build();

        assertTrue(list.add(e1));
        assertFalse(list.add(e1), "Should skip duplicate");
        assertEquals(1, list.size());

        assertTrue(list.add(e2));
        assertEquals(2, list.size());

        // ring size is 2. [e1, e2]. adding e3 pushes e1 out of dedup ring.
        ConfigEvent e3 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("third")
                .build();
        assertTrue(list.add(e3));
        
        // now e1 should be acceptable again for dedup (though it's still in the full list)
        assertTrue(list.add(e1), "e1 should be acceptable again as it left the dedup ring");
        assertEquals(4, list.size());
        }

    @Test
    public void testClean()
        {
        ConfigEventListImpl list = new ConfigEventListImpl(10, 5);
        ConfigEvent e = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.INFO)
                .domain(ConfigEvent.Domain.OTHER)
                .kind(ConfigEvent.Kind.MISC)
                .message("msg")
                .build();
        
        list.add(e);
        assertFalse(list.add(e)); // deduped
        
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.add(e), "Ring should be cleared too");
        }
}
