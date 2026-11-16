package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.util.Collections;
import java.util.Map;

public class BasicSecretValue implements SecretValue
{
    private final byte[] value;
    private final SecretType type;

    /**
     * creates a secret value from a copy of the given bytes.
     * <p>
     * The array is copied, so {@link #erase()} wipes only this instance's copy and never
     * the caller's array (which may be a stored configuration value). A caller handing
     * over a temporary array is responsible for wiping it.
     *
     * @param value the secret bytes; copied, may be null
     * @param type  the type of secret
     */
    public BasicSecretValue(byte[] value, SecretType type)
        {
        this.value = (value == null) ? null : value.clone();
        this.type = type;
        }

    @Override
    public byte[] getValue()
        {
        return value;
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
//___EOF___
