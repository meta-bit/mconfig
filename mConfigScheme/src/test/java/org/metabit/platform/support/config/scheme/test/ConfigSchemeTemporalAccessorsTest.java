package org.metabit.platform.support.config.scheme.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemeTemporalAccessorsTest {

    @Test
    public void testNativeAccessors() throws ConfigCheckedException {
        ConfigEntry entry = new TypedConfigEntryLeaf("myDate", "2026-01-14", ConfigEntryType.DATE, new ConfigEntryMetadata(null));
        
        assertEquals(LocalDate.of(2026, 1, 14), entry.getValueAsLocalDate());
        
        ConfigEntry timeEntry = new TypedConfigEntryLeaf("myTime", "22:14:00", ConfigEntryType.TIME, new ConfigEntryMetadata(null));
        assertEquals(LocalTime.of(22, 14, 0), timeEntry.getValueAsLocalTime());
    }
}
