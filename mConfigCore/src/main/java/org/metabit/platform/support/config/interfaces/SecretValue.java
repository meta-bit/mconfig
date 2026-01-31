package org.metabit.platform.support.config.interfaces;

import java.util.Map;

public interface SecretValue
    {
    byte[] getValue();
    default char[] getChars()
        {
        // default implementation, ideally would be overridden for better security
        byte[] value = getValue();
        if (value == null)
            {
            return null;
            }
        char[] chars = new char[value.length]; // assuming 1:1 for now, or just placeholder
        for (int i = 0; i < value.length; i++)
            {
            chars[i] = (char) value[i];
            }
        return chars;
        }
    SecretType getType();
    Map<String,String> getMetadata();
    default boolean isSecret() { return true; }
    /**
     * Erases the secret from memory if possible.
     */
    default void erase() { }
    }
