package org.metabit.platform.support.config.impl.logging;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LogSafeTest
{
    @Test
    void plainTextIsUnchanged()
        {
        String s = "/home/user/.config/metabit/app/network.properties";
        assertSame(s, LogSafe.text(s));
        assertFalse(LogSafe.containsUnsafeCharacters(s));
        }

    @Test
    void windowsPathAndNonAsciiAreUnchanged()
        {
        assertEquals("C:\\Users\\Jörg\\AppData\\Roaming\\metabit", LogSafe.text("C:\\Users\\Jörg\\AppData\\Roaming\\metabit"));
        assertEquals("/srv/конфиг/設定", LogSafe.text("/srv/конфиг/設定"));
        }

    @Test
    void lineBreaksCannotForgeLogLines()
        {
        String forged = "a.properties\n2026-10-15 INFO [mConfig] everything fine\r";
        String rendered = LogSafe.text(forged);
        assertEquals("a.properties\\n2026-10-15 INFO [mConfig] everything fine\\r", rendered);
        assertFalse(rendered.contains("\n"));
        assertFalse(rendered.contains("\r"));
        assertTrue(LogSafe.containsUnsafeCharacters(forged));
        }

    @Test
    void terminalEscapesAndOtherControlsAreEscaped()
        {
        assertEquals("x\\u001b[2Jy", LogSafe.text("x\u001b[2Jy"));
        assertEquals("tab\\there", LogSafe.text("tab\there"));
        assertEquals("nul\\u0000", LogSafe.text("nul\u0000"));
        assertEquals("del\\u007f c1\\u009b", LogSafe.text("del\u007f c1\u009b"));
        }

    @Test
    void unicodeSeparatorsAndBidiControlsAreEscaped()
        {
        assertEquals("a\\u2028b\\u2029c", LogSafe.text("a\u2028b\u2029c"));
        assertEquals("cfg\\u202egpj.exe", LogSafe.text("cfg\u202egpj.exe"));
        assertEquals("\\u2066x\\u2069", LogSafe.text("\u2066x\u2069"));
        }

    @Test
    void acceptsPathsAndNull()
        {
        java.nio.file.Path p = Paths.get("tmp", "x");
        assertEquals(p.toString(), LogSafe.text(p));
        assertEquals("null", LogSafe.text(null));
        assertFalse(LogSafe.containsUnsafeCharacters(null));
        }
}
//___EOF___
