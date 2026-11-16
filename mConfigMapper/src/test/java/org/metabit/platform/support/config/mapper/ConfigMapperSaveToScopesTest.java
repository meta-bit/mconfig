package org.metabit.platform.support.config.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * saveTo(config, file, scopes) exports only what the given scopes hold (MLIB-138); the parameter used to be ignored.
 */
public class ConfigMapperSaveToScopesTest
{
    @TempDir
    Path tempDir;

    @Test
    public void onlyTheGivenScopesAreExported() throws Exception
        {
        Path userDir = tempDir.resolve("user");
        Files.createDirectories(userDir);
        Files.write(userDir.resolve("scopetest.properties"), java.util.Arrays.asList("shared=fromUser", "userOnly=u"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "testapp");
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setTestConfigPaths(ConfigScope.USER, Collections.singletonList(userDir.toAbsolutePath().toString()));
        try (ConfigFactory factory = builder.build())
            {
            Configuration config = factory.getConfig("scopetest");
            config.put("shared", "fromSession", ConfigScope.SESSION);
            config.put("sessionOnly", "s", ConfigScope.SESSION);
            assertEquals("fromSession", config.getString("shared"));

            Path userExport = tempDir.resolve("user-export.properties");
            new ConfigMapperImpl().saveTo(config, userExport, EnumSet.of(ConfigScope.USER));
            String written = new String(Files.readAllBytes(userExport), StandardCharsets.UTF_8);
            assertTrue(written.contains("fromUser"), "the USER value, although SESSION overrides it overall: "+written);
            assertTrue(written.contains("userOnly"), written);
            assertFalse(written.contains("fromSession"), written);
            assertFalse(written.contains("sessionOnly"), written);

            Path allExport = tempDir.resolve("all-export.properties");
            new ConfigMapperImpl().saveTo(config, allExport);
            String all = new String(Files.readAllBytes(allExport), StandardCharsets.UTF_8);
            assertTrue(all.contains("fromSession") && all.contains("sessionOnly") && all.contains("userOnly"), all);
            assertFalse(all.contains("fromUser"), "overridden value is not exported: "+all);
            }
        }
}
//___EOF___
