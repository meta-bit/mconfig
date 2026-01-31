package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.library.format.json.JsonStreamParser;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;
import org.metabit.platform.support.config.scheme.ConfigSchemeFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SchemeTests
{
    @Test
    void checkSchemeParser()
        {
        // JsonStreamParser.JsonStreamConsumer consumer = new DebugJsonStreamConsumer();
        // String[] validTestStrings = {format3JSONtestSchemeVariant2};
        String[] validTestStrings = {format3JSONtestScheme, format3JSONtestSchemeVariant2};

        for (String s : validTestStrings)
            {
            JsonStreamParser jsp = new JsonStreamParser();
            JsonStreamParser.JsonStreamConsumer consumer = new SchemeConverterInDevelopment();
            jsp.parse(s, consumer);
            }
        return;
        }


    @Test
    void setSchemeAndTryDefaultingWithManualConstruction()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            {

            ConfigSchemeEntry testSchemeEntry = new ConfigSchemeEntry("testkey", ConfigEntryType.STRING).setDefault("testvalue");
            ConfigScheme testScheme = ConfigScheme.fromSchemeEntries(Set.of(testSchemeEntry));

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            cfg.setConfigScheme(testScheme); // this is an internal API, not intended for public use. may be removed later.

            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }


    final static String format3JSONtestScheme         = "[{\"name\":\"test\",\"entries\":[{\"key\":\"testkey\",\"type\":\"string\",\"default\":\"testvalue\"}]}]";
    final static String format3JSONtestSchemeVariant2 = "[{\"entries\":[{\"key\":\"testkey\",\"type\":\"string\",\"default\":\"testvalue\"}],\"name\":\"test\"}]";
    // format 2: scheme with single entry
    final static String format2JSONtestScheme         = "{\"name\":\"test\",\"entries\":[{\"key\":\"testkey\",\"type\":\"string\",\"default\":\"testvalue\"}]}";

    // not implemented as of now.
//     @Test
    void setSchemeAndTryDefaultingWithJSONsingleEntry()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5)
                .setFeature(ConfigFeature.LOGGING_TO_USE_IN_CONFIGLIB, "stderr-logger")
                .build())
            {


            System.out.println(format3JSONtestScheme);

            factory.addConfigScheme(CONFIG_NAME, format3JSONtestScheme);

            Configuration cfg = factory.getConfig(CONFIG_NAME);

            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }


    String format1SingleScheme    = "["+
            "{\"key\":\"testkey\",\"type\":\"string\",\"default\":\"testvalue\"}"+
            ",{\"key\":\"key2\",\"type\":\"string\",\"default\":\"otherValue\"}"+
            "]";
    String format2SchemeAWithName = "{\"name\":\"schemeA\","+format1SingleScheme+"}";
    String format2SchemeBWithName = "{ \"name\" : \"schemeB\" , "+format1SingleScheme+" } ";

    String format3singleSchemes = "["+format2SchemeAWithName+"]";
    String format3twoSchemes    = "["+format2SchemeAWithName+","+format2SchemeBWithName+"]";


    String format1SingleLanguageScheme = "["+
            "{\"key\":\"testkey\",\"type\":\"string\",\"default\":\"testvalue\",\"description\":\"the first test entry in the scheme\"}"+
            ",{\"key\":\"key2\",\"type\":\"string\",\"default\":\"otherValue\"},\"description\":\"the second test entry\"}"+
            "]";
    String format1MultiLanguageScheme  = "["+
            "{\"key\":\"testkey\",\"type\":\"string\",\"default\":\"testvalue\",\"description\":"+
            "{"+
            "\"en\":\"test me\","+
            "\"tlh\":\"Wah jih\""+
            "}"+
            ",{\"key\":\"key2\",\"type\":\"string\",\"default\":\"otherValue\"},\"description\":\"the second test entry\"}"+
            "]";

    // @Test
    void format1()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            {
            // format 1 - the scheme, plain
            factory.addConfigScheme(CONFIG_NAME, format1SingleScheme);

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }

    // @Test
    void format2()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            {
            // format 1 - the scheme, plain
            factory.addConfigScheme(CONFIG_NAME, format2SchemeAWithName);
            factory.addConfigScheme(CONFIG_NAME, format2SchemeBWithName);

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }


    // @Test
    void format3single()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            {
            // format 1 - the scheme, plain
            factory.addConfigScheme(CONFIG_NAME, format3singleSchemes);

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }


    // @Test
    void format3multiple()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            {
            // format 1 - the scheme, plain
            factory.addConfigScheme(CONFIG_NAME, format3twoSchemes);

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }

    // @Test
    void format4()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            {
            // format 1 - the scheme, plain
            factory.addConfigScheme(CONFIG_NAME, format1SingleScheme);

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value = cfg.getString("testkey");
            assertEquals("testvalue", value);
            }
        }

    @Test
    void testGetAllConfigurationKeysWithSchemesFlattened()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .build())
            {
            ConfigSchemeEntry schemeEntry1 = new ConfigSchemeEntry("key1", ConfigEntryType.STRING).setDefault("val1");
            ConfigScheme scheme = ConfigScheme.fromSchemeEntries(Set.of(schemeEntry1));

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            cfg.setConfigScheme(scheme);

            Map<String, ConfigEntrySpecification> keysWithSchemes = cfg.getAllConfigurationKeysWithSchemesFlattened(EnumSet.allOf(ConfigScope.class));

            assertTrue(keysWithSchemes.containsKey("key1"));
            assertTrue(keysWithSchemes.containsKey("testdata")); // From product/testconfig.properties
            assertNotNull(keysWithSchemes.get("key1"), "key1 should have a scheme entry");
            assertEquals("key1", keysWithSchemes.get("key1").getKey());
            assertNull(keysWithSchemes.get("testdata"), "testdata should not have a scheme entry");
            }
        }

    @Test
    void testNewValidators()
            throws ConfigCheckedException
        {
        ConfigSchemeFactory sf = ConfigSchemeFactory.create();
        ConfigEntryMetadata meta = new ConfigEntryMetadata(null);

        // port
        ConfigSchemeEntry portEntry = sf.createEntry("port", ConfigEntryType.NUMBER, "", null, "port", null, null);
        meta.setSpecification(portEntry);
        assertTrue(portEntry.validateEntry(new TypedConfigEntryLeaf("port", "80", ConfigEntryType.NUMBER, meta)));
        assertFalse(portEntry.validateEntry(new TypedConfigEntryLeaf("port", "99999", ConfigEntryType.NUMBER, meta)));

        // email
        ConfigSchemeEntry emailEntry = sf.createEntry("email", ConfigEntryType.STRING, "", null, "email", null, null);
        meta.setSpecification(emailEntry);
        assertTrue(emailEntry.validateEntry(new TypedConfigEntryLeaf("email", "test@example.com", ConfigEntryType.STRING, meta)));
        assertFalse(emailEntry.validateEntry(new TypedConfigEntryLeaf("email", "invalid-email", ConfigEntryType.STRING, meta)));

        // duration
        ConfigSchemeEntry durationEntry = sf.createEntry("duration", ConfigEntryType.DURATION, "", null, "duration", null, null);
        meta.setSpecification(durationEntry);
        assertTrue(durationEntry.validateEntry(new TypedConfigEntryLeaf("duration", "PT1H", ConfigEntryType.DURATION, meta)));
        assertFalse(durationEntry.validateEntry(new TypedConfigEntryLeaf("duration", "invalid", ConfigEntryType.DURATION, meta)));

        // size
        ConfigSchemeEntry sizeEntry = sf.createEntry("size", ConfigEntryType.NUMBER, "", null, "size", null, null);
        meta.setSpecification(sizeEntry);
        assertTrue(sizeEntry.validateEntry(new TypedConfigEntryLeaf("size", "1024", ConfigEntryType.NUMBER, meta)));
        assertFalse(sizeEntry.validateEntry(new TypedConfigEntryLeaf("size", "abc", ConfigEntryType.NUMBER, meta)));
        }

    public static final  String COMPANY_NAME     = "metabit";
    public static final  String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME      = "testconfig";
}
