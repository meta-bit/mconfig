package org.metabit.platform.support.config.impl.format.jsonschema;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * standard JSON Schema support, on Jackson 3 and json-schema-validator 3.x only (TD1).
 */
class JsonSchemaConfigSchemaTest
{
    private static final String SCHEMA = "{"
            +"\"$schema\": \"http://json-schema.org/draft-07/schema#\","
            +"\"x-mconfig-config\": \"network\","
            +"\"type\": \"object\","
            +"\"properties\": {"
            +"  \"server\": { \"type\": \"object\", \"properties\": {"
            +"     \"port\": { \"type\": \"integer\", \"minimum\": 1, \"maximum\": 65535, \"default\": 8080, \"description\": \"listen port\" },"
            +"     \"url\":  { \"type\": \"string\", \"format\": \"uri\" } } },"
            +"  \"password\": { \"type\": \"string\", \"x-mconfig-secret\": true, \"x-mconfig-write-scope\": \"user\" }"
            +"} }";

    @Test
    void entriesAreInferredWithMConfigExtensions()
        {
        JsonSchemaConfigSchema schema = new JsonSchemaConfigSchema(SCHEMA);
        assertEquals("network", schema.getConfigName());
        assertEquals(3, schema.getEntryKeys().size(), schema.getEntryKeys().toString());

        ConfigSchemaEntry port = (ConfigSchemaEntry) schema.getSpecification("server/port");
        assertEquals(ConfigEntryType.NUMBER, port.getType());
        assertEquals("8080", port.getDefault());
        assertEquals("listen port", port.getDescription());

        assertEquals(ConfigEntryType.URI, ((ConfigSchemaEntry) schema.getSpecification("server/url")).getType());

        ConfigSchemaEntry password = (ConfigSchemaEntry) schema.getSpecification("password");
        assertTrue(password.isSecret(), "x-mconfig-secret must mark the entry as secret");
        assertEquals(ConfigScope.USER, password.getWriteScope());
        }

    @Test
    void valuesAreValidatedAgainstTheSchema()
        {
        JsonSchemaConfigSchema schema = new JsonSchemaConfigSchema(SCHEMA);
        assertTrue(schema.checkConfigEntryValidity("server/port", number("server/port", "8443")));
        assertFalse(schema.checkConfigEntryValidity("server/port", number("server/port", "70000")), "above maximum");
        }

    @Test
    void schemaJsonIsReturned()
        {
        String json = new JsonSchemaConfigSchema(SCHEMA).toJSON("network", false, false);
        assertNotNull(json);
        assertTrue(json.contains("\"properties\""), json);
        }

    @Test
    void patternHasJsonSchemaSearchSemantics()
        {
        ConfigSchemaEntry unanchored = (ConfigSchemaEntry) new JsonSchemaConfigSchema(withPattern("ab")).getSpecification("name");
        assertTrue(unanchored.validationPattern.matcher("xxabyy").matches(), "unanchored pattern matches anywhere");
        assertFalse(unanchored.validationPattern.matcher("xxayby").matches());

        ConfigSchemaEntry anchored = (ConfigSchemaEntry) new JsonSchemaConfigSchema(withPattern("^ab$")).getSpecification("name");
        assertTrue(anchored.validationPattern.matcher("ab").matches());
        assertFalse(anchored.validationPattern.matcher("xab").matches(), "anchors still apply");
        }

    @Test
    void patternIsNotReadAsValidatorKeyword()
        {
        // "port" is a named validator in mConfig schemas; in JSON Schema it is just a regular expression
        ConfigSchemaEntry entry = (ConfigSchemaEntry) new JsonSchemaConfigSchema(withPattern("port")).getSpecification("name");
        assertTrue(entry.validationPattern.matcher("airport").matches());
        }

    @Test
    void invalidPatternFailsInsteadOfDroppingEntries()
        {
        // "(?<" is not a valid Java group; before, all entries after it were dropped silently, secret ones included
        String schema = "{\"type\":\"object\",\"properties\":{"
                +"\"name\":{\"type\":\"string\",\"pattern\":\"(?<\"},"
                +"\"password\":{\"type\":\"string\",\"x-mconfig-secret\":true}}}";
        // the validator (json-schema-validator 3.x) rejects it already; the own check stays as a second guard
        assertThrows(RuntimeException.class, ()->new JsonSchemaConfigSchema(schema));
        ConfigException e = assertThrows(ConfigException.class, ()->JsonSchemaConfigSchema.compileJsonSchemaPattern("name", "(?<"));
        assertTrue(e.getMessage().contains("name"), e.getMessage());
        }

    /** wrapped for search semantics, "a)|(.*" would compile to "(?s).*?(?:a)|(.*).*", which matches everything. */
    @Test
    void patternInvalidOnItsOwnIsRejectedNotWrappedIntoAMatchAll()
        {
        for (String broken : new String[]{"a)|(.*", "x)|(", ")"})
            {
            ConfigException e = assertThrows(ConfigException.class, ()->JsonSchemaConfigSchema.compileJsonSchemaPattern("name", broken), broken);
            assertEquals(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID, e.getReason());
            }
        }

    /** JSON Schema applies "pattern" to every string, also one with a "format": both must hold. */
    @Test
    void patternAndFormatMustBothHold()
        {
        String json = "{\"type\":\"object\",\"properties\":{"
                +"\"endpoint\":{\"type\":\"string\",\"format\":\"uri\",\"pattern\":\"^https://\"},"
                +"\"day\":{\"type\":\"string\",\"format\":\"date\",\"pattern\":\"^2026-\"},"
                +"\"mail\":{\"type\":\"string\",\"format\":\"email\",\"pattern\":\"@example\\\\.com$\"},"
                +"\"opt\":{\"type\":[\"string\",\"null\"],\"pattern\":\"^x\"},"
                +"\"count\":{\"type\":\"integer\",\"pattern\":\"^1\"}}}";
        JsonSchemaConfigSchema schema = new JsonSchemaConfigSchema(json);

        ConfigSchemaEntry endpoint = (ConfigSchemaEntry) schema.getSpecification("endpoint");
        assertEquals(ConfigEntryType.URI, endpoint.getType());
        assertTrue(endpoint.validateEntry(string("endpoint", "https://example.com/api")));
        assertFalse(endpoint.validateEntry(string("endpoint", "http://example.com/api")), "a URI, but not matching the pattern");
        assertFalse(endpoint.validateEntry(string("endpoint", "https://exa mple.com")), "matching the pattern, but no URI");

        ConfigSchemaEntry day = (ConfigSchemaEntry) schema.getSpecification("day");
        assertTrue(day.validateEntry(string("day", "2026-11-16")));
        assertFalse(day.validateEntry(string("day", "2025-11-16")), "a date, but not matching the pattern");
        assertFalse(day.validateEntry(string("day", "2026-13-45")), "matching the pattern, but no date");

        // a format mConfig does not map stays STRING: only the pattern is checked
        ConfigSchemaEntry mail = (ConfigSchemaEntry) schema.getSpecification("mail");
        assertEquals(ConfigEntryType.STRING, mail.getType());
        assertTrue(mail.validateEntry(string("mail", "joe@example.com")));
        assertFalse(mail.validateEntry(string("mail", "joe@example.org")));

        ConfigSchemaEntry opt = (ConfigSchemaEntry) schema.getSpecification("opt");
        assertNotNull(opt.validationPattern, "a type list containing \"string\" gets the pattern");

        assertEquals(ConfigEntryType.STRING, opt.getType());

        ConfigSchemaEntry count = (ConfigSchemaEntry) schema.getSpecification("count");
        assertNull(count.validationPattern, "JSON Schema ignores \"pattern\" for non-string types");
        }

    /** a type list made the whole schema fail to load (Jackson 3: asString() on an array throws). */
    @Test
    void typeListIsReadByItsFirstNonNullType()
        {
        String json = "{\"type\":\"object\",\"properties\":{"
                +"\"port\":{\"type\":[\"integer\",\"null\"]},"
                +"\"name\":{\"type\":[\"null\",\"string\"]},"
                +"\"after\":{\"type\":\"boolean\"}}}";
        JsonSchemaConfigSchema schema = new JsonSchemaConfigSchema(json);
        assertEquals(ConfigEntryType.NUMBER, ((ConfigSchemaEntry) schema.getSpecification("port")).getType());
        assertEquals(ConfigEntryType.STRING, ((ConfigSchemaEntry) schema.getSpecification("name")).getType());
        assertEquals(ConfigEntryType.BOOLEAN, ((ConfigSchemaEntry) schema.getSpecification("after")).getType(), "entries after it are kept");
        }

    private static GenericConfigEntryLeaf string(String key, String value)
        {
        return new GenericConfigEntryLeaf(key, value, ConfigEntryType.STRING, new ConfigEntryMetadata(null));
        }

    /**
     * a value is checked on read (checkConfigEntryValidity: the JSON Schema validator on the original schema) and on
     * write (ConfigSchemaEntry.validateEntry: the inferred mConfig entry). Both must give the same verdict, else a
     * value can be written that is then skipped when read, or the other way round. Values arrive as strings, as
     * from a file. The only accepted difference: formats mConfig does not map (email, ...) are asserted on read only.
     */
    @Test
    void readAndWriteValidationAgree()
        {
        String json = "{\"type\":\"object\",\"properties\":{"
                +"\"endpoint\":{\"type\":\"string\",\"format\":\"uri\",\"pattern\":\"^https://\"},"
                +"\"day\":{\"type\":\"string\",\"format\":\"date\",\"pattern\":\"^2026-\"},"
                +"\"mail\":{\"type\":\"string\",\"format\":\"email\",\"pattern\":\"example\\\\.com$\"},"
                +"\"dot\":{\"type\":\"string\",\"pattern\":\"^a.b$\"},"
                +"\"either\":{\"type\":[\"integer\",\"string\"],\"pattern\":\"^x\"},"
                +"\"nullable\":{\"type\":[\"string\",\"null\"],\"pattern\":\"^x\"}}}";
        JsonSchemaConfigSchema schema = new JsonSchemaConfigSchema(json);
        String[][] cases = {
                {"endpoint", "https://example.com/api", "true"}, {"endpoint", "http://example.com/api", "false"},
                {"endpoint", "https://exa mple.com", "false"},
                {"day", "2026-11-16", "true"}, {"day", "2025-11-16", "false"}, {"day", "2026-13-45", "false"},
                {"mail", "joe@example.com", "true"}, {"mail", "joe@example.org", "false"},
                {"dot", "axb", "true"}, {"dot", "a\nb", "false"},           // "." does not match a line break
                {"either", "xyz", "true"}, {"either", "42", "false"}, {"either", "abc", "false"},
                {"nullable", "xyz", "true"}, {"nullable", "abc", "false"}};
        for (String[] c : cases)
            {
            GenericConfigEntryLeaf entry = string(c[0], c[1]);
            boolean expected = Boolean.parseBoolean(c[2]);
            String label = c[0]+" = \""+c[1].replace("\n", "\\n")+"\"";
            assertEquals(expected, schema.checkConfigEntryValidity(c[0], entry), "read: "+label);
            assertEquals(expected, ((ConfigSchemaEntry) schema.getSpecification(c[0])).validateEntry(entry), "write: "+label);
            }

        // documented difference (0.8.17): a format mConfig does not map is asserted on read only
        GenericConfigEntryLeaf notAnEmail = string("mail", "not-an-email example.com");
        assertFalse(schema.checkConfigEntryValidity("mail", notAnEmail), "read asserts format email");
        assertTrue(((ConfigSchemaEntry) schema.getSpecification("mail")).validateEntry(notAnEmail), "write checks the pattern only");
        }

    private static String withPattern(String pattern)
        {
        return "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"pattern\":\""+pattern+"\"}}}";
        }

    private static GenericConfigEntryLeaf number(String key, String value)
        {
        return new GenericConfigEntryLeaf(key, value, ConfigEntryType.NUMBER, new ConfigEntryMetadata(null));
        }
}
//___EOF___
