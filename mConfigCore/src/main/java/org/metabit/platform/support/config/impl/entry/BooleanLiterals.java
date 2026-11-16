package org.metabit.platform.support.config.impl.entry;

import java.util.Locale;

/**
 * the string forms mConfig reads as a Boolean, in one place: conversion, schema validation, formats,
 * feature settings and tools agree on them.
 * <p>
 * Accepted, in any case and with surrounding blanks: {@code true/false}, {@code yes/no}, {@code on/off},
 * {@code 1/0}. Anything else, an empty string included, is not a Boolean. The plain Java behaviour
 * ({@link Boolean#parseBoolean(String)}: anything but "true" is false) is available through
 * {@code ConfigFeature.LENIENT_BOOLEAN_CONVERSION}, in the Configuration getters only.
 * <p>
 * Internal; used by core and the official modules (formats, tools).
 * @since 0.8.17
 */
public final class BooleanLiterals
{
    private BooleanLiterals() { }

    /**
     * @param text the string form
     * @return the Boolean it stands for; null if it is none of the accepted literals (or null)
     */
    public static Boolean parse(final String text)
        {
        if (text == null)
            { return null; }
        switch (text.trim().toLowerCase(Locale.ROOT))
            {
            case "true":
            case "yes":
            case "on":
            case "1":
                return Boolean.TRUE;
            case "false":
            case "no":
            case "off":
            case "0":
                return Boolean.FALSE;
            default:
                return null;
            }
        }

    /**
     * @param text the string form
     * @return true if {@link #parse(String)} accepts it
     */
    public static boolean isBoolean(final String text)
        {
        return parse(text) != null;
        }
}
//___EOF___
