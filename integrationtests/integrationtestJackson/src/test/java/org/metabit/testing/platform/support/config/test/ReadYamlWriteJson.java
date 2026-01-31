package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * a test to (ab)use mconfig as a conversion tool,
 * reading a configuration in one format (YAML),
 * writing it in another (JSON).
 */
public class ReadYamlWriteJson
{
    private static final String COMPANY_NAME     = "metabit";
    private static final String APPLICATION_NAME = "mConfigIT";


    @Test
    void testReadYamlWriteJson()
            throws IOException, ConfigCheckedException
        {
        Path tempDir = Files.createTempDirectory("mconfig-test-json");
        try
            {
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of(tempDir.toString()));
            builder.setFeature(ConfigFeature.TEST_MODE, true);
            builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, List.of("JSON"));
            // builder.setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, List.of("YAML"));

            try (ConfigFactory factory = builder.build())
                {
                Configuration cfg = factory.getConfig("readTestYaml");

                String val = cfg.getString("test");

/* @TODO
                // this does create a config, and it goes to the resources directory in the test resources.
                // the subdirectory is fine, but we are in test mode, so we need to ensure the config is written to the temp directory
                Configuration cfg2 = factory.getConfig("temporaryTestJson");
                cfg2.put("test", val, ConfigScope.USER);
 */
                }
            }
        finally
            {
            // cleanup
            Files.walk(tempDir)
                    .sorted((a, b)->b.compareTo(a))
                    .forEach(p->p.toFile().delete());
            }
        return;
        }
}
