package org.metabit.platform.support.config.schema.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemaTemporalAccessorsTest
{

    @Test
    public void testNativeAccessors() throws ConfigCheckedException {
        ConfigEntry entry = new GenericConfigEntryLeaf("myDate", "2026-01-14", ConfigEntryType.DATE, new ConfigEntryMetadata(null));
        
        assertEquals(LocalDate.of(2026, 1, 14), entry.getValueAsLocalDate());
        
        ConfigEntry timeEntry = new GenericConfigEntryLeaf("myTime", "22:14:00", ConfigEntryType.TIME, new ConfigEntryMetadata(null));
        assertEquals(LocalTime.of(22, 14, 0), timeEntry.getValueAsLocalTime());
    }
}
