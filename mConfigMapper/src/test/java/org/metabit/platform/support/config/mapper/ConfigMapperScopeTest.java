package org.metabit.platform.support.config.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigMapperScopeTest
{
    @TempDir
    Path tempDir;

    @Test
    public void testWriteToSpecificScope() throws Exception
    {
        Path productDir = tempDir.resolve("product");
        Path userDir = tempDir.resolve("user");
        Files.createDirectories(productDir);
        Files.createDirectories(userDir);

        // Create a product config file
        Path productFile = productDir.resolve("testconfig.properties");
        Files.write(productFile, Collections.singletonList("name=originalProductValue"));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "testapp");
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setTestConfigPaths(ConfigScope.PRODUCT, Collections.singletonList(productDir.toAbsolutePath().toString()));
        builder.setTestConfigPaths(ConfigScope.USER, Collections.singletonList(userDir.toAbsolutePath().toString()));
        ConfigFactory factory = builder.build();

        try (Configuration config = factory.getConfig("testconfig"))
            {
            // Initial state: value comes from product scope
            assertEquals("originalProductValue", config.getString("name"));
            ConfigEntry entry = config.getConfigEntryFromFullKey("name", EnumSet.allOf(ConfigScope.class));
            assertEquals(ConfigScope.PRODUCT, entry.getScope());

            ConfigMapper mapper = new ConfigMapperImpl();
            TestPojo pojo = new TestPojo();
            pojo.setName("newUserValue");

            ConfigCursor cursor = config.getConfigCursor();
            
            // Write to USER scope specifically
            int count = mapper.writeObject(pojo, cursor, EnumSet.of(ConfigScope.USER));
            assertTrue(count > 0, "Should have written at least one property");

            // Verify that the USER scope now has the value, and PRODUCT scope is overridden
            assertEquals("newUserValue", config.getString("name"));
            
            ConfigEntry newEntry = config.getConfigEntryFromFullKey("name", EnumSet.allOf(ConfigScope.class));
            assertEquals(ConfigScope.USER, newEntry.getScope());
            
            // Verify PRODUCT scope still has its original value (not modified)
            ConfigEntry productEntry = config.getConfigEntryFromFullKey("name", EnumSet.of(ConfigScope.PRODUCT));
            assertEquals("originalProductValue", productEntry.getValueAsString());
            }
        finally
            {
            factory.close();
            }
    }
}
