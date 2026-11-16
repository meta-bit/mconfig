package org.metabit.platform.support.config.impl.entry;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.interfaces.SecretType;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BasicSecretValueTest
{
    @Test
    void constructorCopiesCallerArray()
        {
        byte[] callers = "s3cret".getBytes(StandardCharsets.UTF_8);
        BasicSecretValue sv = new BasicSecretValue(callers, SecretType.PLAIN_TEXT);

        callers[0] = 'X';
        assertArrayEquals("s3cret".getBytes(StandardCharsets.UTF_8), sv.getValue(), "later changes by the caller must not leak in");
        }

    @Test
    void eraseWipesOwnCopyOnly()
        {
        byte[] callers = "s3cret".getBytes(StandardCharsets.UTF_8);
        BasicSecretValue sv = new BasicSecretValue(callers, SecretType.PLAIN_TEXT);

        sv.erase();
        assertArrayEquals(new byte[callers.length], sv.getValue(), "own copy is zeroed");
        assertArrayEquals("s3cret".getBytes(StandardCharsets.UTF_8), callers, "caller's array, e.g. a stored config value, is untouched");
        }

    @Test
    void getCharsDecodesUtf8()
        {
        String text = "pässwörd€";
        BasicSecretValue sv = new BasicSecretValue(text.getBytes(StandardCharsets.UTF_8), SecretType.PLAIN_TEXT);
        assertArrayEquals(text.toCharArray(), sv.getChars());
        }

    @Test
    void nullValue()
        {
        BasicSecretValue sv = new BasicSecretValue(null, SecretType.PLAIN_TEXT);
        assertNull(sv.getValue());
        assertNull(sv.getChars());
        assertDoesNotThrow(sv::erase);
        }

    @Test
    void typeAndMetadata()
        {
        BasicSecretValue sv = new BasicSecretValue(new byte[]{1, 2}, SecretType.SYMMETRIC_KEY);
        assertEquals(SecretType.SYMMETRIC_KEY, sv.getType());
        assertTrue(sv.isSecret());
        assertTrue(sv.getMetadata().isEmpty());
        assertFalse(sv.toString().contains("\u0001"), "toString must not expose the value");
        }
}
//___EOF___
