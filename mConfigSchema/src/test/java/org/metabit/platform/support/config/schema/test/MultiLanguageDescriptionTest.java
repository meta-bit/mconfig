package org.metabit.platform.support.config.schema.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;
import org.metabit.platform.support.config.schema.ConfigSchemaFactory;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiLanguageDescriptionTest {

    @Test
    public void testMultiLanguageDescription() {
        ConfigSchemaEntry entry = new ConfigSchemaEntry("test", ConfigEntryType.STRING);
        entry.setDescription("Default Description");
        entry.setDescription("en", "English Description");
        entry.setDescription("de", "Deutsche Beschreibung");

        assertEquals("English Description", entry.getDescription(Locale.ENGLISH));
        assertEquals("Deutsche Beschreibung", entry.getDescription(Locale.GERMAN));
        assertEquals("English Description", entry.getDescription(Locale.UK));
        assertEquals("Default Description", entry.getDescription(Locale.FRENCH));
    }

    @Test
    public void testJsonParsingWithMultiLanguageDescription() throws Exception {
        String json = "{\n" +
                "  \"name\": \"testScheme\",\n" +
                "  \"entries\": [\n" +
                "    {\n" +
                "      \"KEY\": \"myKey\",\n" +
                "      \"TYPE\": \"STRING\",\n" +
                "      \"DESCRIPTION\": {\n" +
                "        \"en\": \"English JSON Description\",\n" +
                "        \"de\": \"Deutsche JSON Beschreibung\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        ConfigFactory factory = ConfigFactoryBuilder.create("test", "test").build();
        DefaultConfigFactory df = (DefaultConfigFactory) factory;
        
        ConfigSchemaFactory schemaFactory = ConfigSchemaFactory.create();
        java.util.Map<String, ConfigSchema> schemes = schemaFactory.createSchemasFromJSON(json, df.ctx);
        ConfigSchema schema = schemes.get("testScheme");
        ConfigSchemaEntry entry = (ConfigSchemaEntry) schema.getSpecification("myKey");

        assertEquals("English JSON Description", entry.getDescription(Locale.ENGLISH));
        assertEquals("Deutsche JSON Beschreibung", entry.getDescription(Locale.GERMAN));
    }
}
