package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigNameSanitizerTest
{
    @Test
    void normalizesWhitespace()
        {
        assertEquals("network", ConfigNameSanitizer.sanitize("  network "));
        assertEquals("my config", ConfigNameSanitizer.sanitize("my \t  config"));
        assertEquals("", ConfigNameSanitizer.sanitize(""));
        assertNull(ConfigNameSanitizer.sanitizeNullable(null));
        }

    @Test
    void acceptsOrdinaryNamesIncludingDots()
        {
        for (String name : new String[]{"network", "app-settings", "app_settings", "app.v2", ".hidden", "...", "trailing."})
            {
            assertTrue(ConfigNameSanitizer.isValid(name), name);
            assertEquals(name, ConfigNameSanitizer.sanitize(name));
            }
        }

    @Test
    void rejectsPathOperators()
        {
        for (String name : new String[]{".", "..", " .. ", "\t.\n"})
            {
            assertFalse(ConfigNameSanitizer.isValid(name), "'"+name+"'");
            assertThrows(ConfigException.class, ()->ConfigNameSanitizer.sanitize(name), "'"+name+"'");
            }
        }

    @Test
    void rejectsPathAndShellCharacters()
        {
        for (String name : new String[]{"a/b", "a\\b", "../etc", "%WINDIR%", "${HOME}", "`id`", "a:b", "a\"b", "a*b"})
            {
            assertFalse(ConfigNameSanitizer.isValid(name), name);
            assertThrows(ConfigException.class, ()->ConfigNameSanitizer.sanitize(name), name);
            }
        }

    @Test
    void isValidAgreesWithSanitize()
        {
        // isValid used to replace spaces with dashes before checking; both now share one normalization
        assertTrue(ConfigNameSanitizer.isValid("my config"));
        assertEquals("my config", ConfigNameSanitizer.sanitize("my config"));
        assertFalse(ConfigNameSanitizer.isValid("   "));
        assertThrows(ConfigException.class, ()->ConfigNameSanitizer.sanitize("   "));
        }

    @Test
    void nullIsRejected()
        {
        assertFalse(ConfigNameSanitizer.isValid(null));
        assertThrows(NullPointerException.class, ()->ConfigNameSanitizer.sanitize(null));
        }
}
//___EOF___
