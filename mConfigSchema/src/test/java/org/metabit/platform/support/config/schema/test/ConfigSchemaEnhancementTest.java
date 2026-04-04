package org.metabit.platform.support.config.schema.test;
import org.metabit.platform.support.config.schema.*;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemaEnhancementTest
{

    @Test
    public void testRangeValidation() throws ConfigCheckedException {
        ConfigSchemaFactory factory = ConfigSchemaFactory.create();
        ConfigSchema scheme = factory.createSchema();
        
        // Test uint16
        ConfigSchemaEntry uint16Entry = factory.createEntry("port", ConfigEntryType.NUMBER);
        uint16Entry.setValidationPattern("uint16");
        scheme.addSchemaEntry(uint16Entry);

        assertTrue(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 80)));
        assertTrue(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 65535)));
        assertFalse(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 65536)));
        assertFalse(scheme.checkConfigEntryValidity("port", createNumberEntry("port", -1)));
        assertFalse(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 80.5)));

        // Test Interval [10, 20)
        ConfigSchemaEntry intervalEntry = factory.createEntry("range", ConfigEntryType.NUMBER);
        intervalEntry.setValidationPattern("[10, 20)");
        scheme.addSchemaEntry(intervalEntry);

        assertTrue(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 10)));
        assertTrue(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 15.5)));
        assertTrue(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 19.99)));
        assertFalse(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 20)));
        assertFalse(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 9.99)));
    }

    @Test
    public void testEnumValidation() throws ConfigCheckedException {
        ConfigSchemaFactory factory = ConfigSchemaFactory.create();
        ConfigSchema scheme = factory.createSchema();

        // Test ENUM
        ConfigSchemaEntry enumEntry = factory.createEntry("level", ConfigEntryType.ENUM);
        enumEntry.setValidationPattern("DEBUG|INFO|WARN");
        scheme.addSchemaEntry(enumEntry);

        assertTrue(scheme.checkConfigEntryValidity("level", createStringEntry("level", "DEBUG", ConfigEntryType.ENUM)));
        assertTrue(scheme.checkConfigEntryValidity("level", createStringEntry("level", "INFO", ConfigEntryType.ENUM)));
        assertFalse(scheme.checkConfigEntryValidity("level", createStringEntry("level", "ERROR", ConfigEntryType.ENUM)));

        // Test ENUM_SET
        ConfigSchemaEntry enumSetEntry = factory.createEntry("options", ConfigEntryType.ENUM_SET);
        enumSetEntry.setValidationPattern("OPT1|OPT2|OPT3");
        scheme.addSchemaEntry(enumSetEntry);

        assertTrue(scheme.checkConfigEntryValidity("options", createListEntry("options", Arrays.asList("OPT1", "OPT2"))));
        assertFalse(scheme.checkConfigEntryValidity("options", createListEntry("options", Arrays.asList("OPT1", "INVALID"))));
    }

    @Test
    public void testUnicodeRegexValidation() throws ConfigCheckedException {
        ConfigSchemaFactory factory = ConfigSchemaFactory.create();
        ConfigSchema scheme = factory.createSchema();

        ConfigSchemaEntry entry = factory.createEntry("name", ConfigEntryType.STRING);
        entry.setValidationPattern("\\p{L}+"); // Unicode letters
        scheme.addSchemaEntry(entry);

        assertTrue(scheme.checkConfigEntryValidity("name", createStringEntry("name", "John", ConfigEntryType.STRING)));
        assertTrue(scheme.checkConfigEntryValidity("name", createStringEntry("name", "Иван", ConfigEntryType.STRING))); // Cyrillic
        assertFalse(scheme.checkConfigEntryValidity("name", createStringEntry("name", "John123", ConfigEntryType.STRING)));
    }

    private ConfigEntry createNumberEntry(String key, Number value) {
        return new GenericConfigEntryLeaf(key, value, ConfigEntryType.NUMBER, null);
    }

    private ConfigEntry createStringEntry(String key, String value, ConfigEntryType type) {
        return new GenericConfigEntryLeaf(key, value, type, null);
    }

    private ConfigEntry createListEntry(String key, java.util.List<String> values) {
        return new GenericConfigEntryLeaf(key, values, ConfigEntryType.ENUM_SET, null);
    }
}
