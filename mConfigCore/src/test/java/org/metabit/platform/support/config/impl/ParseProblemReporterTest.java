package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.schema.NullConfigSchema;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parse problems: the parser's own text is shown unless the content may hold a secret value (rules 1-4).
 */
class ParseProblemReporterTest
{
    private static final String SECRET_TEXT = "Unrecognized token 's3cr3t-VALUE'";

    @Test
    void safeMessageNeverContainsParserText()
        {
        ConfigParseException e = problem("db/password");
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertTrue(e.getMessage().contains("line 3"), e.getMessage());
        assertTrue(e.getMessage().contains("db/password"), e.getMessage());
        assertNull(e.getCause(), "the parser exception must not be the cause");
        assertEquals("IllegalStateException", e.getErrorType());
        }

    @Test
    void keyPathFromContentIsEscapedInTheMessage()
        {
        ConfigParseException e = ConfigParseException.builder("TEST", "not valid TEST content")
                .position(1, 1).keyPath("a\nFORGED log line").build();
        assertFalse(e.getMessage().contains("\n"), e.getMessage());
        assertTrue(e.getMessage().contains("a\\nFORGED"), e.getMessage());
        assertEquals("a\nFORGED log line", e.getKeyPath(), "the accessor keeps the raw key");
        }

    @Test
    void withoutSchemaOrSecretEntriesTheTextIsShown()
        {
        ConfigFactorySettings settings = settings(false);
        assertTrue(ParseProblemReporter.isDetailAllowed(problem(null), null, false, settings));
        assertTrue(ParseProblemReporter.isDetailAllowed(problem(null), NullConfigSchema.INSTANCE, false, settings));
        assertTrue(ParseProblemReporter.isDetailAllowed(problem(null), schema("server/port"), false, settings), "rule 3: no SECRET entry");
        }

    @Test
    void secretStoresAndBootstrapNeverShowTheText()
        {
        assertFalse(ParseProblemReporter.isDetailAllowed(problem("server/port"), null, true, settings(false)), "rule 1");
        }

    @Test
    void keyPathAtAboveOrBelowASecretWithholdsTheText()
        {
        ConfigSchema schema = schema("server/port", "!db/password", "!token");
        ConfigFactorySettings settings = settings(false);
        assertTrue(ParseProblemReporter.isDetailAllowed(problem("server/port"), schema, false, settings));
        assertFalse(ParseProblemReporter.isDetailAllowed(problem("db/password"), schema, false, settings), "at");
        assertFalse(ParseProblemReporter.isDetailAllowed(problem("db"), schema, false, settings), "above");
        assertFalse(ParseProblemReporter.isDetailAllowed(problem("token/part"), schema, false, settings), "below");
        assertTrue(ParseProblemReporter.isDetailAllowed(problem("dbx"), schema, false, settings), "a name prefix is not a branch");
        assertFalse(ParseProblemReporter.isDetailAllowed(problem(null), schema, false, settings), "rule 3: position unknown");
        }

    @Test
    void switchShowsTheTextRegardless()
        {
        assertTrue(ParseProblemReporter.isDetailAllowed(problem("db/password"), schema("!db/password"), true, settings(true)));
        }

    @Test
    void reportWithholdsOrShowsEscapedText()
        {
        List<String> lines = new ArrayList<>();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings(false));
        ctx.setLogger(new NullLogging()
            {
            @Override public void warn(String msg) { lines.add(msg); }
            });

        ParseProblemReporter.report(problem("db/password"), null, ctx, "network", schema("!db/password"), false);
        assertEquals(1, lines.size());
        assertFalse(lines.get(0).contains("s3cr3t"), lines.get(0));
        assertTrue(lines.get(0).contains("withheld"), lines.get(0));
        assertTrue(lines.get(0).contains("network"), lines.get(0));

        lines.clear();
        ConfigParseException injected = ConfigParseException.builder("TEST", "not valid")
                .position(1, 1).parserDetail("bad token\nFORGED log line").build();
        ParseProblemReporter.report(injected, null, ctx, "network", null, false);
        assertTrue(lines.get(0).contains("bad token"), lines.get(0));
        assertFalse(lines.get(0).contains("\n"), "parser text must be escaped: "+lines.get(0));
        }

    private static ConfigParseException problem(String keyPath)
        {
        return ConfigParseException.builder("TEST", "not valid TEST content")
                .position(3, 7)
                .keyPath(keyPath)
                .parserCause(new IllegalStateException(SECRET_TEXT))
                .build();
        }

    private static ConfigFactorySettings settings(boolean showDetails)
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactorySettings.initDefaults(settings);
        settings.setBoolean(ConfigFeature.LOG_PARSE_ERROR_DETAILS, showDetails);
        return settings;
        }

    /** keys prefixed with "!" are SECRET. */
    private static ConfigSchema schema(String... keys)
        {
        Map<String, ConfigSchemaEntry> entries = new HashMap<>();
        for (String k : keys)
            {
            boolean secret = k.startsWith("!");
            String key = secret ? k.substring(1) : k;
            entries.put(key, new ConfigSchemaEntry(key, ConfigEntryType.STRING).setSecret(secret));
            }
        return new ConfigSchema()
            {
            @Override public boolean checkConfigEntryValidity(String fullKey, ConfigEntry entry) { return true; }
            @Override public void addSchemaEntry(String format1) { }
            @Override public void addSchemaEntry(String fullKey, ConfigEntryType type, String validitySchema, Object defaultValue, String description, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes) { }
            @Override public void addSchemaEntry(ConfigSchemaEntry entry) { }
            @Override public void init(ConfigFactoryInstanceContext ctx) { }
            @Override public void transferDefaults(DefaultLayer defaultLayer) { }
            @Override public Set<String> getEntryKeys() { return entries.keySet(); }
            @Override public ConfigEntrySpecification getSpecification(String fullKey)
                {
                ConfigEntrySpecification spec = entries.get(fullKey);
                return (spec != null) ? spec : NullConfigEntrySpecification.INSTANCE;
                }
            };
        }
}
//___EOF___
