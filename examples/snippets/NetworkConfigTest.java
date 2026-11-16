package com.example.snippets;

// snippet:start test-mode
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkConfigTest
{
    @TempDir Path tempDir;

    @Test
    void readsPortFromUserScope() throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "demoApp");
        builder.setTestMode(true);
        builder.setTestConfigPaths(ConfigScope.USER, List.of(tempDir.toAbsolutePath().toString()));
        builder.setFeature(ConfigFeature.CREATE_MISSING_PATHS, true);
        builder.setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true);

        try (ConfigFactory factory = builder.build())
            {
            Configuration cfg = factory.getConfig("network");
            cfg.put("server/port", 12345, ConfigScope.USER);
            cfg.flush();

            assertEquals(12345, cfg.getInteger("server/port"));
            }
        // @TempDir cleans up; no real user config was touched.
        }
}
// snippet:end test-mode
//___EOF___
