package org.metabit.platform.support.config.schema.test;
import org.metabit.platform.support.config.schema.*;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.schema.impl.ext.JsonConfigSchemaParser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSchemaArityTest
{

    @Test
    public void testArityParsing() throws ConfigCheckedException {
        String json = "{" +
                "  \"NAME\": \"my-app\"," +
                "  \"ENTRIES\": [" +
                "    { \"KEY\": \"mandatoryKey\", \"TYPE\": \"STRING\", \"FLAGS\": [\"MANDATORY\"] }," +
                "    { \"KEY\": \"optionalKey\", \"TYPE\": \"STRING\" }," +
                "    { \"KEY\": \"listKey\", \"TYPE\": \"MULTIPLE_STRINGS\", \"ARITY\": \"1..*\" }," +
                "    { \"KEY\": \"fixedList\", \"TYPE\": \"ENUM_SET\", \"ARITY\": \"2\" }" +
                "  ]" +
                "}";

        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        
        Map<String, ConfigSchema> schemes = JsonConfigSchemaParser.parseJSON(json, ctx);
        ConfigSchema scheme = schemes.get("my-app");
        
        assertNotNull(scheme);
        
        ConfigEntrySpecification specMandatory = scheme.getSpecification("mandatoryKey");
        assertEquals(1, specMandatory.getMinArity());
        assertEquals(1, specMandatory.getMaxArity());
        assertFalse(specMandatory.isList());
        
        ConfigEntrySpecification specOptional = scheme.getSpecification("optionalKey");
        assertEquals(0, specOptional.getMinArity());
        assertEquals(1, specOptional.getMaxArity());
        
        ConfigEntrySpecification specList = scheme.getSpecification("listKey");
        assertEquals(1, specList.getMinArity());
        assertEquals(-1, specList.getMaxArity());
        assertTrue(specList.isList());
        
        ConfigEntrySpecification specFixed = scheme.getSpecification("fixedList");
        assertEquals(2, specFixed.getMinArity());
        assertEquals(2, specFixed.getMaxArity());
        assertTrue(specFixed.isList());
    }

    @Test
    public void testArityValidation() throws ConfigCheckedException {
        ConfigSchemaEntry spec = new ConfigSchemaEntry("myList", ConfigEntryType.MULTIPLE_STRINGS);
        spec.setMinArity(2);
        spec.setMaxArity(4);

        // Valid case: 3 items
        ConfigEntry entry3 = createListEntry("myList", java.util.Arrays.asList("a", "b", "c"));
        assertTrue(spec.validateEntry(entry3));

        // Invalid: too few
        ConfigEntry entry1 = createListEntry("myList", java.util.Arrays.asList("a"));
        assertFalse(spec.validateEntry(entry1));

        // Invalid: too many
        ConfigEntry entry5 = createListEntry("myList", java.util.Arrays.asList("a", "b", "c", "d", "e"));
        assertFalse(spec.validateEntry(entry5));
    }

    private ConfigEntry createListEntry(String key, java.util.List<String> values) {
        return new org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf(key, values, ConfigEntryType.MULTIPLE_STRINGS, null);
    }
}
