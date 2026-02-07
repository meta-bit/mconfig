package org.metabit.platform.support.config.scheme.impl.ext;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;
import org.metabit.platform.support.config.scheme.ConfigSchemeException;

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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.metabit.library.format.json.JsonStreamWriter.jsonEscapeStringContent;

/**
 * <p>JsonConfigSchemeParser class.</p>
 * parses JSON config scheme to internal runtime representation
 * @version $Id: $Id
 */
public class JsonConfigSchemeParser
{
    private static final char   DOUBLE_QUOTE                = '\"';
    private static final String ENDSTRING_COLON_STARTSTRING = "\":\"";
    private static final String ENDSTRING_COLON             = "\":";
    private static final char   COMMA                       = ',';
    private static final char   BACKSLASH                   = '\\';

        // manually construct JSON form
    static String toCondensedForm(final ConfigSchemeEntry entry)
        {
        String s;
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // type is mandatory
        sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.TYPE.name()).append(ENDSTRING_COLON_STARTSTRING);
        sb.append(entry.type.name()); // which are defined so they are JSON-clean without escaping.
        sb.append(DOUBLE_QUOTE);
        sb.append(COMMA);
        // key is mandatory
        sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.KEY.name()).append(ENDSTRING_COLON_STARTSTRING);
        sb.append(jsonEscapeStringContent(entry.key));
        sb.append(DOUBLE_QUOTE);

        if (entry.getMinArity() != (entry.isMandatory() ? 1 : 0))
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append("MINARITY").append(ENDSTRING_COLON);
            sb.append(entry.getMinArity());
            }

        if (entry.getMaxArity() != (entry.isList() ? -1 : 1))
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append("MAXARITY").append(ENDSTRING_COLON);
            sb.append(entry.getMaxArity());
            }

        //@TODO this will allow to be a key-mapped set, with languages as keys.
        // straight-forward one-string description stays an option.
        // parsing needs to allow for both.
        s = entry.getDescription(); // currently string only.
        if (s != null)
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.DESCRIPTION.name()).append(ENDSTRING_COLON_STARTSTRING);
            sb.append(jsonEscapeStringContent(s));
            sb.append(DOUBLE_QUOTE);
            }

        s = entry.getDefault(); // currently string only.
        if (s != null)
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.DEFAULT.name()).append(ENDSTRING_COLON_STARTSTRING);
            sb.append(jsonEscapeStringContent(s));
            sb.append(DOUBLE_QUOTE);
            }


        // this will need heavy escaping...
        s = entry.getValidationPattern(); // currently string only.
        if (s != null)
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.PATTERN.name()).append(ENDSTRING_COLON_STARTSTRING);
            sb.append(jsonEscapeStringContent(s));
            sb.append(DOUBLE_QUOTE);
            }

        EnumSet<ConfigScope> scopes = entry.getScopes();
        if (!scopes.isEmpty())
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.SCOPES.name()).append(ENDSTRING_COLON);
            appendEnumList(sb, scopes);
            }

        EnumSet<ConfigEntry.ConfigEntryFlags> flags = entry.getFlags();
        if (!flags.isEmpty())
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.FLAGS.name()).append(ENDSTRING_COLON);
            appendEnumList(sb, flags);
            }

        if (entry.isSecret())
            {
            sb.append(COMMA);
            sb.append(DOUBLE_QUOTE).append(ConfigSchemeSetter.ConfigEntryLabel.SECRET.name()).append(ENDSTRING_COLON);
            sb.append("true");
            }

        // Add MANDATORY block if it has temporal or path validation flags
        if (entry.type == ConfigEntryType.FILEPATH || entry.type == ConfigEntryType.DATE || entry.type == ConfigEntryType.TIME || entry.type == ConfigEntryType.DATETIME)
            {
            Map<String, Object> flagsMap = null;
            if (entry.type == ConfigEntryType.FILEPATH) flagsMap = entry.getPathValidationFlags();
            else flagsMap = entry.getTemporalValidationFlags();

            if (flagsMap != null && !flagsMap.isEmpty())
                {
                sb.append(COMMA);
                sb.append(DOUBLE_QUOTE).append("MANDATORY").append(ENDSTRING_COLON).append("{");
                boolean first = true;
                for (Map.Entry<String, Object> flagEntry : flagsMap.entrySet())
                    {
                    if (!first) sb.append(COMMA);
                    else first = false;
                    sb.append(DOUBLE_QUOTE).append(flagEntry.getKey()).append(ENDSTRING_COLON);
                    Object val = flagEntry.getValue();
                    if (val instanceof Boolean) sb.append(val);
                    else sb.append(DOUBLE_QUOTE).append(jsonEscapeStringContent(String.valueOf(val))).append(DOUBLE_QUOTE);
                    }
                sb.append("}");
                }
            }

        sb.append('}');
        return sb.toString();
        }


    private static <T extends Enum<T>> void appendEnumList(StringBuilder sb, Collection<T> items)
        {
        sb.append('[');
        boolean isFirst = true;
        if (items != null && !items.isEmpty())
            {
            for (T item : items)
                {
                if (item == null) { continue; }
                if (!isFirst) { sb.append(COMMA); }
                else { isFirst = false; }
                sb.append(DOUBLE_QUOTE).append(quote(item.name())).append(DOUBLE_QUOTE);
                }
            }
        sb.append(']');
        return;
        }

// ... existing code ...

    private static String quote(String s)
        {
        // Minimal JSON string escaping for quotes and backslashes
        if (s == null || s.isEmpty()) { return s; }
        StringBuilder out = new StringBuilder(s.length()+8);
        for (int i = 0; i < s.length(); i++)
            {
            char c = s.charAt(i);
            if (c == DOUBLE_QUOTE || c == BACKSLASH) { out.append(BACKSLASH); }
            out.append(c);
            }
        return out.toString();
        }

// ... existing code ...

    static void setFromJSON(ConfigSchemeEntry entry, final String condensedForm)
            throws ConfigCheckedException
        {
        JsonStreamParser jsp = new JsonStreamParser(); // wasteful but threadsafe this way
        JsonConfigSchemeParser.ConfigSchemeSetter cec = new JsonConfigSchemeParser.ConfigSchemeSetter(entry);
        try
            {
            jsp.parse(condensedForm, cec);
            }
        catch (JsonStreamParser.JsonParsingException ex)
            {
            // logger: the Scheme was not good.
            throw new ConfigSchemeException(ex.getMessage(), ex, ex.getLine(), ex.getColumn());
            }
        catch (IllegalArgumentException ex)
            {
            // logger: the Scheme was not good.
            throw new ConfigCheckedException(ex);
            }
        return;
        }

    /**
     * main parsing function.
     * convert all four format variants to a map of ConfigSchemes.
     * for the format variant without a name, the String key is "".
     *
     * @param jsonFormattedConfigScheme the string supposed to be a JSON ConfigScheme format
     * @param ctx                       the internal context to use in parsing
     * @return all ConfigSchemes parsed
     *
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the input is not in a valid config scheme format.
     */
    public static Map<String, ConfigScheme> parseJSON(String jsonFormattedConfigScheme, ConfigFactoryInstanceContext ctx)
            throws ConfigCheckedException
        {
        Map<String, ConfigScheme> resultmap = new HashMap<>();

        JsonStreamParser jsp = new JsonStreamParser(); // wasteful but threadsafe this way
        JsonConfigSchemeParser.MultiFormatConfigSchemeParser mfcsp = new JsonConfigSchemeParser.MultiFormatConfigSchemeParser(resultmap, ctx.getLogger());
        try
            {
            jsp.parse(jsonFormattedConfigScheme, mfcsp);
            }
        catch (JsonStreamParser.JsonParsingException ex)
            {
            // logger: the Scheme was not good.
            throw new ConfigSchemeException(ex.getMessage(), ex, ex.getLine(), ex.getColumn());
            }
        catch (IllegalArgumentException ex)
            {
            // logger: the Scheme was not good.
            throw new ConfigCheckedException(ex);
            }
        return resultmap;
        }

    // mapping class internal to the Scheme converter.
    static class ConfigSchemeSetter implements JsonStreamParser.JsonStreamConsumer
    {
        enum ConfigEntryLabel
        {NONE, TYPE, KEY, DESCRIPTION, FLAGS, DEFAULT, PATTERN, SCOPES, SECRET}

        private final ConfigSchemeEntry to;
        private final Map<String, Object> data = new HashMap<>();
        private String currentKey;
        private List<Object> currentArray;

        ConfigSchemeSetter(ConfigSchemeEntry target)
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
            if (currentArray != null)
                {
                currentArray.add(string);
                }
            else if (currentKey != null)
                {
                data.put(currentKey, string);
                }
            }

        @Override
        public void consumeTrue(int line, int column, int level)
            {
            if (currentKey != null) data.put(currentKey, Boolean.TRUE);
            }

        @Override
        public void consumeFalse(int line, int column, int level)
            {
            if (currentKey != null) data.put(currentKey, Boolean.FALSE);
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

        static void applyData(ConfigSchemeEntry entry, Map<String, Object> data)
            {
            // use a normalized map for case-insensitive lookup
            Map<String, Object> norm = new HashMap<>();
            // When adding new features to the scheme format, ensure they are added to this set
            // and handled in the logic below. Keys in MANDATORY block are checked against knownMandatoryKeys.
            // See devdoc/config_schemes_internals.md for more details.
            Set<String> knownKeys = new HashSet<>(Arrays.asList("KEY", "TYPE", "DESCRIPTION", "DEFAULT", "PATTERN", "SECRET", "SCOPES", "FLAGS", "MANDATORY", "HIDDEN", "MINARITY", "MAXARITY", "ARITY", "EXISTS", "IS_DIRECTORY", "IS_FILE", "CAN_WRITE", "AFTER", "BEFORE", "REQUIRE_OFFSET"));
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

        @Override public void consumeNull(int line, int column, int level) {}
        @Override public void consumeNumberInteger(int line, int column, int level, int i) { if (currentKey != null) data.put(currentKey, i); }
        @Override public void consumeNumberLong(int line, int column, int level, long l) { if (currentKey != null) data.put(currentKey, l); }
        @Override public void consumeNumberDouble(int line, int column, int level, double v) { if (currentKey != null) data.put(currentKey, v); }
        @Override public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger) { if (currentKey != null) data.put(currentKey, bigInteger); }
        @Override public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal) { if (currentKey != null) data.put(currentKey, bigDecimal); }
        @Override public void consumeObjectStart(int line, int column, int level) {}
    }

    /**
     * <p>MultiFormatConfigSchemeParser class.</p>
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
    private static class MultiFormatConfigSchemeParser implements JsonStreamParser.JsonStreamConsumer
    {
        private final ConfigLoggingInterface logger;
        private final Map<String, ConfigScheme> resultStorage;

        private enum ParsingScope { ROOT, SCHEME_LIST, SCHEME, ENTRY_LIST, ENTRY, NESTED_OBJECT, NESTED_ARRAY }
        private final Deque<ParsingScope> scopeStack = new ArrayDeque<>();
        private final Deque<String> keyStack = new ArrayDeque<>();
        
        private String currentKey;
        private Map<String, Object> currentObjectData;
        private final Deque<Map<String, Object>> objectStack = new ArrayDeque<>();
        private List<Object> currentArrayData;
        private final Deque<List<Object>> arrayStack = new ArrayDeque<>();

        public MultiFormatConfigSchemeParser(Map<String, ConfigScheme> result, ConfigLoggingInterface logger)
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
                    processScheme(data, objKey);
                    }
                }
            }

        private void processScheme(Map<String, Object> data, String parentName)
            {
            Map<String, Object> norm = new HashMap<>();
            data.forEach((k, v) -> norm.put(k.toUpperCase(), v));

            String name = (String) norm.getOrDefault("NAME", parentName);
            if (name == null) name = "";
            ConfigSchemeImpl scheme = (ConfigSchemeImpl) resultStorage.computeIfAbsent(name, k -> new ConfigSchemeImpl());
            Object entries = norm.get("ENTRIES");
            if (entries instanceof List)
                {
                List<?> entriesList = (List<?>) entries;
                for (Object entryObj : entriesList)
                    {
                    if (entryObj instanceof ConfigSchemeEntry)
                        {
                        scheme.addSchemeEntry((ConfigSchemeEntry) entryObj);
                        }
                    else if (entryObj instanceof Map)
                        {
                        ConfigSchemeEntry entry = new ConfigSchemeEntry();
                        ConfigSchemeSetter.applyData(entry, (Map<String, Object>) entryObj);
                        scheme.addSchemeEntry(entry);
                        }
                    }
                }
            
            // Handle the case where entries were added to resultStorage with name "" but should have been here
            if (resultStorage.containsKey("") && name != null && !name.isEmpty())
                {
                ConfigScheme anonymous = resultStorage.get("");
                for (String key : anonymous.getEntryKeys())
                    {
                    scheme.addSchemeEntry((ConfigSchemeEntry) anonymous.getSpecification(key));
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
         * @param parentName the name of the parent scheme (if any)
         */
        private void processEntry(Map<String, Object> data, String parentName)
            {
            // System.out.println("[DEBUG_LOG] processEntry map: " + data);
            ConfigSchemeEntry entry = new ConfigSchemeEntry();
            ConfigSchemeSetter.applyData(entry, data);
            
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
                ConfigScheme scheme = resultStorage.computeIfAbsent(name, k -> new ConfigSchemeImpl());
                scheme.addSchemeEntry(entry);
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
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(string);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, string);
                }
            }

        @Override
        public void consumeTrue(int line, int column, int level)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(Boolean.TRUE);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, Boolean.TRUE);
                }
            }

        @Override
        public void consumeFalse(int line, int column, int level)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(Boolean.FALSE);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, Boolean.FALSE);
                }
            }

        @Override
        public void consumeNull(int line, int column, int level)
            {
            }

        @Override
        public void consumeNumberInteger(int line, int column, int level, int i)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(i);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, i);
                }
            }

        @Override
        public void consumeNumberLong(int line, int column, int level, long l)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(l);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, l);
                }
            }

        @Override
        public void consumeNumberDouble(int line, int column, int level, double v)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(v);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, v);
                }
            }

        @Override
        public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(bigInteger);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, bigInteger);
                }
            }

        @Override
        public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
            {
            ParsingScope scope = scopeStack.peek();
            if (scope == ParsingScope.SCHEME_LIST || scope == ParsingScope.ENTRY_LIST || scope == ParsingScope.NESTED_ARRAY)
                {
                if (currentArrayData != null) currentArrayData.add(bigDecimal);
                }
            else if (currentObjectData != null && currentKey != null)
                {
                currentObjectData.put(currentKey, bigDecimal);
                }
            }
    }
}
