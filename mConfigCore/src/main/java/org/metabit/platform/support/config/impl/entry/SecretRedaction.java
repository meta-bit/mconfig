package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigEntry;

/**
 * Central decision whether the value of an entry must be kept out of exports and output
 * (files written by the mapper, CLI listings, toString), and the marker used in its place.
 * <p>
 * Only the value is suppressed. Key, scope and location of a protected entry stay visible,
 * so the data remains findable, correctable and erasable.
 * <p>
 * Internal; used by core and the official modules (mapper, tools, secrets).
 * SENSITIVE (entry flags, Path B) is planned to be decided here as well.
 */
public final class SecretRedaction
{
    /** marker shown in place of a suppressed value, in human-readable output. */
    public static final String REDACTED = "[REDACTED]";

    private SecretRedaction() { }

    /**
     * check whether the value of an entry must not be exported or shown.
     *
     * @param entry the entry to check; may be null
     * @return true if the value is confidential (currently: the entry is a secret)
     */
    public static boolean isConfidential(final ConfigEntry entry)
        {
        return (entry != null) && entry.isSecret();
        }
}
//___EOF___
