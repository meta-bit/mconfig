package org.metabit.platform.support.config.impl.format.jsonschema;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.source.core.DefaultLayer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Implementation of ConfigSchema that wraps a standard JSON Schema (json-schema.org).
 */
public class JsonSchemaConfigSchema implements ConfigSchema
{
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String MCONFIG_SCHEMA_FILENAME_EXTENSION = ".mconfig-schema.json";
    private final        Schema                         jsonSchema;
    private final        Map<String, ConfigSchemaEntry> inferredEntries = new HashMap<>();
    private              String                         company;
    private              String                         application;
    private              String                         configName;
    private              String                         version;

    public JsonSchemaConfigSchema(String schemaJson)
        {
        this.jsonSchema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7).getSchema(schemaJson);
        parseMetadata(schemaJson);
        // the validator above has accepted the JSON. A failure while inferring the entries propagates:
        // swallowing it would drop all following entries silently, x-mconfig-secret ones included.
        inferMConfigEntries(mapper.readTree(schemaJson), "");
        }

    private void parseMetadata(String schemaJson)
        {
        try
            {
            JsonNode node = mapper.readTree(schemaJson);
            if (node.has("x-mconfig-company")) this.company = node.get("x-mconfig-company").asString();
            if (node.has("x-mconfig-application")) this.application = node.get("x-mconfig-application").asString();
            if (node.has("x-mconfig-config")) this.configName = node.get("x-mconfig-config").asString();
            if (node.has("VERSION")) this.version = node.get("VERSION").asString();
            else if (node.has("version")) this.version = node.get("version").asString();

            if ((company == null || application == null) && node.has("$id"))
                {
                decomposeId(node.get("$id").asString());
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
                        if (last.endsWith(MCONFIG_SCHEMA_FILENAME_EXTENSION)) configName = last.substring(0, last.length()-MCONFIG_SCHEMA_FILENAME_EXTENSION.length());
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
            Iterator<Map.Entry<String, JsonNode>> it = properties.properties().iterator();
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

        if (node.has("description")) entry.setDescription(node.get("description").asString());
        if (node.has("default"))
            {
            try { entry.setDefault(node.get("default").asString()); } catch (Exception ignored) { }
            }

        // Metadata Extensions
        if (node.has("x-mconfig-secret")) entry.setSecret(node.get("x-mconfig-secret").asBoolean());
        if (node.has("x-mconfig-hidden")) entry.setHidden(node.get("x-mconfig-hidden").asBoolean());
        if (node.has("x-mconfig-write-scope"))
            {
            try { entry.setWriteScope(ConfigScope.valueOf(node.get("x-mconfig-write-scope").asString().toUpperCase())); } catch (Exception ignored) { }
            }

        // Pattern/Constraints. JSON Schema applies "pattern" to every string value, also one with a "format":
        // a string with format "uri" must then be a URI *and* match the pattern (ConfigSchemaEntry checks the
        // type first, then the pattern against the value's string form). Other JSON types ignore it.
        if (node.has("pattern") && isStringTyped(node))
            entry.validationPattern = compileJsonSchemaPattern(key, node.get("pattern").asString());

        return entry;
        }

    /**
     * JSON Schema "pattern" is an ECMA-262 regular expression with search semantics: it may match anywhere in the
     * value unless anchored. ConfigSchemaEntry checks its pattern with matches(), i.e. against the whole value,
     * so the pattern is wrapped accordingly. DOTALL applies to the wrapper only: inside the pattern, "." does not
     * match a line break, as in JSON Schema (and in the read-path validator). It is set directly, not through
     * setValidationPattern(), which would read keywords like "email" or "port" as named validators.
     *
     * @throws ConfigException if Java cannot compile the pattern; the schema must not load with the check missing.
     */
    static Pattern compileJsonSchemaPattern(String key, String pattern)
        {
        try
            {
            // compiled on its own first: wrapped, an invalid pattern such as "a)|(.*" would compile into one
            // that matches everything, and the check would silently do nothing.
            Pattern.compile(pattern);
            return Pattern.compile("(?s:.*?)(?:"+pattern+")(?s:.*)");
            }
        catch (PatternSyntaxException e)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID,
                    "JSON schema entry "+LogSafe.text(key)+": pattern is not a valid Java regular expression: "+LogSafe.text(pattern));
            }
        }

    /**
     * @return true if the JSON type of the node admits strings: no "type" (the default here), "string",
     *         or a type list containing "string".
     */
    static boolean isStringTyped(JsonNode node)
        {
        if (!node.has("type"))
            { return true; }
        JsonNode type = node.get("type");
        if (type.isArray())
            {
            for (JsonNode t : type)
                {
                if ("string".equalsIgnoreCase(t.asString()))
                    { return true; }
                }
            return false;
            }
        return "string".equalsIgnoreCase(type.asString());
        }

    /**
     * @return the JSON type to map: "string" if none is given. For a type list (asString() on it would throw, and
     *         fail the whole schema): "string" if the list contains it, since values read from files arrive as text
     *         and the read-path validator checks them as strings then (with "pattern"); otherwise the first type
     *         other than "null", e.g. "integer" for ["integer", "null"].
     */
    static String primaryType(JsonNode node)
        {
        if (!node.has("type"))
            { return "string"; }
        JsonNode type = node.get("type");
        if (!type.isArray())
            { return type.asString(); }
        if (isStringTyped(node))
            { return "string"; }
        for (JsonNode t : type)
            {
            if (!"null".equalsIgnoreCase(t.asString()))
                { return t.asString(); }
            }
        return "string";
        }

    private ConfigEntryType inferType(JsonNode node)
        {
        String typeStr = primaryType(node);
        String format = node.has("format") ? node.get("format").asString() : null;

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
            List<Error> errors = jsonSchema.validate(root);
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
        ConfigSource source = defaultLayer.getSource();
        for (Map.Entry<String, ConfigSchemaEntry> entry : inferredEntries.entrySet())
            {
            if (entry.getValue().getDefault() != null)
                {
                ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
                meta.setSpecification(entry.getValue());
                defaultLayer.putEntry(entry.getKey(), new GenericConfigEntryLeaf(entry.getKey(), entry.getValue().getDefault(), ConfigEntryType.STRING, meta));
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
        ConfigEntrySpecification spec = inferredEntries.get(fullKey);
        return (spec != null) ? spec : NullConfigEntrySpecification.INSTANCE;
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
