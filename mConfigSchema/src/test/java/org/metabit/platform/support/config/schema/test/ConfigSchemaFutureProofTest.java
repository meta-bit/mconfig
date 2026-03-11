package org.metabit.platform.support.config.schema.test;
import org.metabit.platform.support.config.schema.*;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.schema.impl.ext.JsonConfigSchemaParser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemaFutureProofTest
{

    @Test
    public void testUnknownFeaturesAreIgnored() throws ConfigCheckedException {
        // Simple format: list of entries directly
        String json = "[" +
                "      {" +
                "        \"KEY\": \"myKey\"," +
                "        \"TYPE\": \"STRING\"," +
                "        \"UNKNOWN_FEATURE\": \"someValue\"" +
                "      }" +
                "]";

        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        
        Map<String, ConfigSchema> schemes = JsonConfigSchemaParser.parseJSON(json, ctx);
        // Simplified format usually maps to "" name
        ConfigSchema scheme = schemes.get("");
        
        assertNotNull(scheme, "Scheme '' should not be null. Found: " + schemes.keySet());
        assertNotNull(scheme.getSpecification("myKey"));
        assertEquals("myKey", scheme.getSpecification("myKey").getKey());
    }

    @Test
    public void testMandatoryUnknownFeatureShouldFail() {
        String json = "[" +
                "      {" +
                "        \"KEY\": \"myKey\"," +
                "        \"TYPE\": \"STRING\"," +
                "        \"MANDATORY\": { \"NEW_STRICT_FEATURE\": \"value\" }" +
                "      }" +
                "]";

        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        
        // Use a more robust check: expect it to throw during addSchemaEntry inside parseJSON
        assertThrows(RuntimeException.class, () -> {
            JsonConfigSchemaParser.parseJSON(json, ctx);
        });
    }
}
