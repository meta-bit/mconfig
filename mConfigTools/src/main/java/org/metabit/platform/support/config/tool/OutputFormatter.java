package org.metabit.platform.support.config.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OutputFormatter
{
    private OutputFormatter() { }

    static String toYaml(Object data)
        {
        ObjectMapper mapper = new YAMLMapper();
        try
            {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            }
        catch (Exception e)
            {
            throw new IllegalStateException("Failed to serialize YAML output.", e);
            }
        }

    static String toToml(Object data)
        {
        ObjectMapper mapper = new TomlMapper();
        try
            {
            return mapper.writer().withDefaultPrettyPrinter().writeValueAsString(data);
            }
        catch (Exception e)
            {
            throw new IllegalStateException("Failed to serialize TOML output.", e);
            }
        }

    static Object entryValue(ConfigEntry entry)
        {
        ConfigEntryType type = entry.getType();
        switch (type)
            {
            case BOOLEAN:
                try
                    {
                    return entry.getValueAsBoolean();
                    }
                catch (Exception e)
                    {
                    return safeString(entry);
                    }
            case NUMBER:
                try
                    {
                    return entry.getValueAsBigDecimal();
                    }
                catch (Exception ignored)
                    {
                    return safeString(entry);
                    }
            case MULTIPLE_STRINGS:
                try
                    {
                    return new ArrayList<>(entry.getValueAsStringList());
                    }
                catch (Exception ignored)
                    {
                    return safeString(entry);
                    }
            case BYTES:
            case STRING:
            default:
                return safeString(entry);
            }
        }

    private static String safeString(ConfigEntry entry)
        {
        try
            {
            return entry.getValueAsString();
            }
        catch (Exception e)
            {
            return "";
            }
        }

    static void putNestedValue(Map<String, Object> root, String key, Object value)
        {
        String[] nodes = key.split("/");
        Map<String, Object> current = root;
        for (int i = 0; i < nodes.length-1; i++)
            {
            String node = nodes[i];
            Object next = current.get(node);
            if (!(next instanceof Map))
                {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(node, created);
                next = created;
                }
            current = (Map<String, Object>) next;
            }
        if (nodes.length > 0)
            {
            current.put(nodes[nodes.length-1], value);
            }
        }

    static Map<String, Object> newLinkedMap()
        {
        return new LinkedHashMap<>();
        }

    static List<Map<String, Object>> newLinkedList()
        {
        return new ArrayList<>();
        }
}
