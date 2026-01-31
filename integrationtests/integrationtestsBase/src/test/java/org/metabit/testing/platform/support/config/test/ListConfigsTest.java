package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigScope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public class ListConfigsTest
{
    @Test
    void testListConfigs() throws Exception
        {
        Path tempDir = Files.createTempDirectory("mconfig-test-list");
        try
            {
            Files.write(tempDir.resolve("config1.properties"), "key1=value1".getBytes());
            Files.write(tempDir.resolve("config2.bin"), new byte[]{1,2,3,4});
            Files.write(tempDir.resolve("not-a-config.txt"), "hello".getBytes());

            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "test");
            builder.setTestMode(true);
            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.TESTMODE_DIRECTORIES, Collections.singletonList("USER:" + tempDir.toAbsolutePath().toString()));
            // builder.setTestConfigPaths(ConfigScope.USER, Collections.singletonList(tempDir.toAbsolutePath().toString()));

            try (ConfigFactory factory = builder.build())
                {
                Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> configs = factory.listAvailableConfigurations();
                boolean foundConfig1 = false;
                boolean foundConfig2 = false;
                boolean foundNotAConfig = false;

                for (org.metabit.platform.support.config.ConfigDiscoveryInfo info : configs)
                    {
                    System.out.println("[DEBUG_LOG] Discovered: " + info);
                    if ("config1".equals(info.getConfigName()))
                        {
                        foundConfig1 = true;
                        Assertions.assertEquals(ConfigScope.USER, info.getScope());
                        Assertions.assertEquals("properties", info.getFormatID());
                        }
                    else if ("config2".equals(info.getConfigName()))
                        {
                        foundConfig2 = true;
                        Assertions.assertEquals(ConfigScope.USER, info.getScope());
                        Assertions.assertEquals("binary_file", info.getFormatID());
                        }
                    else if ("not-a-config".equals(info.getConfigName()))
                        {
                        foundNotAConfig = true;
                        Assertions.assertEquals(ConfigScope.USER, info.getScope());
                        Assertions.assertEquals("text_file", info.getFormatID());
                        }
                    }

                Assertions.assertTrue(foundConfig1, "Should contain config1");
                Assertions.assertTrue(foundConfig2, "Should contain config2 (bin)");
                Assertions.assertTrue(foundNotAConfig, "Should contain not-a-config (txt)");
                }
            }
        finally
            {
            Files.deleteIfExists(tempDir.resolve("config1.properties"));
            Files.deleteIfExists(tempDir.resolve("config2.bin"));
            Files.deleteIfExists(tempDir.resolve("not-a-config.txt"));
            Files.delete(tempDir);
            }
        }
}
