package org.metabit.platform.support.config.impl.format.json.jackson;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JSONEventIntegrationTest
{
    @Test
    public void testJsonWithCommentsProducesEvent() throws Exception
    {
        // 1. Prepare a JSON file with comments
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "mconfig-event-test-" + System.currentTimeMillis());
        tempDir.toFile().mkdirs();
        File jsonFile = new File(tempDir.toFile(), "test.json");
        try (FileWriter writer = new FileWriter(jsonFile))
        {
            writer.write("// This is a comment\n{\n  \"key\": \"value\"\n}");
        }

        try
        {
            // 2. Setup ConfigFactory in TEST_MODE with explicit test directory
            DefaultConfigFactoryBuilder builder = new DefaultConfigFactoryBuilder("metabit", "testapp");
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, List.of(tempDir.toAbsolutePath().toString()));
            builder.setFeature(ConfigFeature.EVENTS_DETAIL_LEVEL, "FAILURES_ONLY");
            ConfigFactory factory = builder.build();

            // 3. Attempt to get config - this should trigger discovery and parsing
            factory.getConfig("test");
            
            // 4. Verify event recorded in factory
            List<ConfigEvent> events = factory.getEvents();
            boolean found = false;
            for (ConfigEvent ev : events)
            {
                if (ev.getDomain() == ConfigEvent.Domain.PARSE &&
                    (ev.getKind() == ConfigEvent.Kind.UNSUPPORTED_SYNTAX || ev.getKind() == ConfigEvent.Kind.FAILED_GENERIC))
                {
                    found = true;
                    assertEquals("JSONwithJackson", ev.getFormatId());
                    assertTrue(ev.getAttributes().containsKey("line") || ev.getAttributes().containsKey("column") || ev.getAttributes().containsKey("filePath"));
                    break;
                }
            }
            assertTrue(found, "Should have found PARSE/UNSUPPORTED_SYNTAX event");
        }
        finally
        {
            jsonFile.delete();
            tempDir.toFile().delete();
        }
    }
}
