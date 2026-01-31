package org.metabit.platform.support.config.impl.format.yaml.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.BlobConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.util.Iterator;

/**
 * YAMLJacksonConfigLayer represents a configuration layer backed by YAML data
 * processed with the Jackson library. It implements the ConfigLayerInterface,
 * providing methods to interact with configuration entries in a hierarchical
 * YAML structure.
 */
public class YAMLJacksonConfigLayer implements ConfigLayerInterface
{

    private final ConfigFactorySettings  settings;
    private final ConfigLoggingInterface logger;
    private final JsonNode               yamlTreeRoot;
    private final JsonNodeType           treeRootType;
    private final ConfigLocation         location;
    private final ConfigSource           source;

    public YAMLJacksonConfigLayer(ConfigFactorySettings settings, ConfigLoggingInterface logger, ConfigLocation configLocation, YAMLwithJacksonFormat format, JsonNode rootNode, Object storageInstanceHandle)
        {
        this.settings = settings;
        this.logger = logger;
        this.location = configLocation;
        this.source = new ConfigLocationImpl(location, this, format, storageInstanceHandle);
        this.yamlTreeRoot = rootNode;
        this.treeRootType = rootNode.getNodeType();
        }



    @Override
    public boolean isEmpty()
        { return yamlTreeRoot.isEmpty(); }

    @Override
    public ConfigScope getScope()
        { return location.getScope(); }

    @Override
    public ConfigSource getSource()
        { return source; }


    public JsonNode getRootNode()
        { return yamlTreeRoot; }


    @Override
    public boolean isWriteable()
        {
        return yamlTreeRoot.isObject();
        }

    @Override
    public void writeEntry(ConfigEntry configEntry)
            throws ConfigCheckedException
        {
        if (!isWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }

        ObjectNode root = (ObjectNode) yamlTreeRoot;
        String fullKey = configEntry.getKey();
        String[] nodes = fullKey.split("/");
        
        ObjectNode current = root;
        for (int i = 0; i < nodes.length - 1; i++)
            {
            String node = nodes[i];
            JsonNode next = current.get(node);
            if (next == null || !next.isObject())
                {
                next = current.putObject(node);
                }
            current = (ObjectNode) next;
            }
        
        String leafKey = nodes[nodes.length - 1];
        writeTypedValue(current, leafKey, configEntry);
        }

    private void writeTypedValue(ObjectNode node, String key, ConfigEntry entry) throws ConfigCheckedException
        {
        switch (entry.getType())
            {
            case BOOLEAN:
                node.put(key, entry.getValueAsBoolean());
                break;
            case NUMBER:
                try
                    {
                    node.put(key, entry.getValueAsBigDecimal());
                    }
                catch (Exception eb)
                    {
                    try
                        {
                        node.put(key, entry.getValueAsBigInteger());
                        }
                    catch (Exception eb2)
                        {
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
                        }
                    }
                break;
            case BYTES:
                node.put(key, entry.getValueAsBytes());
                break;
            case MULTIPLE_STRINGS:
                try
                    {
                    ArrayNode arrayNode = node.putArray(key);
                    for (String s : entry.getValueAsStringList())
                        {
                        arrayNode.add(s);
                        }
                    }
                catch (Exception e)
                    {
                    node.put(key, entry.getValueAsString());
                    }
                break;
            case STRING:
            default:
                node.put(key, entry.getValueAsString());
                break;
            }
        }

    @Override
    public int flush()
            throws ConfigCheckedException
        {
        ((YAMLwithJacksonFormat)source.getStorageFormat()).writeFile(this);
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
        if (yamlTreeRoot.isObject())
            {
            java.util.List<String> keys = new java.util.ArrayList<>();
            collectKeys(yamlTreeRoot, "", keys);
            return keys.iterator();
            }
        return null;
        }

    private void collectKeys(JsonNode node, String prefix, java.util.List<String> keys)
        {
        if (node.isObject())
            {
            Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext())
                {
                java.util.Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String fullKey = prefix.isEmpty() ? key : prefix + "/" + key;
                JsonNode value = entry.getValue();
                if (value.isObject())
                    {
                    collectKeys(value, fullKey, keys);
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
        JsonNode current = yamlTreeRoot;
        for (int i = 0; i < nodes.length; i++)
            {
            current = current.get(nodes[i]);
            if (current == null)
                {
                return null;
                }
            }

        if (current.isMissingNode() || current.isNull())
            {
            return null;
            }

        return jacksonJsonNodeToConfigEntry(hierarchicalKeyPath, current);
        }

    ConfigEntry jacksonJsonNodeToConfigEntry(final String leafKey, final JsonNode jsonNode)
        {
        ConfigEntryMetadata meta = new ConfigEntryMetadata(this.source);
        switch (jsonNode.getNodeType())
            {
            case STRING:
                return new TypedConfigEntryLeaf(leafKey, jsonNode.textValue(), ConfigEntryType.STRING, meta);
            case NUMBER:
                if (jsonNode.isInt())
                    {
                    return new TypedConfigEntryLeaf(leafKey, jsonNode.intValue(), ConfigEntryType.NUMBER, meta);
                    }
                else if (jsonNode.isLong())
                    {
                    return new TypedConfigEntryLeaf(leafKey, jsonNode.longValue(), ConfigEntryType.NUMBER, meta);
                    }
                else if (jsonNode.isDouble())
                    {
                    return new TypedConfigEntryLeaf(leafKey, jsonNode.doubleValue(), ConfigEntryType.NUMBER, meta);
                    }
                else if (jsonNode.isBigInteger())
                    {
                    return new TypedConfigEntryLeaf(leafKey, jsonNode.bigIntegerValue(), ConfigEntryType.NUMBER, meta);
                    }
                else if (jsonNode.isBigDecimal())
                    {
                    return new TypedConfigEntryLeaf(leafKey, jsonNode.decimalValue(), ConfigEntryType.NUMBER, meta);
                    }
                return new TypedConfigEntryLeaf(leafKey, jsonNode.numberValue(), ConfigEntryType.NUMBER, meta);
            case BOOLEAN:
                return new TypedConfigEntryLeaf(leafKey, jsonNode.booleanValue(), ConfigEntryType.BOOLEAN, meta);
            case BINARY:
                try
                    {
                    return new BlobConfigEntryLeaf(leafKey, jsonNode.binaryValue(), meta);
                    }
                catch (Exception e)
                    {
                    return null;
                    }
            default:
                // Returning null for non-leaf nodes for now instead of throwing an exception.
                // This allows the cursor to skip over branches when iterating for leaf values.
                return null;
            }
        }
}
