package org.metabit.platform.support.config.impl.util;

/**
 * Low-level IO and data transformation utility functions for mConfig core.
 * Internal implementation class.
 */
public class ConfigIOUtil
{
    private static final String HEX_SEPARATORS = " \t\n\r:";

    /**
     * Decodes UTF-8 bytes into a new char array, without creating an intermediate String.
     * <p>
     * Decoding is lenient: malformed or truncated sequences become U+FFFD. The temporary
     * buffer is wiped, so only the returned array holds the characters.
     *
     * @param bytes the UTF-8 bytes; not modified
     * @return the decoded characters; null if the input is null
     */
    public static char[] utf8ToChars(byte[] bytes)
        {
        if (bytes == null)
            {
            return null;
            }
        java.nio.charset.CharsetDecoder decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE);
        // UTF-8 never yields more chars than bytes: 1..3 bytes -> 1 char, 4 bytes -> 2 chars, malformed -> 1 char.
        java.nio.CharBuffer out = java.nio.CharBuffer.allocate(bytes.length);
        decoder.decode(java.nio.ByteBuffer.wrap(bytes), out, true);
        decoder.flush(out);
        out.flip();
        char[] result = new char[out.remaining()];
        out.get(result);
        java.util.Arrays.fill(out.array(), '\0');
        return result;
        }

    /**
     * Decodes a hexadecimal string into a byte array.
     * Supports a "0x" prefix and the separators space, tab, newline, carriage return and colon
     * between bytes, e.g. "DE AD BE EF" or "de:ad:be:ef".
     * <p>
     * Decoding is strict: input that is not a complete hex sequence is rejected as a whole,
     * never truncated. Callers use {@code null} to fall back to another interpretation.
     *
     * @param tmpString the hexadecimal string to decode
     * @return the decoded byte array; null if the input is null, contains no hex digits,
     * contains any other character, has an odd number of hex digits,
     * or has a separator between the two digits of a byte.
     */
    public static byte[] hexDecode(String tmpString)
        {
        if (tmpString == null)
            {
            return null;
            }
        // detect and trim "0x" introducer.
        if (tmpString.startsWith("0x") || tmpString.startsWith("0X"))
            {
            tmpString = tmpString.substring(2);
            }
        byte[] byteBuffer = new byte[tmpString.length() / 2]; // upper boundary.
        int offset = 0;
        boolean msbFlag = true;
        int b = 0;
        for (int i = 0; i < tmpString.length(); i++)
            {
            char ch = tmpString.charAt(i);
            int c = Character.digit(ch, 16);
            if (c >= 0 && ch < 128) // ASCII hex digit only; Character.digit also accepts fullwidth digits
                {
                if (msbFlag)
                    {
                    b = c << 4; // upper nybble
                    msbFlag = false;
                    }
                else
                    {
                    byteBuffer[offset++] = (byte) (b | c);
                    msbFlag = true;
                    }
                }
            else if (msbFlag && HEX_SEPARATORS.indexOf(ch) >= 0)
                {
                continue; // separator between bytes
                }
            else
                {
                return null; // invalid character, or separator inside a byte
                }
            }
        if (!msbFlag || offset == 0) // dangling nybble, or no hex digits at all
            {
            return null;
            }
        if (offset == byteBuffer.length)  // no need to trim
            {
            return byteBuffer;
            }
        // trim bytes to the number of bytes actually used
        byte[] resultBytes = new byte[offset];
        System.arraycopy(byteBuffer, 0, resultBytes, 0, offset);
        java.util.Arrays.fill(byteBuffer, (byte) 0); // may hold key material
        return resultBytes;
        }
}
//___EOF___
