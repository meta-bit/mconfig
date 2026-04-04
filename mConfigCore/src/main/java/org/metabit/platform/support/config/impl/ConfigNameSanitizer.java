package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Internal utility for validating and normalizing configuration names.
 * Not part of the public API; kept in impl package on purpose.
 * <p>
 * Rules (adjust as needed):
 * - Null is not allowed (callers decide semantics for null)
 * - Empty string "" is allowed and returned unchanged (used by scheme-aggregation paths)
 * - Trim leading/trailing whitespace
 * - Collapse inner whitespace to a single space
 * : converting spaces to dashes or underscores has been disabled
 * - Allow [A-Za-z0-9._- ]
 * - contents are case sensitive, for filename matching
 */
final class ConfigNameSanitizer
{

    private static final Pattern ALLOWED           = Pattern.compile("[a-zA-Z0-9._\\s\\-]+");

    private ConfigNameSanitizer() { }

    static String sanitize(String name)
        {
        Objects.requireNonNull(name, "config name");
        if (name.isEmpty()) return name;

        // replace all multuple whitespace with a single space, and trim leading/trailing whitespace
        String trimmed = name.trim().replaceAll("\\s+", " ");
        // String normalized = trimmed.replace(' ', '-'); //@CHECK!
        String normalized = trimmed;

        if (!ALLOWED.matcher(normalized).matches())
            { throw new ConfigException("config name still invalid after normalization: "+normalized); }
        return normalized;
        }

    static String sanitizeNullable(String name)
        {
        return (name == null) ? null : sanitize(name);
        }

    static boolean isValid(String name)
        {
        if (name == null) return false;
        if (name.isEmpty()) return true;
        String normalized = name.trim().replaceAll("\\s+", " ").replace(' ', '-');
        return ALLOWED.matcher(normalized).matches();
        }
}
