package org.metabit.platform.support.config.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.ConfigSchemaFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MLIB-130: saveTo() must never write secret values; the omitted keys are reported as a config event.
 */
public class ConfigMapperSaveToSecretsTest
{
    @TempDir
    Path tempDir;

    @Test
    public void secretEntriesAreOmittedAndReported() throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "testapp");
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        try (ConfigFactory factory = builder.build())
            {
            ConfigSchemaFactory sf = ConfigSchemaFactory.create();
            ConfigSchema schema = sf.createSchema();
            ConfigSchemaEntry password = sf.createEntry("db/password", ConfigEntryType.STRING);
            password.setSecret(true);
            schema.addSchemaEntry(password);
            schema.addSchemaEntry(sf.createEntry("db/host", ConfigEntryType.STRING));

            Configuration config = factory.getConfig("savetest", schema);
            config.put("db/host", "dbserver.example", ConfigScope.SESSION);
            config.put("db/password", "s3cr3t-value", ConfigScope.SESSION);
            config.put("other/key", "plain-value", ConfigScope.SESSION); // not defined in the schema

            Path target = tempDir.resolve("export.json");
            new ConfigMapperImpl().saveTo(config, target);

            String written = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
            assertTrue(written.contains("dbserver.example"), "non-secret value must be exported: "+written);
            assertTrue(written.contains("plain-value"), "undefined, non-secret key must be exported: "+written);
            assertFalse(written.contains("s3cr3t-value"), "secret value must not be exported: "+written);
            assertFalse(written.contains("password"), "secret entry must be left out entirely: "+written);

            ConfigEvent event = config.getEvents().stream()
                    .filter(e -> "EXPORT_SECRETS_OMITTED".equals(e.getDetailCode()))
                    .findFirst().orElse(null);
            assertNotNull(event, "omitted secrets must be reported as a config event");
            assertEquals(ConfigEvent.Severity.WARNING, event.getSeverity());
            assertTrue(event.getMessage().contains("db/password"), "event names the omitted key: "+event.getMessage());
            assertFalse(event.getMessage().contains("s3cr3t-value"), "event must not contain the value");
            }
        }
}
//___EOF___
