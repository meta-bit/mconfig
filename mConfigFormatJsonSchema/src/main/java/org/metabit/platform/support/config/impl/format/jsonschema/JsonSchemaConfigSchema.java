package org.metabit.platform.support.config.impl.format.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.util.*;

/**
 * Implementation of ConfigSchema that wraps a standard JSON Schema (json-schema.org).
 */
public class JsonSchemaConfigSchema implements ConfigSchema
{
    private static final ObjectMapper                   mapper          = new ObjectMapper();
    private final        JsonSchema                     jsonSchema;
    private final        Map<String, ConfigSchemaEntry> inferredEntries = new HashMap<>();
    private              String                         company;
    private              String                         application;
    private              String                         configName;
    private              String                         version;

    public JsonSchemaConfigSchema(String schemaJson)
        {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.jsonSchema = factory.getSchema(schemaJson);
        parseMetadata(schemaJson);
        inferMConfigEntries(jsonSchema.getSchemaNode(), "");
        }

    private void parseMetadata(String schemaJson)
        {
        try
            {
            JsonNode node = mapper.readTree(schemaJson);
            if (node.has("x-mconfig-company")) this.company = node.get("x-mconfig-company").asText();
            if (node.has("x-mconfig-application")) this.application = node.get("x-mconfig-application").asText();
            if (node.has("x-mconfig-config")) this.configName = node.get("x-mconfig-config").asText();
            if (node.has("VERSION")) this.version = node.get("VERSION").asText();
            else if (node.has("version")) this.version = node.get("version").asText();

            if ((company == null || application == null) && node.has("$id"))
                {
                decomposeId(node.get("$id").asText());
                }
            }
        catch (Exception ignored) { }
        }

    private void decomposeId(String id)
        {
        try
            {
            java.net.URI uri = java.net.URI.create(id);
            String path = uri.getPath();
            if (path != null)
                {
                String[] parts = path.split("/");
                if (parts.length >= 3)
                    {
                    if (configName == null)
                        {
                        String last = parts[parts.length-1];
                        if (last.endsWith(".schema.json")) configName = last.substring(0, last.length()-12);
                        else if (last.endsWith(".json")) configName = last.substring(0, last.length()-5);
                        }
                    if (application == null) application = parts[parts.length-2];
                    if (company == null && parts.length >= 4) company = parts[parts.length-3];
                    }
                }
            }
        catch (Exception ignored) { }
        }

    private void inferMConfigEntries(JsonNode node, String prefix)
        {
        if (node == null || !node.isObject()) return;

        if (node.has("properties"))
            {
            JsonNode properties = node.get("properties");
            Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
            while (it.hasNext())
                {
                Map.Entry<String, JsonNode> field = it.next();
                String key = prefix.isEmpty() ? field.getKey() : prefix+"/"+field.getKey();
                JsonNode child = field.getValue();

                if (child.has("properties"))
                    {
                    inferMConfigEntries(child, key);
                    }
                else
                    {
                    ConfigSchemaEntry entry = mapNodeToEntry(key, child);
                    inferredEntries.put(key, entry);
                    }
                }
            }
        }

    private ConfigSchemaEntry mapNodeToEntry(String key, JsonNode node)
        {
        ConfigEntryType type = inferType(node);
        ConfigSchemaEntry entry = new ConfigSchemaEntry(key, type);

        if (node.has("description")) entry.setDescription(node.get("description").asText());
        if (node.has("default"))
            {
            try { entry.setDefault(node.get("default").asText()); } catch (Exception ignored) { }
            }

        // Metadata Extensions
        if (node.has("x-mconfig-secret")) entry.setSecret(node.get("x-mconfig-secret").asBoolean());
        if (node.has("x-mconfig-hidden")) entry.setHidden(node.get("x-mconfig-hidden").asBoolean());
        if (node.has("x-mconfig-write-scope"))
            {
            try { entry.setWriteScope(ConfigScope.valueOf(node.get("x-mconfig-write-scope").asText().toUpperCase())); } catch (Exception ignored) { }
            }

        // Pattern/Constraints
        if (node.has("pattern")) entry.setValidationPattern(node.get("pattern").asText());

        return entry;
        }

    private ConfigEntryType inferType(JsonNode node)
        {
        String typeStr = node.has("type") ? node.get("type").asText() : "string";
        String format = node.has("format") ? node.get("format").asText() : null;

        if ("boolean".equalsIgnoreCase(typeStr)) return ConfigEntryType.BOOLEAN;
        if ("integer".equalsIgnoreCase(typeStr) || "number".equalsIgnoreCase(typeStr)) return ConfigEntryType.NUMBER;
        if ("array".equalsIgnoreCase(typeStr)) return ConfigEntryType.MULTIPLE_STRINGS;

        if (format != null)
            {
            if ("uri".equalsIgnoreCase(format)) return ConfigEntryType.URI;
            if ("date".equalsIgnoreCase(format)) return ConfigEntryType.DATE;
            if ("time".equalsIgnoreCase(format)) return ConfigEntryType.TIME;
            if ("date-time".equalsIgnoreCase(format)) return ConfigEntryType.DATETIME;
            }

        return ConfigEntryType.STRING;
        }

    @Override
    public boolean checkConfigEntryValidity(String fullKey, ConfigEntry entry)
        {
        try
            {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode current = root;
            String[] parts = fullKey.split("/");
            for (int i = 0; i < parts.length-1; i++)
                {
                current = current.putObject(parts[i]);
                }
            current.set(parts[parts.length-1], convertToNode(entry));
            Set<ValidationMessage> errors = jsonSchema.validate(root);
            return errors.isEmpty();
            }
        catch (Exception e)
            {
            return false;
            }
        }

    private JsonNode convertToNode(ConfigEntry entry)
            throws Exception
        {
        switch (entry.getType())
            {
            case BOOLEAN:
                return mapper.valueToTree(entry.getValueAsBoolean());
            case NUMBER:
                return mapper.valueToTree(entry.getValueAsBigDecimal());
            case MULTIPLE_STRINGS:
            case ENUM_SET:
                return mapper.valueToTree(entry.getValueAsStringList());
            default:
                return mapper.valueToTree(entry.getValueAsString());
            }
        }

    @Override
    public void addSchemaEntry(String format1)
            throws ConfigCheckedException
        { throw new UnsupportedOperationException(); }

    @Override
    public void addSchemaEntry(String fullKey, ConfigEntryType type, String validityScheme, Object defaultValue, String description, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes)
            throws ConfigCheckedException
        { throw new UnsupportedOperationException(); }

    @Override
    public void addSchemaEntry(ConfigSchemaEntry entry) { throw new UnsupportedOperationException(); }

    @Override
    public void init(ConfigFactoryInstanceContext ctx) { }

    @Override
    public void transferDefaults(DefaultLayer defaultLayer)
        {
        // Create a dummy source for defaults
        ConfigSource source = new org.metabit.platform.support.config.impl.ConfigLocationImpl(ConfigScope.PRODUCT, null, null, null);
        for (Map.Entry<String, ConfigSchemaEntry> entry : inferredEntries.entrySet())
            {
            if (entry.getValue().getDefault() != null)
                {
                ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
                meta.setSpecification(entry.getValue());
                defaultLayer.putEntry(entry.getKey(), new StringConfigEntryLeaf(entry.getKey(), entry.getValue().getDefault(), meta));
                }
            }
        }

    @Override
    public Set<String> getEntryKeys()
        {
        return inferredEntries.keySet();
        }

    @Override
    public ConfigEntrySpecification getSpecification(String fullKey)
        {
        return inferredEntries.get(fullKey);
        }

    @Override
    public String toJSON(String name, boolean filterHidden, boolean sanitizeSecrets)
        {
        // For standard JSON schemas, we currently return the original JSON.
        // In the future, we could implement filtering here as well if we decompose it fully.
        try
            {
            return mapper.writeValueAsString(jsonSchema.getSchemaNode());
            }
        catch (Exception e)
            {
            return null;
            }
        }

    @Override
    public boolean isCompatible(String version)
        {
        if (this.version == null || version == null) return true;
        try
            {
            String[] v1 = this.version.split("\\.");
            String[] v2 = version.split("\\.");
            return v1[0].equals(v2[0]); // Simple major version compatibility
            }
        catch (Exception e)
            {
            return true;
            }
        }

    public String getApplication() { return application; }

    public String getConfigName() { return configName; }

    public String getVersion() { return version; }
}
