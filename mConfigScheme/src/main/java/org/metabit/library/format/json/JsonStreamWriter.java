package org.metabit.library.format.json;

import java.util.PrimitiveIterator;

/**
 * <p>JsonStreamWriter class.</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class JsonStreamWriter
{
    /**
     * perform JSON-escaping on a string.
     *
     * @param unescaped unescaped input string
     * @return JSON-escaped output string
     * @throws java.lang.IllegalArgumentException if an invalid codepoint is encountered in the input.
     */
    public static String jsonEscapeStringContent(final CharSequence unescaped)
            throws IllegalArgumentException
        {
        StringBuilder sb = new StringBuilder();
        PrimitiveIterator.OfInt it = unescaped.chars().iterator();
        while (it.hasNext())
            {
            // int ic = it.nextInt();
            int ic = it.nextInt();
            switch (ic)
                {
                case '"':   sb.append("\\\""); break;
                case '\\':  sb.append("\\\\"); break;
                case '/':   sb.append("\\/"); break;
                case 0x08:  sb.append("\\b"); break; // backspace
                case 0x09:  sb.append("\\t"); break; // tab, HT
                case 0x0A:  sb.append("\\n"); break; // LF
                case 0x0C:  sb.append("\\f"); break; // FF
                case 0x0D:  sb.append("\\r"); break; // CR
                default:
                    if (!Character.isValidCodePoint(ic))
                        throw new IllegalArgumentException();
                    if (Character.isISOControl(ic)) // trusting Java on this
                        { sb.append(String.format("\\u%04x",ic)); }
                    else // "Any codepoint except ... or control characters"
                        { sb.append((char)ic); }  // codepoint to char conversion implicit in Java.
                        // not usung Character.toChars(): (a) requires JDK11, (b) may replace single characters with a surrogate pair.
                }
            }
        return sb.toString();
        }
}
