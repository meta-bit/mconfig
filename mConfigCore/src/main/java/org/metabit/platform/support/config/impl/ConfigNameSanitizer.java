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
 * - Reject "." and "..": config names become file and directory names, where these are path operators
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
        String normalized = normalize(name);
        if (!isAcceptable(normalized))
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
        return isAcceptable(normalize(name));
        }

    // replace all multiple whitespace with a single space, and trim leading/trailing whitespace
    private static String normalize(String name)
        {
        return name.trim().replaceAll("\\s+", " ");
        }

    private static boolean isAcceptable(String normalized)
        {
        if (normalized.equals(".") || normalized.equals("..")) return false;
        return ALLOWED.matcher(normalized).matches();
        }
}
//___EOF___
