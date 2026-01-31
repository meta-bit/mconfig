package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.BlobConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * YAMLSnakeYAMLConfigLayer represents a configuration layer backed by YAML data
 * processed with the SnakeYAML library.
 */
public class YAMLSnakeYAMLConfigLayer implements ConfigLayerInterface
{
    private final ConfigFactorySettings  settings;
    private final ConfigLoggingInterface logger;
    private final Object                 data;
    private final ConfigLocation         location;
    private final ConfigSource           source;

    public YAMLSnakeYAMLConfigLayer(ConfigFactorySettings settings, ConfigLoggingInterface logger, ConfigLocation configLocation, YAMLwithSnakeYAMLFormat format, Object data, Object storageInstanceHandle)
        {
        this.settings = settings;
        this.logger = logger;
        this.location = configLocation;
        this.source = new ConfigLocationImpl(location, this, format, storageInstanceHandle);
        this.data = data;
        }

    @Override
    public boolean isEmpty()
        {
        if (data instanceof Map)
            {
            return ((Map<?, ?>) data).isEmpty();
            }
        if (data instanceof List)
            {
            return ((List<?>) data).isEmpty();
            }
        return data == null;
        }

    @Override
    public ConfigScope getScope()
        {
        return location.getScope();
        }

    @Override
    public ConfigSource getSource()
        {
        return source;
        }

    public Object getData()
        {
        return data;
        }

    @Override
    public boolean isWriteable()
        {
        return data instanceof Map;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void writeEntry(ConfigEntry configEntry) throws ConfigCheckedException
        {
        if (!isWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }

        Map<String, Object> root = (Map<String, Object>) data;
        String fullKey = configEntry.getKey();
        String[] nodes = fullKey.split("/");

        Map<String, Object> current = root;
        for (int i = 0; i < nodes.length - 1; i++)
            {
            String node = nodes[i];
            Object next = current.get(node);
            if (!(next instanceof Map))
                {
                next = new java.util.LinkedHashMap<String, Object>();
                current.put(node, next);
                }
            current = (Map<String, Object>) next;
            }

        String leafKey = nodes[nodes.length - 1];
        writeTypedValue(current, leafKey, configEntry);
        }

    private void writeTypedValue(Map<String, Object> node, String key, ConfigEntry entry) throws ConfigCheckedException
        {
        switch (entry.getType())
            {
            case BOOLEAN:
                node.put(key, entry.getValueAsBoolean());
                break;
            case NUMBER:
                try
                    {
                    node.put(key, entry.getValueAsInteger());
                    }
                catch (Exception e)
                    {
                    try
                        {
                        node.put(key, entry.getValueAsLong());
                        }
                    catch (Exception e1)
                        {
                        try
                            {
                            node.put(key, entry.getValueAsDouble());
                            }
                        catch (Exception e2)
                            {
                            node.put(key, entry.getValueAsString());
                            }
                        }
                    }
                break;
            case BYTES:
                node.put(key, entry.getValueAsBytes());
                break;
            case STRING:
            default:
                node.put(key, entry.getValueAsString());
                break;
            }
        }

    @Override
    public int flush() throws ConfigCheckedException
        {
        ((YAMLwithSnakeYAMLFormat) source.getStorageFormat()).writeFile(this);
        return 1;
        }

    @Override
    public int compareTo(ConfigLayerInterface configLayerInterface)
        {
        return 0;
        }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        if (data instanceof Map)
            {
            java.util.List<String> keys = new java.util.ArrayList<>();
            collectKeys((Map<String, Object>) data, "", keys);
            return keys.iterator();
            }
        return null;
        }

    private void collectKeys(Map<String, Object> node, String prefix, java.util.List<String> keys)
        {
        for (java.util.Map.Entry<String, Object> entry : node.entrySet())
            {
            String key = entry.getKey();
            String fullKey = prefix.isEmpty() ? key : prefix + "/" + key;
            Object value = entry.getValue();
            if (value instanceof Map)
                {
                collectKeys((Map<String, Object>) value, fullKey, keys);
                }
            else
                {
                keys.add(fullKey);
                }
            }
        }

    @Override
    public ConfigEntry getEntry(final String hierarchicalKeyPath)
        {
        String[] nodes = hierarchicalKeyPath.split("/");
        Object current = data;
        for (int i = 0; i < nodes.length; i++)
            {
            if (!(current instanceof Map))
                {
                return null;
                }
            current = ((Map<?, ?>) current).get(nodes[i]);
            if (current == null)
                {
                return null;
                }
            }

        return snakeYamlObjectToConfigEntry(hierarchicalKeyPath, current);
        }

    ConfigEntry snakeYamlObjectToConfigEntry(final String leafKey, final Object value)
        {
        if (value instanceof Map || value instanceof List)
            {
            return null; // Not a leaf
            }
        ConfigEntryMetadata meta = new ConfigEntryMetadata(this.source);
        if (value instanceof String)
            {
            return new TypedConfigEntryLeaf(leafKey, value, ConfigEntryType.STRING, meta);
            }
        else if (value instanceof Number)
            {
            return new TypedConfigEntryLeaf(leafKey, value, ConfigEntryType.NUMBER, meta);
            }
        else if (value instanceof Boolean)
            {
            return new TypedConfigEntryLeaf(leafKey, value, ConfigEntryType.BOOLEAN, meta);
            }
        else if (value instanceof byte[])
            {
            return new BlobConfigEntryLeaf(leafKey, (byte[]) value, meta);
            }
        return new TypedConfigEntryLeaf(leafKey, String.valueOf(value), ConfigEntryType.STRING, meta);
        }
}
