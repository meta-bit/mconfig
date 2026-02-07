package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.BlobConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.*;
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
        if (data instanceof MappingNode)
            {
            return ((MappingNode) data).getValue().isEmpty();
            }
        if (data instanceof SequenceNode)
            {
            return ((SequenceNode) data).getValue().isEmpty();
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
        return data instanceof Map || data instanceof MappingNode;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void writeEntry(ConfigEntry configEntry) throws ConfigCheckedException
        {
        if (!isWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }

        String fullKey = configEntry.getKey();
        String[] nodes = fullKey.split("/");

        if (data instanceof Map)
            {
            Map<String, Object> root = (Map<String, Object>) data;
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
            writeTypedValueToMap(current, leafKey, configEntry);
            }
        else if (data instanceof MappingNode)
            {
            MappingNode current = (MappingNode) data;
            for (int i = 0; i < nodes.length - 1; i++)
                {
                String node = nodes[i];
                MappingNode next = findMappingNode(current, node);
                if (next == null)
                    {
                    next = new MappingNode(Tag.MAP, new java.util.ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
                    current.getValue().add(new NodeTuple(new ScalarNode(Tag.STR, node, null, null, DumperOptions.ScalarStyle.PLAIN), next));
                    }
                current = next;
                }
            String leafKey = nodes[nodes.length - 1];
            writeTypedValueToNode(current, leafKey, configEntry);
            }
        }

    private MappingNode findMappingNode(MappingNode parent, String key)
        {
        for (NodeTuple tuple : parent.getValue())
            {
            Node keyNode = tuple.getKeyNode();
            if (keyNode instanceof ScalarNode && key.equals(((ScalarNode) keyNode).getValue()))
                {
                if (tuple.getValueNode() instanceof MappingNode)
                    {
                    return (MappingNode) tuple.getValueNode();
                    }
                }
            }
        return null;
        }

    private void writeTypedValueToMap(Map<String, Object> node, String key, ConfigEntry entry) throws ConfigCheckedException
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

    private void writeTypedValueToNode(MappingNode node, String key, ConfigEntry entry) throws ConfigCheckedException
        {
        Node valueNode;
        switch (entry.getType())
            {
            case BOOLEAN:
                valueNode = new ScalarNode(Tag.BOOL, entry.getValueAsString(), null, null, DumperOptions.ScalarStyle.PLAIN);
                break;
            case NUMBER:
                valueNode = new ScalarNode(Tag.INT, entry.getValueAsString(), null, null, DumperOptions.ScalarStyle.PLAIN);
                break;
            case BYTES:
                // Base64 encode for bytes in YAML?
                valueNode = new ScalarNode(Tag.BINARY, java.util.Base64.getEncoder().encodeToString(entry.getValueAsBytes()), null, null, DumperOptions.ScalarStyle.LITERAL);
                break;
            case STRING:
            default:
                valueNode = new ScalarNode(Tag.STR, entry.getValueAsString(), null, null, DumperOptions.ScalarStyle.PLAIN);
                break;
            }

        // Find and replace or add
        for (int i = 0; i < node.getValue().size(); i++)
            {
            NodeTuple tuple = node.getValue().get(i);
            Node kn = tuple.getKeyNode();
            if (kn instanceof ScalarNode && key.equals(((ScalarNode) kn).getValue()))
                {
                node.getValue().set(i, new NodeTuple(kn, valueNode));
                return;
                }
            }
        node.getValue().add(new NodeTuple(new ScalarNode(Tag.STR, key, null, null, DumperOptions.ScalarStyle.PLAIN), valueNode));
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
            collectKeysFromMap((Map<String, Object>) data, "", keys);
            return keys.iterator();
            }
        else if (data instanceof MappingNode)
            {
            java.util.List<String> keys = new java.util.ArrayList<>();
            collectKeysFromNode((MappingNode) data, "", keys);
            return keys.iterator();
            }
        return null;
        }

    private void collectKeysFromMap(Map<String, Object> node, String prefix, java.util.List<String> keys)
        {
        for (java.util.Map.Entry<String, Object> entry : node.entrySet())
            {
            String key = entry.getKey();
            String fullKey = prefix.isEmpty() ? key : prefix + "/" + key;
            Object value = entry.getValue();
            if (value instanceof Map)
                {
                collectKeysFromMap((Map<String, Object>) value, fullKey, keys);
                }
            else
                {
                keys.add(fullKey);
                }
            }
        }

    private void collectKeysFromNode(MappingNode node, String prefix, java.util.List<String> keys)
        {
        for (NodeTuple tuple : node.getValue())
            {
                Node kn = tuple.getKeyNode();
                if (kn instanceof ScalarNode)
                {
                    String key = ((ScalarNode) kn).getValue();
                    String fullKey = prefix.isEmpty() ? key : prefix + "/" + key;
                    Node vn = tuple.getValueNode();
                    if (vn instanceof MappingNode)
                    {
                        collectKeysFromNode((MappingNode) vn, fullKey, keys);
                    }
                    else
                    {
                        keys.add(fullKey);
                    }
                }
            }
        }

    @Override
    public ConfigEntry getEntry(final String hierarchicalKeyPath)
        {
        String[] nodes = hierarchicalKeyPath.split("/");
        if (data instanceof Map)
            {
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
        else if (data instanceof MappingNode)
            {
            MappingNode current = (MappingNode) data;
            for (int i = 0; i < nodes.length - 1; i++)
                {
                current = findMappingNode(current, nodes[i]);
                if (current == null) return null;
                }
            Node leaf = findNode(current, nodes[nodes.length - 1]);
            if (leaf == null) return null;
            return snakeYamlNodeToConfigEntry(hierarchicalKeyPath, leaf);
            }

        return null;
        }

    private Node findNode(MappingNode parent, String key)
        {
        for (NodeTuple tuple : parent.getValue())
            {
            Node keyNode = tuple.getKeyNode();
            if (keyNode instanceof ScalarNode && key.equals(((ScalarNode) keyNode).getValue()))
                {
                return tuple.getValueNode();
                }
            }
        return null;
        }

    ConfigEntry snakeYamlNodeToConfigEntry(final String leafKey, final Node node)
        {
        if (node instanceof MappingNode || node instanceof SequenceNode)
            {
            return null; // Not a leaf
            }
        if (!(node instanceof ScalarNode)) return null;

        ScalarNode scalar = (ScalarNode) node;
        ConfigEntryMetadata meta = new ConfigEntryMetadata(this.source);
        Tag tag = scalar.getTag();

        if (Tag.BOOL.equals(tag))
            {
            return new TypedConfigEntryLeaf(leafKey, Boolean.valueOf(scalar.getValue()), ConfigEntryType.BOOLEAN, meta);
            }
        else if (Tag.INT.equals(tag) || Tag.FLOAT.equals(tag))
            {
            return new TypedConfigEntryLeaf(leafKey, scalar.getValue(), ConfigEntryType.NUMBER, meta);
            }
        else if (Tag.BINARY.equals(tag))
            {
            return new BlobConfigEntryLeaf(leafKey, java.util.Base64.getDecoder().decode(scalar.getValue()), meta);
            }

        return new TypedConfigEntryLeaf(leafKey, scalar.getValue(), ConfigEntryType.STRING, meta);
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
