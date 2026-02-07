package org.metabit.platform.support.config.util.ts;

import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates TypeScript type definitions from an mConfig ConfigScheme.
 */
public class TypeScriptGenerator
{
    /**
     * Generates a TypeScript interface definition from the given ConfigScheme.
     *
     * @param scheme        The ConfigScheme to process.
     * @param interfaceName The name of the generated TypeScript interface.
     * @return A string containing the TypeScript interface definition.
     */
    public String generateInterface(ConfigScheme scheme, String interfaceName)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * Generated from mConfig ConfigScheme\n");
        sb.append(" */\n");
        sb.append("export interface ").append(interfaceName).append(" {\n");

        // Use a TreeMap to keep entries sorted and facilitate hierarchy building
        Map<String, ConfigEntrySpecification> specs = new TreeMap<>();
        for (String key : scheme.getEntryKeys())
            {
            specs.put(key, scheme.getSpecification(key));
            }

        // Build a nested structure to represent the hierarchical keys
        Map<String, Object> root = new TreeMap<>();
        for (Map.Entry<String, ConfigEntrySpecification> entry : specs.entrySet())
            {
            String[] parts = entry.getKey().split("/");
            Map<String, Object> current = root;
            for (int i = 0; i < parts.length - 1; i++)
                {
                current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new TreeMap<String, Object>());
                }
            current.put(parts[parts.length - 1], entry.getValue());
            }

        generateNestedInterface(sb, root, 1);

        sb.append("}\n");
        return sb.toString();
        }

    private void generateNestedInterface(StringBuilder sb, Map<String, Object> map, int indentLevel)
        {
        String indent = String.join("", Collections.nCopies(indentLevel * 4, " "));

        for (Map.Entry<String, Object> entry : map.entrySet())
            {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigEntrySpecification)
                {
                ConfigEntrySpecification spec = (ConfigEntrySpecification) value;
                appendProperty(sb, indent, key, spec);
                }
            else if (value instanceof Map)
                {
                sb.append(indent).append(key).append(": {\n");
                generateNestedInterface(sb, (Map<String, Object>) value, indentLevel + 1);
                sb.append(indent).append("};\n");
                }
            }
        }

    private void appendProperty(StringBuilder sb, String indent, String key, ConfigEntrySpecification spec)
        {
        String description = spec.getDescription();
        if (description != null && !description.isEmpty())
            {
            sb.append(indent).append("/** ").append(description).append(" */\n");
            }

        String tsType = mapToTypeScriptType(spec);
        boolean optional = !spec.isMandatory();

        sb.append(indent).append(key).append(optional ? "?" : "").append(": ").append(tsType).append(";\n");
        }

    private String mapToTypeScriptType(ConfigEntrySpecification spec)
        {
        ConfigEntryType type = spec.getType();
        String limitations = spec.getValueLimitations();

        switch (type)
            {
            case BOOLEAN:
                return "boolean";
            case NUMBER:
                return "number";
            case STRING:
            case URI:
            case FILEPATH:
            case DATE:
            case TIME:
            case DATETIME:
            case DURATION:
                return "string";
            case BYTES:
                return "string"; // Often base64 in JSON
            case MULTIPLE_STRINGS:
                return "string[]";
            case ENUM:
                if (limitations != null && !limitations.isEmpty())
                    {
                    // Assuming ENUM limitations are "VAL1|VAL2|VAL3"
                    return Arrays.stream(limitations.split("\\|"))
                            .map(v -> "'" + v.trim() + "'")
                            .collect(Collectors.joining(" | "));
                    }
                return "string";
            case ENUM_SET:
                if (limitations != null && !limitations.isEmpty())
                    {
                    String union = Arrays.stream(limitations.split("\\|"))
                            .map(v -> "'" + v.trim() + "'")
                            .collect(Collectors.joining(" | "));
                    return "(" + union + ")[]";
                    }
                return "string[]";
            default:
                return "any";
            }
        }
}
