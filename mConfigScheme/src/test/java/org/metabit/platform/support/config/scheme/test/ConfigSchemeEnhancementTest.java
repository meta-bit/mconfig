package org.metabit.platform.support.config.scheme.test;
import org.metabit.platform.support.config.scheme.*;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemeEnhancementTest {

    @Test
    public void testRangeValidation() throws ConfigCheckedException {
        ConfigSchemeFactory factory = ConfigSchemeFactory.create();
        ConfigScheme scheme = factory.createScheme();
        
        // Test uint16
        ConfigSchemeEntry uint16Entry = factory.createEntry("port", ConfigEntryType.NUMBER);
        uint16Entry.setValidationPattern("uint16");
        scheme.addSchemeEntry(uint16Entry);

        assertTrue(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 80)));
        assertTrue(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 65535)));
        assertFalse(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 65536)));
        assertFalse(scheme.checkConfigEntryValidity("port", createNumberEntry("port", -1)));
        assertFalse(scheme.checkConfigEntryValidity("port", createNumberEntry("port", 80.5)));

        // Test Interval [10, 20)
        ConfigSchemeEntry intervalEntry = factory.createEntry("range", ConfigEntryType.NUMBER);
        intervalEntry.setValidationPattern("[10, 20)");
        scheme.addSchemeEntry(intervalEntry);

        assertTrue(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 10)));
        assertTrue(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 15.5)));
        assertTrue(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 19.99)));
        assertFalse(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 20)));
        assertFalse(scheme.checkConfigEntryValidity("range", createNumberEntry("range", 9.99)));
    }

    @Test
    public void testEnumValidation() throws ConfigCheckedException {
        ConfigSchemeFactory factory = ConfigSchemeFactory.create();
        ConfigScheme scheme = factory.createScheme();

        // Test ENUM
        ConfigSchemeEntry enumEntry = factory.createEntry("level", ConfigEntryType.ENUM);
        enumEntry.setValidationPattern("DEBUG|INFO|WARN");
        scheme.addSchemeEntry(enumEntry);

        assertTrue(scheme.checkConfigEntryValidity("level", createStringEntry("level", "DEBUG", ConfigEntryType.ENUM)));
        assertTrue(scheme.checkConfigEntryValidity("level", createStringEntry("level", "INFO", ConfigEntryType.ENUM)));
        assertFalse(scheme.checkConfigEntryValidity("level", createStringEntry("level", "ERROR", ConfigEntryType.ENUM)));

        // Test ENUM_SET
        ConfigSchemeEntry enumSetEntry = factory.createEntry("options", ConfigEntryType.ENUM_SET);
        enumSetEntry.setValidationPattern("OPT1|OPT2|OPT3");
        scheme.addSchemeEntry(enumSetEntry);

        assertTrue(scheme.checkConfigEntryValidity("options", createListEntry("options", Arrays.asList("OPT1", "OPT2"))));
        assertFalse(scheme.checkConfigEntryValidity("options", createListEntry("options", Arrays.asList("OPT1", "INVALID"))));
    }

    @Test
    public void testUnicodeRegexValidation() throws ConfigCheckedException {
        ConfigSchemeFactory factory = ConfigSchemeFactory.create();
        ConfigScheme scheme = factory.createScheme();

        ConfigSchemeEntry entry = factory.createEntry("name", ConfigEntryType.STRING);
        entry.setValidationPattern("\\p{L}+"); // Unicode letters
        scheme.addSchemeEntry(entry);

        assertTrue(scheme.checkConfigEntryValidity("name", createStringEntry("name", "John", ConfigEntryType.STRING)));
        assertTrue(scheme.checkConfigEntryValidity("name", createStringEntry("name", "Иван", ConfigEntryType.STRING))); // Cyrillic
        assertFalse(scheme.checkConfigEntryValidity("name", createStringEntry("name", "John123", ConfigEntryType.STRING)));
    }

    private ConfigEntry createNumberEntry(String key, Number value) {
        return new StringConfigEntryLeaf(key, String.valueOf(value), null) {
            @Override public ConfigEntryType getType() { return ConfigEntryType.NUMBER; }
        };
    }

    private ConfigEntry createStringEntry(String key, String value, ConfigEntryType type) {
        return new StringConfigEntryLeaf(key, value, null) {
            @Override public ConfigEntryType getType() { return type; }
        };
    }

    private ConfigEntry createListEntry(String key, java.util.List<String> values) {
        return new TypedConfigEntryLeaf(key, values, ConfigEntryType.ENUM_SET, null);
    }
}
