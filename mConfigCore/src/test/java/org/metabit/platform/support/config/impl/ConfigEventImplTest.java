package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigEventImplTest
{
    @Test
    public void testBuilderAndImmutability()
        {
        Instant now = Instant.now();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("key", "value");

        ConfigEventImpl event = ConfigEventImpl.builder()
                .timestamp(now)
                .severity(ConfigEvent.Severity.WARNING)
                .domain(ConfigEvent.Domain.PARSE)
                .kind(ConfigEvent.Kind.UNSUPPORTED_SYNTAX)
                .message("test message")
                .attributes(attrs)
                .build();

        assertEquals(now, event.getTimestamp());
        assertEquals(ConfigEvent.Severity.WARNING, event.getSeverity());
        assertEquals(ConfigEvent.Domain.PARSE, event.getDomain());
        assertEquals(ConfigEvent.Kind.UNSUPPORTED_SYNTAX, event.getKind());
        assertEquals("test message", event.getMessage());
        assertEquals("value", event.getAttributes().get("key"));

        // test immutability of attributes
        attrs.put("key", "new value");
        assertEquals("value", event.getAttributes().get("key"));

        try
            {
            event.getAttributes().put("other", "val");
            fail("Attributes map should be unmodifiable");
            }
        catch (UnsupportedOperationException ignored) {}
        }

    @Test
    public void testEqualityForDedup()
        {
        ConfigEventImpl e1 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.ERROR)
                .domain(ConfigEvent.Domain.PARSE)
                .kind(ConfigEvent.Kind.FAILED_GENERIC)
                .message("fail")
                .build();

        // Same content, different timestamp
        ConfigEventImpl e2 = ConfigEventImpl.builder()
                .timestamp(Instant.now().plusSeconds(10))
                .severity(ConfigEvent.Severity.ERROR)
                .domain(ConfigEvent.Domain.PARSE)
                .kind(ConfigEvent.Kind.FAILED_GENERIC)
                .message("fail")
                .build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());

        // Different message
        ConfigEventImpl e3 = ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.ERROR)
                .domain(ConfigEvent.Domain.PARSE)
                .kind(ConfigEvent.Kind.FAILED_GENERIC)
                .message("different")
                .build();

        assertNotEquals(e1, e3);
        }
}
