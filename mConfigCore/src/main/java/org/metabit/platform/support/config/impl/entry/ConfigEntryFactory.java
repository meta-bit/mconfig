package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.interfaces.ConfigFactoryComponent;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.NullConfigSchema;

import java.util.ServiceLoader;

/**
 * Factory for creating ConfigEntry instances based on type and scheme.
 * It would be punny if this was a ConfigTree; alas, it is not.
 */
public class ConfigEntryFactory
{
    /**
     * Create a new ConfigEntry instance.
     *
     * @param fullKey the full key of the entry
     * @param value the value to be stored
     * @param type the type of the entry
     * @param scheme the config scheme (optional)
     * @param source the config source
     * @return the created ConfigEntry
     */
    public static ConfigEntry createEntry(String fullKey, Object value, ConfigEntryType type, ConfigSchema scheme, ConfigSource source)
        {
        if (scheme == null)
            {
            scheme = NullConfigSchema.INSTANCE;
            }
        ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
        ConfigEntrySpecification spec = scheme.getSpecification(fullKey);
        if (spec == null) // schema implementations predating the never-null contract
            { spec = org.metabit.platform.support.config.schema.NullConfigEntrySpecification.INSTANCE; }
        meta.setSpecification(spec);

        if (!(spec instanceof org.metabit.platform.support.config.schema.NullConfigEntrySpecification))
            {
            type = spec.getType();
            }

        boolean isSecret = spec.isSecret();

        if (isSecret)
            {
            SecretValue sv;
            if (value instanceof SecretValue)
                {
                sv = (SecretValue) value;
                }
            else
                {
                SecretType secretType = SecretType.PLAIN_TEXT;
                if (type == ConfigEntryType.BYTES)
                    {
                    secretType = SecretType.SYMMETRIC_KEY; // Or some other binary secret type
                    }
                if (value instanceof byte[])
                    {
                    sv = new BasicSecretValue((byte[]) value, secretType); // copied; the caller's array stays untouched
                    }
                else
                    {
                    byte[] tmpBytes = String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    sv = new BasicSecretValue(tmpBytes, secretType);
                    java.util.Arrays.fill(tmpBytes, (byte) 0); // the constructor copied it
                    }
                }

            // Try to find a component that can create secret entries
            ServiceLoader<ConfigFactoryComponent> factoryComponents = ServiceLoader.load(ConfigFactoryComponent.class);
            for (ConfigFactoryComponent component : factoryComponents)
                {
                if (component instanceof SecretEntryFactory)
                    {
                    return ((SecretEntryFactory) component).createSecretEntry(fullKey, sv, meta);
                    }
                }

            // Fallback: If no secret entry factory is found, return a normal entry marked as secret.
            return new GenericConfigEntryLeaf(fullKey, value, type, meta);
            }

        return new GenericConfigEntryLeaf(fullKey, value, type, meta);
        }
}
//___EOF___
