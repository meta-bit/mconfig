package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JacksonWriteTest
{
    private static final String COMPANY_NAME     = "metabit";
    private static final String APPLICATION_NAME = "mConfigIT";

    @Test
    void testWriteJson() throws IOException, ConfigCheckedException
        {
        Path tempDir = Files.createTempDirectory("mconfig-test-json");
        try
            {
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(tempDir.toString()));
            // specifically allow only the format module we're testing here.
            builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, List.of("JSONwithJackson"));
            
            try (ConfigFactory factory = builder.build())
                {
                Configuration cfg = factory.getConfig("writeTestJson");
                
                // Write multiple values, including nested ones
                cfg.put("simple", "value1", ConfigScope.USER);
                cfg.put("nested/key", "value2", ConfigScope.USER);
                cfg.put("deeply/nested/value", "value3", ConfigScope.USER);
                
                // Flush to ensure it's written to disk
                cfg.flush();
                
                // Verify file exists on disk
                Path expectedFile = tempDir.resolve("writeTestJson.json");
                assertTrue(Files.exists(expectedFile), "File should be created at " + expectedFile);
                
                String content = Files.readString(expectedFile);
                assertTrue(content.contains("\"simple\" : \"value1\""));
                assertTrue(content.contains("\"nested\" : {"));
                assertTrue(content.contains("\"key\" : \"value2\""));
                assertTrue(content.contains("\"deeply\" : {"));
                assertTrue(content.contains("\"nested\" : {"));
                assertTrue(content.contains("\"value\" : \"value3\""));
                
                // Verify reading it back through the API (new instance to be sure)
                }
            
            try (ConfigFactory factory2 = builder.build())
                {
                Configuration cfg2 = factory2.getConfig("writeTestJson");
                assertEquals("value1", cfg2.getString("simple"));
                assertEquals("value2", cfg2.getString("nested/key"));
                assertEquals("value3", cfg2.getString("deeply/nested/value"));
                }
            }
        finally
            {
            // cleanup
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> p.toFile().delete());
            }
        }
}
