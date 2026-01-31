package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.impl.ConfigFactoryComponent;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.NullConfigScheme;

import java.util.List;
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
    public static ConfigEntry createEntry(String fullKey, Object value, ConfigEntryType type, ConfigScheme scheme, ConfigSource source)
        {
        if (scheme == null)
            {
            scheme = NullConfigScheme.INSTANCE;
            }
        ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
        ConfigEntrySpecification spec = scheme.getSpecification(fullKey);
        meta.setSpecification(spec);

        if (spec != null && !(spec instanceof org.metabit.platform.support.config.scheme.NullConfigEntrySpecification))
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
                byte[] secretBytes;
                if (value instanceof byte[])
                    {
                    secretBytes = (byte[]) value;
                    }
                else
                    {
                    secretBytes = String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }

                SecretType secretType = SecretType.PLAIN_TEXT;
                if (type == ConfigEntryType.BYTES)
                    {
                    secretType = SecretType.SYMMETRIC_KEY; // Or some other binary secret type
                    }
                sv = new BasicSecretValue(secretBytes, secretType);
                }
            
            // Try to find a component that can create secret entries
            ServiceLoader<ConfigFactoryComponent> factoryComponents = ServiceLoader.load(ConfigFactoryComponent.class);
            for (ConfigFactoryComponent component : factoryComponents)
                {
                if (component instanceof SecretEntryFactory)
                    {
                    return ((SecretEntryFactory) component).createSecretEntry(fullKey, sv, source);
                    }
                }
            
            // Fallback: If no secret entry factory is found, return a normal entry marked as secret?
            // Or throw exception? The user wants modularity.
            return new StringConfigEntryLeaf(fullKey, String.valueOf(value), meta);
            }

        switch (type)
            {
            case NUMBER:
            case BOOLEAN:
            case ENUM:
            case URI:
            case FILEPATH:
            case DATE:
            case TIME:
            case DATETIME:
                return new TypedConfigEntryLeaf(fullKey, value, type, meta);
            case BYTES:
                return new BlobConfigEntryLeaf(fullKey, (byte[]) value, meta);
            case ENUM_SET:
            case MULTIPLE_STRINGS:
                @SuppressWarnings("unchecked")
                List<String> stringList = (List<String>) value;
                return new TypedConfigEntryLeaf(fullKey, stringList, type, meta);
            case STRING:
            default:
                if (value instanceof byte[])
                    {
                    return new BlobConfigEntryLeaf(fullKey, (byte[]) value, meta);
                    }
                return new StringConfigEntryLeaf(fullKey, String.valueOf(value), meta);
            }
        }
}
