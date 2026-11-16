package org.metabit.platform.support.config.impl.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigIOUtilTest
{
    private static final byte[] DEADBEEF = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};

    @Test
    void decodesPlainHex()
        {
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode("DEADBEEF"));
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode("deadbeef"));
        assertArrayEquals(new byte[]{0x00, 0x7F, (byte) 0x80, (byte) 0xFF}, ConfigIOUtil.hexDecode("007F80FF"));
        }

    @Test
    void acceptsPrefixAndSeparatorsBetweenBytes()
        {
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode("0xDEADBEEF"));
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode("0XDEADBEEF"));
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode("DE AD BE EF"));
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode("de:ad:be:ef"));
        assertArrayEquals(DEADBEEF, ConfigIOUtil.hexDecode(" DE\tAD\r\nBE   EF\n"));
        }

    @Test
    void rejectsInsteadOfTruncating()
        {
        assertNull(ConfigIOUtil.hexDecode(null));
        assertNull(ConfigIOUtil.hexDecode(""));
        assertNull(ConfigIOUtil.hexDecode("0x"));
        assertNull(ConfigIOUtil.hexDecode("   "));
        assertNull(ConfigIOUtil.hexDecode("DEADBEE"), "odd number of hex digits");
        assertNull(ConfigIOUtil.hexDecode("DEADBEEG"), "invalid character");
        assertNull(ConfigIOUtil.hexDecode("hello"), "text must not decode to its few hex-like letters");
        assertNull(ConfigIOUtil.hexDecode("D EADBEEF"), "separator inside a byte");
        assertNull(ConfigIOUtil.hexDecode("DEADBEEF  ; comment"), "trailing text");
        assertNull(ConfigIOUtil.hexDecode("０１"), "fullwidth digits are not hex");
        }

    @Test
    void utf8ToCharsDecodesNonAscii()
        {
        // SR2: the old (char) bytes[i] cast turned every byte >= 0x80 into U+FFxx
        String text = "päss€🔑"; // ä, €, and a surrogate pair
        assertArrayEquals(text.toCharArray(), ConfigIOUtil.utf8ToChars(text.getBytes(StandardCharsets.UTF_8)));
        assertArrayEquals("plain".toCharArray(), ConfigIOUtil.utf8ToChars("plain".getBytes(StandardCharsets.UTF_8)));
        assertArrayEquals(new char[0], ConfigIOUtil.utf8ToChars(new byte[0]));
        assertNull(ConfigIOUtil.utf8ToChars(null));
        }

    @Test
    void utf8ToCharsIsLenient()
        {
        assertArrayEquals(new char[]{'a', '�', 'b'}, ConfigIOUtil.utf8ToChars(new byte[]{'a', (byte) 0xFF, 'b'}), "invalid byte");
        assertArrayEquals(new char[]{'a', '�'}, ConfigIOUtil.utf8ToChars(new byte[]{'a', (byte) 0xE2, (byte) 0x82}), "truncated sequence");
        }

    @Test
    void utf8ToCharsLeavesInputUntouched()
        {
        byte[] input = "s3cret".getBytes(StandardCharsets.UTF_8);
        ConfigIOUtil.utf8ToChars(input);
        assertArrayEquals("s3cret".getBytes(StandardCharsets.UTF_8), input);
        }
}
//___EOF___
