package org.metabit.platform.support.config.impl.logging;

/**
 * render text from outside the library (file names, paths from the OS or environment, names from
 * remote services) safely for log messages.
 * <p>
 * Log lines are read by people and by tools. Text that contains line breaks can forge extra log
 * lines; terminal escape sequences can rewrite what a console shows; bidirectional-override
 * characters can make text appear different from what it is (CWE-117). {@link #text(Object)}
 * replaces such characters with visible escapes - {@code \n}, {@code \r}, {@code \t}, or
 * {@code \}{@code uXXXX} - and leaves everything else unchanged, so a path stays readable and
 * still names the real location. Backslashes are not escaped, to keep Windows paths readable.
 * <p>
 * Use it where the message is built, for each outside value:
 * <pre>logger.warn("config file cannot be read: \"" + LogSafe.text(path) + "\"");</pre>
 * Never put outside text into the <em>format</em> string of the format variants of
 * {@code ConfigLoggingInterface}; pass it as an argument.
 * <p>
 * Not covered: lookups that some logging backends perform on message content (e.g. {@code ${...}}
 * in old log4j 2 versions) are the backend's responsibility.
 * <p>
 * Internal; not part of the public API.
 */
public final class LogSafe
{
    private LogSafe() { }

    /**
     * check whether a text contains characters that {@link #text(Object)} would escape.
     * Input validation uses this to refuse names that are never legitimate for configuration.
     *
     * @param s the text, may be null
     * @return true if s contains at least one control, line-separator or bidi-control character
     */
    public static boolean containsUnsafeCharacters(final CharSequence s)
        {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++)
            {
            if (isUnsafe(s.charAt(i))) return true;
            }
        return false;
        }

    /**
     * render a value for a log message, with unsafe characters escaped.
     *
     * @param value the value, typically a {@code String}, {@code Path} or {@code File}; may be null
     * @return the escaped string form; "null" for null
     */
    public static String text(final Object value)
        {
        String s = String.valueOf(value);
        if (!containsUnsafeCharacters(s)) return s;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++)
            {
            char c = s.charAt(i);
            if (!isUnsafe(c))
                {
                sb.append(c);
                continue;
                }
            switch (c)
                {
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(String.format("\\u%04x", (int) c)); break;
                }
            }
        return sb.toString();
        }

    private static boolean isUnsafe(final char c)
        {
        return Character.isISOControl(c)                // C0, DEL, C1 - includes line breaks and ESC
                || c == ' ' || c == ' '       // line and paragraph separator
                || (c >= '‪' && c <= '‮')     // bidi embeddings and overrides
                || (c >= '⁦' && c <= '⁩');    // bidi isolates
        }
}
//___EOF___
