package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.util.Collections;
import java.util.Map;

public class BasicSecretValue implements SecretValue
{
    private final byte[] value;
    private final SecretType type;

    public BasicSecretValue(byte[] value, SecretType type)
        {
        this.value = value;
        this.type = type;
        }

    @Override
    public byte[] getValue()
        {
        return value;
        }

    @Override
    public char[] getChars()
        {
        if (value == null)
            {
            return null;
            }
        char[] chars = new char[value.length];
        for (int i = 0; i < value.length; i++)
            {
            chars[i] = (char) value[i];
            }
        return chars;
        }

    @Override
    public SecretType getType()
        {
        return type;
        }

    @Override
    public Map<String, String> getMetadata()
        {
        return Collections.emptyMap();
        }

    @Override
    public void erase()
        {
        if (value != null)
            {
            java.util.Arrays.fill(value, (byte) 0);
            }
        }
}
