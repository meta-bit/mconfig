package org.metabit.platform.support.config.schema.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaException;
import org.metabit.platform.support.config.schema.impl.ext.JsonConfigSchemaParser;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * a schema without entries fails loudly instead of loading as an empty schema (M5, fix part).
 * The object-keyed-by-configuration-key shape used to parse to an empty schema, leaving entries
 * without schema, including SECRET protection.
 */
public class ConfigSchemaEmptyShapeTest
{
    private final ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(new ConfigFactorySettings());

    @Test
    void objectKeyedByConfigurationKeyIsRejected()
        {
        ConfigSchemaException e = assertThrows(ConfigSchemaException.class, ()->JsonConfigSchemaParser.parseJSON(
                "{ \"db/password\": { \"TYPE\": \"STRING\", \"SECRET\": true } }", ctx));
        assertTrue(e.getMessage().contains("no entries"), e.getMessage());
        assertTrue(e.getMessage().contains("KEY"), "the message names the expected shape: "+e.getMessage());
        }

    @Test
    void emptyArrayIsRejected()
        {
        assertThrows(ConfigSchemaException.class, ()->JsonConfigSchemaParser.parseJSON("[]", ctx));
        }

    @Test
    void namedSchemaWithoutEntriesIsRejected()
        {
        ConfigSchemaException e = assertThrows(ConfigSchemaException.class, ()->JsonConfigSchemaParser.parseJSON(
                "{ \"NAME\": \"network\", \"ENTRYS\": [ { \"KEY\": \"port\", \"TYPE\": \"NUMBER\" } ] }", ctx));
        assertTrue(e.getMessage().contains("network"), e.getMessage());
        }

    @Test
    void validShapesStillLoad() throws ConfigCheckedException
        {
        Map<String, ConfigSchema> array = JsonConfigSchemaParser.parseJSON("[ { \"KEY\": \"port\", \"TYPE\": \"NUMBER\" } ]", ctx);
        assertEquals(1, array.get("").getEntryKeys().size());
        Map<String, ConfigSchema> named = JsonConfigSchemaParser.parseJSON("{ \"NAME\": \"network\", \"ENTRIES\": [ { \"KEY\": \"port\", \"TYPE\": \"NUMBER\" } ] }", ctx);
        assertEquals(1, named.get("network").getEntryKeys().size());
        }

    @Test
    void syntaxErrorKeepsTheParserMessage()
        {
        ConfigSchemaException e = assertThrows(ConfigSchemaException.class, ()->JsonConfigSchemaParser.parseJSON("[ { \"KEY\": } ]", ctx));
        assertTrue(e.getMessage().contains("unexpected character"), "the parser's message is kept: "+e.getMessage());
        }
}
//___EOF___
