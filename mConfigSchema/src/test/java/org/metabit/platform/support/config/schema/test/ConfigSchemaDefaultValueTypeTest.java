package org.metabit.platform.support.config.schema.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.impl.ext.JsonConfigSchemaParser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON permits DEFAULT to be a string, a number, a boolean or an array, and schema authors
 * write all of these. Before the fix, anything but a JSON string threw a ClassCastException
 * inside the parser, which surfaced as "no ConfigSchemaFactory can parse this JSON" - the
 * entire schema was silently discarded rather than just that one default.
 */
public class ConfigSchemaDefaultValueTypeTest
{
    private static ConfigSchema parse(String json) throws ConfigCheckedException
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        Map<String, ConfigSchema> schemas = JsonConfigSchemaParser.parseJSON(json, ctx);
        ConfigSchema schema = schemas.get("defaults-test");
        assertNotNull(schema, "schema must parse");
        return schema;
        }

    private static String defaultOf(ConfigSchema schema, String key) throws ConfigCheckedException
        {
        ConfigEntrySpecification spec = schema.getSpecification(key);
        assertNotNull(spec, "specification must exist for " + key);
        ConfigEntry entry = spec.getDefaultEntry();
        assertNotNull(entry, "default entry must exist for " + key);
        return entry.getValueAsString();
        }

    @Test
    public void testScalarDefaultsOfEveryJsonType() throws ConfigCheckedException
        {
        String json = "{"
                + "  \"NAME\": \"defaults-test\","
                + "  \"ENTRIES\": ["
                + "    { \"KEY\": \"quotedPort\",  \"TYPE\": \"NUMBER\",  \"DEFAULT\": \"8080\" },"
                + "    { \"KEY\": \"numericPort\", \"TYPE\": \"NUMBER\",  \"DEFAULT\": 8080 },"
                + "    { \"KEY\": \"bigNumber\",   \"TYPE\": \"NUMBER\",  \"DEFAULT\": 9007199254740993 },"
                + "    { \"KEY\": \"ratio\",       \"TYPE\": \"NUMBER\",  \"DEFAULT\": 0.5 },"
                + "    { \"KEY\": \"quotedFlag\",  \"TYPE\": \"BOOLEAN\", \"DEFAULT\": \"false\" },"
                + "    { \"KEY\": \"nativeFlag\",  \"TYPE\": \"BOOLEAN\", \"DEFAULT\": true },"
                + "    { \"KEY\": \"text\",        \"TYPE\": \"STRING\",  \"DEFAULT\": \"127.0.0.1\" }"
                + "  ]"
                + "}";

        ConfigSchema schema = parse(json);

        assertEquals("8080", defaultOf(schema, "quotedPort"));
        assertEquals("8080", defaultOf(schema, "numericPort"), "numeric DEFAULT must behave like the quoted form");
        assertEquals("9007199254740993", defaultOf(schema, "bigNumber"), "values beyond long must survive");
        assertEquals("0.5", defaultOf(schema, "ratio"));
        assertEquals("false", defaultOf(schema, "quotedFlag"));
        assertEquals("true", defaultOf(schema, "nativeFlag"), "boolean DEFAULT must behave like the quoted form");
        assertEquals("127.0.0.1", defaultOf(schema, "text"));
        }

    /**
     * An array default is stored comma-separated, which is the form
     * ConfigTypeConverter.toStringList() splits on for MULTIPLE_STRINGS entries.
     */
    @Test
    public void testArrayDefaultBecomesCommaSeparatedList() throws ConfigCheckedException
        {
        String json = "{"
                + "  \"NAME\": \"defaults-test\","
                + "  \"ENTRIES\": ["
                + "    { \"KEY\": \"features\", \"TYPE\": \"MULTIPLE_STRINGS\", \"DEFAULT\": [\"LOGGING\", \"METRICS\"] },"
                + "    { \"KEY\": \"single\",   \"TYPE\": \"MULTIPLE_STRINGS\", \"DEFAULT\": [\"ONLY\"] }"
                + "  ]"
                + "}";

        ConfigSchema schema = parse(json);

        assertEquals("LOGGING,METRICS", defaultOf(schema, "features"));
        assertEquals("ONLY", defaultOf(schema, "single"));
        }

    /**
     * The documented flat-array schema form must accept the same DEFAULT types
     * as the named {NAME, ENTRIES} form; both go through ConfigSchemaSetter.applyData.
     */
    @Test
    public void testFlatArraySchemaFormAcceptsNonStringDefaults() throws ConfigCheckedException
        {
        String json = "["
                + "  { \"KEY\": \"server/port\", \"TYPE\": \"NUMBER\",  \"DEFAULT\": 8080 },"
                + "  { \"KEY\": \"server/tls\",  \"TYPE\": \"BOOLEAN\", \"DEFAULT\": false }"
                + "]";

        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        Map<String, ConfigSchema> schemas = JsonConfigSchemaParser.parseJSON(json, ctx);

        assertFalse(schemas.isEmpty(), "flat array schema must parse");
        ConfigSchema schema = schemas.values().iterator().next();

        assertEquals("8080", defaultOf(schema, "server/port"));
        assertEquals("false", defaultOf(schema, "server/tls"));
        }
    @Test
    public void testStringEntryValidationForNumericTypeWithPattern() throws ConfigCheckedException
        {
        String json = "{"
                + "  \"NAME\": \"admin\","
                + "  \"ENTRIES\": ["
                + "    { \"KEY\": \"token.length\", \"TYPE\": \"NUMBER\", \"DEFAULT\": 34, \"PATTERN\": \"[22, 256]\" }"
                + "  ]"
                + "}";

        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        Map<String, ConfigSchema> schemas = JsonConfigSchemaParser.parseJSON(json, ctx);
        ConfigSchema schema = schemas.get("admin");
        assertNotNull(schema);

        // String entry with valid number within range [22, 256]
        ConfigEntry validEntry = new GenericConfigEntryLeaf("token.length", "40", ConfigEntryType.STRING, new ConfigEntryMetadata(null));
        assertTrue(schema.checkConfigEntryValidity("token.length", validEntry), "valid numeric string in range should pass");

        // String entry with invalid number out of range (8 < 22)
        ConfigEntry invalidRangeEntry = new GenericConfigEntryLeaf("token.length", "8", ConfigEntryType.STRING, new ConfigEntryMetadata(null));
        assertFalse(schema.checkConfigEntryValidity("token.length", invalidRangeEntry), "numeric string out of range should fail");

        // String entry with non-numeric string
        ConfigEntry notANumberEntry = new GenericConfigEntryLeaf("token.length", "abc", ConfigEntryType.STRING, new ConfigEntryMetadata(null));
        assertFalse(schema.checkConfigEntryValidity("token.length", notANumberEntry), "non-numeric string should fail");
        }

    @Test
    public void testStringEntryValidationForBooleanDateAndTime() throws ConfigCheckedException
        {
        String json = "{"
                + "  \"NAME\": \"defaults-test\","
                + "  \"ENTRIES\": ["
                + "    { \"KEY\": \"flag\", \"TYPE\": \"BOOLEAN\" },"
                + "    { \"KEY\": \"day\",  \"TYPE\": \"DATE\" },"
                + "    { \"KEY\": \"at\",   \"TYPE\": \"TIME\" }"
                + "  ]"
                + "}";
        ConfigSchema schema = parse(json);

        assertTrue(schema.checkConfigEntryValidity("flag", stringEntry("flag", "TRUE")));
        assertTrue(schema.checkConfigEntryValidity("flag", stringEntry("flag", "false")));
        // Boolean.valueOf("maybe") would silently read as false; validation must reject it (yes/no, on/off, 1/0 are literals since 0.8.17)
        assertTrue(schema.checkConfigEntryValidity("flag", stringEntry("flag", "yes")));
        assertFalse(schema.checkConfigEntryValidity("flag", stringEntry("flag", "maybe")));

        assertTrue(schema.checkConfigEntryValidity("day", stringEntry("day", "2026-10-08")));
        // unparseable strings must fail validation, not throw DateTimeParseException
        assertFalse(schema.checkConfigEntryValidity("day", stringEntry("day", "not-a-date")));

        assertTrue(schema.checkConfigEntryValidity("at", stringEntry("at", "12:30")));
        assertFalse(schema.checkConfigEntryValidity("at", stringEntry("at", "25:99")));
        }

    private static ConfigEntry stringEntry(String key, String value)
        {
        return new GenericConfigEntryLeaf(key, value, ConfigEntryType.STRING, new ConfigEntryMetadata(null));
        }
}
