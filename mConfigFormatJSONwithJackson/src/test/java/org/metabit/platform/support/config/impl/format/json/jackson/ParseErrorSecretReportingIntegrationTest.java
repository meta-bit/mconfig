package org.metabit.platform.support.config.impl.format.json.jackson;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.NullConfigEntrySpecification;
import org.metabit.platform.support.config.source.core.DefaultLayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End to end: a broken JSON file read by the filesystem storage. The parse error report (log line and event carry the
 * same text) shows the parser's quote of the content unless the schema marks the entry at the error position SECRET.
 */
class ParseErrorSecretReportingIntegrationTest
{
    private static final String BROKEN = "{\n  \"db\": {\n    \"password\": s3cr3t-VALUE\n  }\n}";

    private Path tempDir;

    @BeforeEach
    void createFile() throws IOException
        {
        tempDir = Files.createTempDirectory("mconfig-parse-error-test");
        Files.write(tempDir.resolve("network.json"), BROKEN.getBytes(StandardCharsets.UTF_8));
        }

    @AfterEach
    void removeFile() throws IOException
        {
        Files.deleteIfExists(tempDir.resolve("network.json"));
        Files.deleteIfExists(tempDir);
        }

    private ConfigFactory factory(boolean showDetails) throws ConfigCheckedException
        {
        DefaultConfigFactoryBuilder builder = new DefaultConfigFactoryBuilder("metabit", "parseErrorTest");
        builder.setTestMode(true);
        builder.setTestConfigPaths(ConfigScope.USER, List.of(tempDir.toAbsolutePath().toString()));
        builder.setFeature(ConfigFeature.EVENTS_DETAIL_LEVEL, "FAILURES_ONLY");
        builder.setFeature(ConfigFeature.LOG_PARSE_ERROR_DETAILS, showDetails);
        return builder.build();
        }

    private static String parseReport(ConfigFactory factory)
        {
        for (ConfigEvent event : factory.getEvents())
            {
            if (event.getDomain() == ConfigEvent.Domain.PARSE)
                { return event.getMessage(); }
            }
        fail("no PARSE event recorded");
        return null;
        }

    @Test
    void secretEntryWithholdsTheParserText() throws Exception
        {
        try (ConfigFactory factory = factory(false))
            {
            factory.getConfig("network", schema("db/password"));
            String report = parseReport(factory);
            assertFalse(report.contains("s3cr3t"), report);
            assertTrue(report.contains("withheld"), report);
            assertTrue(report.contains("db/password"), report);
            assertTrue(report.contains("line 3"), report);
            }
        }

    @Test
    void withoutSecretTheParserTextIsShown() throws Exception
        {
        try (ConfigFactory factory = factory(false))
            {
            factory.getConfig("network");
            String report = parseReport(factory);
            assertTrue(report.contains("s3cr3t"), report);
            }
        }

    @Test
    void switchShowsTheParserTextForSecrets() throws Exception
        {
        try (ConfigFactory factory = factory(true))
            {
            factory.getConfig("network", schema("db/password"));
            assertTrue(parseReport(factory).contains("s3cr3t"));
            }
        }

    /** a schema marking the given keys SECRET; the schema parser module is not on this module's test classpath. */
    private static ConfigSchema schema(String... secretKeys)
        {
        Map<String, ConfigSchemaEntry> entries = new HashMap<>();
        for (String key : secretKeys)
            { entries.put(key, new ConfigSchemaEntry(key, ConfigEntryType.STRING).setSecret(true)); }
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
