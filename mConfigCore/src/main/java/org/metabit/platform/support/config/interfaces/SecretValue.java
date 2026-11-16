package org.metabit.platform.support.config.interfaces;

import java.util.Map;

public interface SecretValue
    {
    byte[] getValue();
    /**
     * returns the secret as characters, decoded from its bytes as UTF-8.
     * Malformed sequences become U+FFFD. The caller owns the returned array and should wipe it after use.
     *
     * @return a new char array; null if the value is null
     */
    default char[] getChars()
        {
        return org.metabit.platform.support.config.impl.util.ConfigIOUtil.utf8ToChars(getValue());
        }
    SecretType getType();
    Map<String,String> getMetadata();
    default boolean isSecret() { return true; }
    /**
     * Erases the secret from memory if possible.
     */
    default void erase() { }
    }
//___EOF___
