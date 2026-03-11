package org.metabit.platform.support.config.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigEventList;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OutputFormatter
{
    private OutputFormatter() { }

    static String toJson(Object data)
        {
        return toJson(data, false);
        }

    static String toJson(Object data, boolean whitesmiths)
        {
        ObjectMapper mapper = new ObjectMapper();
        try
            {
            if (whitesmiths)
                {
                return mapper.writer(new ModerateWhitesmithsPrettyPrinter()).writeValueAsString(data);
                }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            }
        catch (Exception e)
            {
            throw new IllegalStateException("Failed to serialize JSON output.", e);
            }
        }

    static String toYaml(Object data)
        {
        return toYaml(data, false);
        }

    static String toYaml(Object data, boolean whitesmiths)
        {
        ObjectMapper mapper = new YAMLMapper();
        try
            {
            if (whitesmiths)
                {
                return mapper.writer(new ModerateWhitesmithsPrettyPrinter()).writeValueAsString(data);
                }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            }
        catch (Exception e)
            {
            throw new IllegalStateException("Failed to serialize YAML output.", e);
            }
        }

    static String toToml(Object data)
        {
        return toToml(data, false);
        }

    static String toToml(Object data, boolean whitesmiths)
        {
        ObjectMapper mapper = new TomlMapper();
        try
            {
            if (whitesmiths)
                {
                return mapper.writer(new ModerateWhitesmithsPrettyPrinter()).writeValueAsString(data);
                }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
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
        if (key == null || key.isEmpty())
            {
            return;
            }
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

    static void reportEvents(ConfigEventList events, Main.OutputFormat format)
        {
        if (events == null || events.isEmpty()) return;

        if (format == Main.OutputFormat.HUMAN)
            {
            System.err.println();
            System.err.println("--- Configuration Events ---");
            for (ConfigEvent event : events)
                {
                String severity = "[" + event.getSeverity() + "]";
                String location = event.getLocation() != null ? " at " + event.getLocation() : "";
                System.err.printf("%-10s %s%s%n", severity, event.getMessage(), location);
                if (event.getRemediation() != ConfigEvent.Remediation.NONE)
                    {
                    System.err.println("           Hint: " + event.getRemediationMessage());
                    }
                }
            System.err.println("----------------------------");
            }
        else if (format == Main.OutputFormat.JSON || format == Main.OutputFormat.YAML || format == Main.OutputFormat.TOML)
            {
            // We print to stderr to avoid breaking piped JSON output from stdout
            System.err.println("Events: " + events.size());
            }
        }

    static void printHierarchical(Map<String, Object> data, int indent, boolean verbose, Configuration cfg, String parentKey)
        {
        for (Map.Entry<String, Object> entry : data.entrySet())
            {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullKey = (parentKey == null || parentKey.isEmpty()) ? key : parentKey + "/" + key;

            for (int i = 0; i < indent; i++) System.out.print("  ");

            if (value instanceof Map)
                {
                System.out.println(key + ":");
                printHierarchical((Map<String, Object>) value, indent + 1, verbose, cfg, fullKey);
                }
            else
                {
                System.out.print(key + " = " + value);
                if (verbose && cfg != null)
                    {
                    ConfigEntry configEntry = cfg.getConfigEntryFromFullKey(fullKey, java.util.EnumSet.allOf(ConfigScope.class));
                    if (configEntry != null)
                        {
                        System.out.println();
                        for (int i = 0; i < indent + 1; i++) System.out.print("  ");
                        System.out.print("Source: " + configEntry.getLocation());
                        System.out.println();
                        for (int i = 0; i < indent + 1; i++) System.out.print("  ");
                        System.out.print("Scope:  " + configEntry.getScope());
                        }
                    }
                System.out.println();
                }
            }
        }
}
