package org.metabit.platform.support.config.schema.impl.ext;

import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.ConfigSchemaException;

import org.metabit.library.format.json.JsonStreamParser;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.metabit.library.format.json.JsonStreamWriter.jsonEscapeStringContent;

/**
 * <p>JsonConfigSchemaParser class.</p>
 * Parses JSON config schema to internal runtime representation and provides methods for generating JSON schema output.
 */
public class JsonConfigSchemaParser
{
    private static final char   DOUBLE_QUOTE                = '\"';
    private static final char   COMMA                       = ',';
    private static final char   BACKSLASH                   = '\\';

        public static String generateJson(ConfigSchema schema, String name, boolean filterHidden, boolean sanitizeSecrets)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"NAME\": \"").append(jsonEscapeStringContent(name)).append("\",\n");
        if (schema.getVersion() != null)
            {
            sb.append("  \"VERSION\": \"").append(jsonEscapeStringContent(schema.getVersion())).append("\",\n");
            }
        sb.append("  \"ENTRIES\": [\n");

        List<String> keys = new ArrayList<>(schema.getEntryKeys());
        Collections.sort(keys);

        boolean first = true;
        for (String key : keys)
            {
            ConfigSchemaEntry entry = (ConfigSchemaEntry) schema.getSpecification(key);
            if (filterHidden && entry.isHidden()) continue;

            if (!first) sb.append(",\n");
            sb.append("    ").append(generateEntryJson(entry, sanitizeSecrets));
            first = false;
            }

        sb.append("\n  ]\n");
        sb.append("}\n");
        return sb.toString();
        }

    private static String generateEntryJson(ConfigSchemaEntry entry, boolean sanitizeSecrets)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"KEY\": \"").append(jsonEscapeStringContent(entry.getKey())).append("\", ");
        sb.append("\"TYPE\": \"").append(entry.getType().name()).append("\"");

        if (entry.getDescription() != null && !entry.getDescription().isEmpty())
            {
            sb.append(", \"DESCRIPTION\": \"").append(jsonEscapeStringContent(entry.getDescription())).append("\"");
            }

        if (entry.getDefault() != null)
            {
            if (!sanitizeSecrets || !entry.isSecret())
                {
                sb.append(", \"DEFAULT\": \"").append(jsonEscapeStringContent(entry.getDefault())).append("\"");
                }
            }

        if (entry.getValidationPattern() != null)
            {
            sb.append(", \"PATTERN\": \"").append(jsonEscapeStringContent(entry.getValidationPattern())).append("\"");
            }

        if (entry.isSecret())
            {
            sb.append(", \"SECRET\": true");
            }

        if (entry.isHidden())
            {
            sb.append(", \"HIDDEN\": true");
            }

        if (entry.getWriteScope() != null)
            {
            sb.append(", \"WRITE_SCOPE\": \"").append(entry.getWriteScope().name()).append("\"");
            }

        if (entry.getMinArity() != (entry.isMandatory() ? 1 : 0))
            {
            sb.append(", \"MINARITY\": ").append(entry.getMinArity());
            }

        if (entry.getMaxArity() != (entry.isList() ? -1 : 1))
            {
            sb.append(", \"MAXARITY\": ").append(entry.getMaxArity());
            }

        sb.append("}");
        return sb.toString();
        }

    /**
     * main parsing function.
     * convert all four format variants to a map of ConfigSchemas.
     * for the format variant without a name, the String key is "".
     *
     * @param jsonFormattedConfigSchema the string supposed to be a JSON ConfigSchema format
     * @param ctx                       the internal context to use in parsing
     * @return all ConfigSchemas parsed
     *
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the input is not in a valid config schema format.
     */
    public static Map<String, ConfigSchema> parseJSON(String jsonFormattedConfigSchema, ConfigFactoryInstanceContext ctx)
            throws ConfigCheckedException
        {
        Map<String, ConfigSchema> resultmap = new HashMap<>();

        org.metabit.platform.support.config.interfaces.ConfigLoggingInterface logger = (ctx != null) ? ctx.getLogger() : null;
        MultiFormatConfigSchemaParser mfcsp = new MultiFormatConfigSchemaParser(resultmap, logger);
        try
            {
            JsonStreamParser.parseJson(jsonFormattedConfigSchema, mfcsp);
            }
        catch (JsonStreamParser.JsonParsingException ex)
            {
            // logger: the Schema was not good.
            throw new ConfigSchemaException(ex.getMessage(), ex, ex.getLine(), ex.getColumn());
            }
        catch (IllegalArgumentException ex)
            {
            // logger: the Schema was not good.
            throw new ConfigCheckedException(ex);
            }
        return resultmap;
        }

    // mapping class internal to the Schema converter.
    static class ConfigSchemaSetter extends org.metabit.library.format.json.DummyJsonStreamConsumer
    {
        enum ConfigEntryLabel
        {NONE, TYPE, KEY, DESCRIPTION, FLAGS, DEFAULT, PATTERN, SCOPES, SECRET, WRITE_SCOPE}

        private final ConfigSchemaEntry   to;
        private final Map<String, Object> data = new HashMap<>();
        private String currentKey;
        private List<Object> currentArray;

        ConfigSchemaSetter(ConfigSchemaEntry target)
            {
            to = target;
            }

        @Override
        public void consumeObjectEntryStart(int line, int column, int level, String key)
            {
            currentKey = key;
            }

        @Override
        public void consumeObjectEntryEnd(int line, int column, int level)
            {
            currentKey = null;
            }

        @Override
        public void consumeString(int line, int column, int level, String string)
            {
            addValue(string);
            }

        @Override
        public void consumeTrue(int line, int column, int level)
            {
            addValue(Boolean.TRUE);
            }

        @Override
        public void consumeFalse(int line, int column, int level)
            {
            addValue(Boolean.FALSE);
            }

        private void addValue(Object value)
            {
            if (currentArray != null)
                {
                currentArray.add(value);
                }
            else if (currentKey != null)
                {
                data.put(currentKey, value);
                }
            }

        @Override
        public void consumeArrayStart(int line, int column, int level)
            {
            currentArray = new ArrayList<>();
            }

        @Override
        public void consumeArrayEnd(int line, int column, int level)
            {
            if (currentKey != null)
                {
                data.put(currentKey, currentArray);
                }
            currentArray = null;
            }

        @Override
        public void consumeObjectEnd(int line, int column, int level)
            {
            // For single entry parsing, we expect only one main object.
            // We apply data when that object ends.
            applyData(to, data);
            }

        static void applyData(ConfigSchemaEntry entry, Map<String, Object> data)
            {
            // use a normalized map for case-insensitive lookup
            Map<String, Object> norm = new HashMap<>();
            // When adding new features to the scheme format, ensure they are added to this set
            // and handled in the logic below. Keys in MANDATORY block are checked against knownMandatoryKeys.
            // See devdoc/config_schemes_internals.md for more details.
            Set<String> knownKeys = new HashSet<>(Arrays.asList("KEY", "TYPE", "DESCRIPTION", "DEFAULT", "PATTERN", "SECRET", "SCOPES", "FLAGS", "MANDATORY", "HIDDEN", "MINARITY", "MAXARITY", "ARITY", "EXISTS", "IS_DIRECTORY", "IS_FILE", "CAN_WRITE", "AFTER", "BEFORE", "REQUIRE_OFFSET", "WRITE_SCOPE"));
            data.forEach((k, v) -> norm.put(k.toUpperCase(), v));

            if (norm.containsKey("KEY")) entry.key = (String) norm.get("KEY");
            if (norm.containsKey("TYPE"))
                {
                String t = String.valueOf(norm.get("TYPE")).toUpperCase();
                entry.type = ConfigEntryType.valueOf(t);
                }

            if (norm.containsKey("MINARITY"))
                {
                entry.setMinArity(((Number) norm.get("MINARITY")).intValue());
                }
            if (norm.containsKey("MAXARITY"))
                {
                entry.setMaxArity(((Number) norm.get("MAXARITY")).intValue());
                }
            if (norm.containsKey("ARITY"))
                {
                String arity = String.valueOf(norm.get("ARITY"));
                if (arity.contains(".."))
                    {
                    String[] parts = arity.split("\\.\\.");
                    entry.setMinArity(parseArityPart(parts[0]));
                    entry.setMaxArity(parseArityPart(parts[1]));
                    }
                else
                    {
                    int val = parseArityPart(arity);
                    entry.setMinArity(val);
                    entry.setMaxArity(val);
                    }
                }
            
            if (norm.containsKey("DESCRIPTION"))
                {
                Object desc = norm.get("DESCRIPTION");
                if (desc instanceof Map)
                    {
                    Map<?, ?> m = (Map<?, ?>) desc;
                    Map<String, String> descriptions = new HashMap<>();
                    m.forEach((k, v) -> descriptions.put(String.valueOf(k), String.valueOf(v)));
                    entry.setDescriptions(descriptions);
                    }
                else
                    {
                    entry.description = String.valueOf(desc);
                    }
                }
            if (norm.containsKey("DEFAULT")) entry.defaultValue = (String) norm.get("DEFAULT");
            
            if (norm.containsKey("PATTERN"))
                {
                entry.setValidationPattern((String) norm.get("PATTERN"));
                }
            
            Object secret = norm.get("SECRET");
            if (secret != null)
                {
                if (secret instanceof Boolean) entry.setSecret((Boolean) secret);
                else entry.setSecret(Boolean.parseBoolean(String.valueOf(secret)));
                }
            
            Object scopes = norm.get("SCOPES");
            if (scopes instanceof List)
                {
                EnumSet<ConfigScope> set = EnumSet.noneOf(ConfigScope.class);
                for (Object s : (List<?>) scopes)
                    {
                    set.add(ConfigScope.valueOf(String.valueOf(s).toUpperCase()));
                    }
                entry.setScopes(set);
                }

            Object flags = norm.get("FLAGS");
            if (flags instanceof List)
                {
                EnumSet<ConfigEntry.ConfigEntryFlags> set = EnumSet.noneOf(ConfigEntry.ConfigEntryFlags.class);
                for (Object f : (List<?>) flags)
                    {
                    set.add(ConfigEntry.ConfigEntryFlags.valueOf(String.valueOf(f).toUpperCase()));
                    }
                entry.setFlags(set);
                }

            if (norm.containsKey("HIDDEN"))
                {
                Object hidden = norm.get("HIDDEN");
                if (hidden instanceof Boolean) entry.setHidden((Boolean) hidden);
                else entry.setHidden(Boolean.parseBoolean(String.valueOf(hidden)));
                }

            if (norm.containsKey("WRITE_SCOPE"))
                {
                entry.setWriteScope(ConfigScope.valueOf(String.valueOf(norm.get("WRITE_SCOPE")).toUpperCase()));
                }

            Object mandatoryBlock = norm.get("MANDATORY");
            if (mandatoryBlock instanceof Map)
                {
                Map<?, ?> m = (Map<?, ?>) mandatoryBlock;
                
                // If a key is added here, it means older versions of the library will reject schemes 
                // that use this feature inside a MANDATORY block, which is the intended future-proofing behavior.
                List<String> knownMandatoryKeys = Arrays.asList("EXISTS", "IS_DIRECTORY", "IS_FILE", "CAN_WRITE", "AFTER", "BEFORE", "REQUIRE_OFFSET", "SCHEME", "REQUIRE_PATH");

                if (entry.type == ConfigEntryType.FILEPATH)
                    {
                    Map<String, Object> pathFlags = new HashMap<>();
                    List<String> pathKeys = Arrays.asList("EXISTS", "IS_DIRECTORY", "IS_FILE", "CAN_WRITE");
                    for (Object k : m.keySet())
                        {
                        String key = String.valueOf(k).toUpperCase();
                        if (pathKeys.contains(key))
                            {
                            pathFlags.put(key, m.get(k));
                            }
                        }
                    entry.setPathValidationFlags(pathFlags);
                    }

                if (entry.type == ConfigEntryType.DATE || entry.type == ConfigEntryType.TIME || entry.type == ConfigEntryType.DATETIME)
                    {
                    Map<String, Object> temporalFlags = new HashMap<>();
                    List<String> temporalKeys = Arrays.asList("AFTER", "BEFORE", "REQUIRE_OFFSET");
                    for (Object k : m.keySet())
                        {
                        String key = String.valueOf(k).toUpperCase();
                        if (temporalKeys.contains(key))
                            {
                            temporalFlags.put(key, m.get(k));
                            }
                        }
                    entry.setTemporalValidationFlags(temporalFlags);
                    }

                for (Object k : m.keySet())
                    {
                    String featureName = String.valueOf(k).toUpperCase();
                    if (!knownMandatoryKeys.contains(featureName))
                        {
                        entry.setHasUnknownMandatoryFeatures(true);
                        break;
                        }
                    }
                }
            }

        private static int parseArityPart(String part)
            {
            part = part.trim();
            if ("*".equals(part) || "n".equalsIgnoreCase(part))
                {
                return -1;
                }
            return Integer.parseInt(part);
            }

        @Override public void consumeNumberInteger(int line, int column, int level, int i) { addValue(i); }
        @Override public void consumeNumberLong(int line, int column, int level, long l) { addValue(l); }
        @Override public void consumeNumberDouble(int line, int column, int level, double v) { addValue(v); }
        @Override public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger) { addValue(bigInteger); }
        @Override public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal) { addValue(bigDecimal); }
    }

    /**
     * <p>MultiFormatConfigSchemaParser class.</p>
     * Implementation of {@link JsonStreamParser.JsonStreamConsumer} that handles
     * multiple JSON formats for configuration schemes.
     * 
     * <p><b>Implementation Note:</b> This parser uses a stack-based approach for 
     * state management to support hierarchical JSON structures (like the {@code MANDATORY} 
     * block). Future changes should ensure that any nested objects or arrays correctly 
     * push their parent state onto the respective stacks and pop it back during 
     * the end-of-structure consumption to maintain data integrity and linkage. 
     * See {@code devdoc/config_schemes_internals.md} for more details.</p>
     */
    private static class MultiFormatConfigSchemaParser extends org.metabit.library.format.json.DummyJsonStreamConsumer
    {
        private final ConfigLoggingInterface    logger;
        private final Map<String, ConfigSchema> resultStorage;

        private enum ParsingScope { ROOT, SCHEME_LIST, SCHEME, ENTRY_LIST, ENTRY, NESTED_OBJECT, NESTED_ARRAY }
        private final Deque<ParsingScope> scopeStack = new ArrayDeque<>();
        private final Deque<String> keyStack = new ArrayDeque<>();
        
        private String currentKey;
        private Map<String, Object> currentObjectData;
        private final Deque<Map<String, Object>> objectStack = new ArrayDeque<>();
        private List<Object> currentArrayData;
        private final Deque<List<Object>> arrayStack = new ArrayDeque<>();

        public MultiFormatConfigSchemaParser(Map<String, ConfigSchema> result, ConfigLoggingInterface logger)
            {
            this.resultStorage = result;
            this.logger = logger;
            scopeStack.push(ParsingScope.ROOT);
            currentObjectData = new HashMap<>(); // Initialize root object
            }

        @Override
        public void consumeObjectStart(int line, int column, int level)
            {
            String key = (currentKey != null) ? currentKey : "";
            keyStack.push(key);
            
            ParsingScope parentScope = scopeStack.peek();
            if (parentScope == ParsingScope.ROOT || parentScope == ParsingScope.SCHEME_LIST || parentScope == ParsingScope.ENTRY_LIST || (parentScope == ParsingScope.SCHEME && "entries".equalsIgnoreCase(key)))
                {
                ParsingScope nextScope;
                if (parentScope == ParsingScope.ENTRY_LIST || (parentScope == ParsingScope.SCHEME && "entries".equalsIgnoreCase(key)) || (parentScope == ParsingScope.SCHEME_LIST && key.isEmpty()))
                    {
                    nextScope = ParsingScope.ENTRY;
                    }
                else
                    {
                    nextScope = ParsingScope.SCHEME;
                    }
                scopeStack.push(nextScope);
                }
            else
                {
                scopeStack.push(ParsingScope.NESTED_OBJECT); 
                }
            
            if (currentObjectData != null)
                {
                objectStack.push(currentObjectData);
                }
            currentObjectData = new HashMap<>();
            currentKey = null; // Correctly reset currentKey here
            }

        @Override
        public void consumeObjectEnd(int line, int column, int level)
            {
            ParsingScope currentScope = scopeStack.pop();
            String objKey = keyStack.pop();
            Map<String, Object> data = currentObjectData;
            
            currentObjectData = objectStack.isEmpty() ? null : objectStack.pop();

            if (currentObjectData != null && !objKey.isEmpty())
                {
                currentObjectData.put(objKey, data);
                }

            if (currentScope == ParsingScope.ENTRY)
                {
                processEntry(data, objKey);
                }
            else if (currentScope == ParsingScope.SCHEME)
                {
                boolean isEntry = data.containsKey("key") || data.containsKey("KEY");
                if (isEntry)
                    {
                    processEntry(data, objKey);
                    }
                else
                    {
                    processSchema(data, objKey);
                    }
                }
            }

        private void processSchema(Map<String, Object> data, String parentName)
            {
            Map<String, Object> norm = new HashMap<>();
            data.forEach((k, v) -> norm.put(k.toUpperCase(), v));

            String name = (String) norm.getOrDefault("NAME", parentName);
            if (name == null) name = "";
            ConfigSchemaImpl schema = (ConfigSchemaImpl) resultStorage.computeIfAbsent(name, k -> new ConfigSchemaImpl());
            if (norm.containsKey("VERSION"))
                {
                schema.setVersion(String.valueOf(norm.get("VERSION")));
                }
            Object entries = norm.get("ENTRIES");
            if (entries instanceof List)
                {
                List<?> entriesList = (List<?>) entries;
                for (Object entryObj : entriesList)
                    {
                    if (entryObj instanceof ConfigSchemaEntry)
                        {
                        schema.addSchemaEntry((ConfigSchemaEntry) entryObj);
                        }
                    else if (entryObj instanceof Map)
                        {
                        ConfigSchemaEntry entry = new ConfigSchemaEntry();
                        ConfigSchemaSetter.applyData(entry, (Map<String, Object>) entryObj);
                        schema.addSchemaEntry(entry);
                        }
                    }
                }
            
            // Handle the case where entries were added to resultStorage with name "" but should have been here
            if (resultStorage.containsKey("") && name != null && !name.isEmpty())
                {
                ConfigSchema anonymous = resultStorage.get("");
                for (String key : anonymous.getEntryKeys())
                    {
                    schema.addSchemaEntry((ConfigSchemaEntry) anonymous.getSpecification(key));
                    }
                resultStorage.remove("");
                }
            }

        /**
         * Processes a single configuration entry from the parsed JSON data.
         * 
         * <p>If the entry contains unknown features within a {@code MANDATORY} block, 
         * a {@link ConfigException} with {@link ConfigException.ConfigExceptionReason#UNKNOWN_MANDATORY_FEATURE} 
         * is thrown to ensure future-proofing. See {@code devdoc/config_schemes_internals.md} 
         * for the mechanism details.</p>
         * 
         * @param data the map of parsed JSON properties for the entry
         * @param parentName the name of the parent schema (if any)
         */
        private void processEntry(Map<String, Object> data, String parentName)
            {
            // System.out.println("[DEBUG_LOG] processEntry map: " + data);
            ConfigSchemaEntry entry = new ConfigSchemaEntry();
            ConfigSchemaSetter.applyData(entry, data);
            
            if (entry.hasUnknownMandatoryFeatures())
                {
                throw new ConfigException(ConfigException.ConfigExceptionReason.UNKNOWN_MANDATORY_FEATURE, "Unknown mandatory feature for entry '" + entry.getKey() + "'");
                }

            ParsingScope parentScope = scopeStack.peek();
            if (parentScope == ParsingScope.ENTRY_LIST)
                {
                currentArrayData.add(entry);
                }
            else if (parentScope == ParsingScope.ROOT || parentScope == ParsingScope.SCHEME_LIST || parentScope == ParsingScope.SCHEME || parentScope == ParsingScope.ENTRY)
                {
                // Format 1 or 3: entries directly under the config name or at top
                String name = (parentScope == ParsingScope.SCHEME_LIST || parentScope == ParsingScope.SCHEME || parentScope == ParsingScope.ENTRY) ? parentName : "";
                if (name == null) name = "";
                ConfigSchema schema = resultStorage.computeIfAbsent(name, k -> new ConfigSchemaImpl());
                schema.addSchemaEntry(entry);
                }
            }

        @Override
        public void consumeObjectEntryStart(int line, int column, int level, String key)
            {
            currentKey = key;
            }

        @Override
        public void consumeObjectEntryEnd(int line, int column, int level)
            {
            currentKey = null; 
            }

        @Override
        public void consumeArrayStart(int line, int column, int level)
            {
            String key = currentKey != null ? currentKey : "";
            keyStack.push(key);
            ParsingScope parentScope = scopeStack.peek();
            if (parentScope == ParsingScope.ROOT)
                {
                scopeStack.push(ParsingScope.SCHEME_LIST); // Could be Format 1 or 3
                }
            else if (parentScope == ParsingScope.SCHEME && "entries".equalsIgnoreCase(currentKey))
                {
                scopeStack.push(ParsingScope.ENTRY_LIST);
                }
            else
                {
                scopeStack.push(ParsingScope.NESTED_ARRAY);
                }
            currentArrayData = new ArrayList<>();
            arrayStack.push(currentArrayData);
            }

        @Override
        public void consumeArrayEnd(int line, int column, int level)
            {
            scopeStack.pop();
            String arrKey = keyStack.pop();
            List<Object> data = arrayStack.pop();
            currentArrayData = arrayStack.peek();
            
            if (currentObjectData != null && !arrKey.isEmpty())
                {
                currentObjectData.put(arrKey, data);
                }
            }

        @Override
        public void consumeString(int line, int column, int level, String string)
            {
            addValue(string);
            }

        @Override
        public void consumeTrue(int line, int column, int level)
            {
            addValue(Boolean.TRUE);
            }

        @Override
        public void consumeFalse(int line, int column, int level)
            {
            addValue(Boolean.FALSE);
            }

        private void addValue(Object value)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(value);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, value);
                }
            }

        @Override
        public void consumeNumberInteger(int line, int column, int level, int i)
            {
            addValue(i);
            }

        @Override
        public void consumeNumberLong(int line, int column, int level, long l)
            {
            addValue(l);
            }

        @Override
        public void consumeNumberDouble(int line, int column, int level, double v)
            {
            addValue(v);
            }

        @Override
        public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
            {
            addValue(bigInteger);
            }

        @Override
        public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
            {
            addValue(bigDecimal);
            }
    }
}
