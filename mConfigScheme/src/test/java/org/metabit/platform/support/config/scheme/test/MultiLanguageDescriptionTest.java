package org.metabit.platform.support.config.scheme.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;
import org.metabit.platform.support.config.scheme.ConfigSchemeFactory;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiLanguageDescriptionTest {

    @Test
    public void testMultiLanguageDescription() {
        ConfigSchemeEntry entry = new ConfigSchemeEntry("test", ConfigEntryType.STRING);
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
        
        ConfigSchemeFactory schemeFactory = ConfigSchemeFactory.create();
        java.util.Map<String, ConfigScheme> schemes = schemeFactory.createSchemesFromJSON(json, df.ctx);
        ConfigScheme scheme = schemes.get("testScheme");
        ConfigSchemeEntry entry = (ConfigSchemeEntry) scheme.getSpecification("myKey");

        assertEquals("English JSON Description", entry.getDescription(Locale.ENGLISH));
        assertEquals("Deutsche JSON Beschreibung", entry.getDescription(Locale.GERMAN));
    }
}
