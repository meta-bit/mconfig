package org.metabit.platform.support.config.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the views of ConfigUtil promise on top of a real configuration with a schema:
 * read-only without exception, secrets handed on as secrets, errors reported instead of null.
 */
class ConfigurationViewsContractTest
{
    private static final String CONFIG_NAME = "viewtest";

    @TempDir
    Path tempDir;

    private ConfigFactory factory;
    private Configuration cfg;

    @BeforeEach
    void setUp()
            throws IOException, ConfigCheckedException
        {
        Files.write(tempDir.resolve(CONFIG_NAME+".properties"),
                "mysql.host=db1\nmysql.port=3306\nmysql.mode=fast\nmysql.password=s3cr3t-value\nother=x\n".getBytes(StandardCharsets.UTF_8));

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "CONFIGVIEWTEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, Collections.singletonList(tempDir.toAbsolutePath().toString()));
        factory = builder.build();

        Set<ConfigSchemaEntry> entries = new HashSet<>();
        entries.add(new ConfigSchemaEntry("mysql.host", ConfigEntryType.STRING));
        entries.add(new ConfigSchemaEntry("mysql.port", ConfigEntryType.NUMBER));
        entries.add(new ConfigSchemaEntry("mysql.mode", ConfigEntryType.STRING).setValidator(Pattern.compile("fast|safe")));
        entries.add(new ConfigSchemaEntry("mysql.password", ConfigEntryType.STRING).setSecret(true));
        entries.add(new ConfigSchemaEntry("other", ConfigEntryType.STRING));

        cfg = factory.getConfig(CONFIG_NAME);
        cfg.setConfigSchema(ConfigSchema.fromSchemaEntries(entries));
        assertEquals("db1", cfg.getString("mysql.host"), "test setup: the file is read");
        }

    @AfterEach
    void tearDown()
        {
        factory.close();
        }

    private static void assertNotWriteable(Runnable write)
        {
        ConfigException thrown = assertThrows(ConfigException.class, write::run);
        assertEquals(ConfigException.ConfigExceptionReason.NOT_WRITEABLE, thrown.getReason());
        }

    /** every kind of put is refused the same way: with and without a scope, for every view. */
    private void assertReadOnly(Configuration view)
        {
        assertFalse(view.isWriteable());
        assertNotWriteable(()->view.put("mysql.host", "changed"));
        assertNotWriteable(()->view.put("mysql.host", "changed", ConfigScope.RUNTIME));
        assertNotWriteable(()->view.put("mysql.host", "changed", EnumSet.of(ConfigScope.RUNTIME)));
        assertNotWriteable(()->view.put("mysql.port", 1));
        assertNotWriteable(()->view.put("mysql.port", 1, ConfigScope.RUNTIME));
        assertNotWriteable(()->view.put("db.host", "changed"));
        assertNotWriteable(()->view.put("other", Arrays.asList("a", "b")));
        assertNotWriteable(()->view.put("other", Arrays.asList("a", "b"), ConfigScope.RUNTIME));
        assertEquals("db1", cfg.getString("mysql.host"), "nothing reached the configuration behind the view");
        assertEquals("x", cfg.getString("other"));
        }

    @Test
    void remappedViewIsReadOnly()
        {
        assertReadOnly(ConfigUtil.remapped(cfg, "mysql.", "db."));
        }

    @Test
    void overridingViewIsReadOnly()
        {
        assertReadOnly(ConfigUtil.withOverrides(cfg, Collections.singletonMap("other", "y")));
        }

    @Test
    void propertiesViewIsReadOnly()
        {
        Properties props = new Properties();
        props.setProperty("mysql.host", "elsewhere");
        Configuration view = ConfigUtil.fromProperties(props);
        assertFalse(view.isWriteable());
        assertNotWriteable(()->view.put("mysql.host", "changed"));
        assertNotWriteable(()->view.put("mysql.host", "changed", ConfigScope.RUNTIME));
        assertEquals("elsewhere", view.getString("mysql.host"));
        }

    @Test
    void propertiesViewGivesAPropertyAsSecretValue()
        {
        Properties props = new Properties();
        props.setProperty("db.password", "s3cr3t-value");
        Configuration view = ConfigUtil.fromProperties(props);

        // twice: the hint in the log is given once, the value every time
        for (int i = 0; i < 2; i++)
            {
            SecretValue secret = view.getSecret("db.password");
            assertNotNull(secret);
            assertArrayEquals("s3cr3t-value".getBytes(StandardCharsets.UTF_8), secret.getValue());
            assertFalse(secret.toString().contains("s3cr3t-value"));
            }
        assertNull(view.getSecret("missing"));
        }

    @Test
    void propertiesViewWithOnlyDefaultsIsNotEmpty()
        {
        Properties defaults = new Properties();
        defaults.setProperty("db.host", "localhost");
        Configuration view = ConfigUtil.fromProperties(new Properties(defaults));

        assertEquals("localhost", view.getString("db.host"));
        assertFalse(view.isEmpty(), "isEmpty() must agree with what the view gives");
        assertTrue(ConfigUtil.fromProperties(new Properties()).isEmpty());
        }

    @Test
    void remappedViewReadsTypedValuesAndSecrets()
        {
        Configuration view = ConfigUtil.remapped(cfg, "mysql.", "db.");

        assertEquals("db1", view.getString("db.host"));
        assertEquals(3306, view.getInteger("db.port"));
        assertEquals("x", view.getString("other"));

        SecretValue secret = view.getSecret("db.password");
        assertNotNull(secret);
        assertArrayEquals("s3cr3t-value".getBytes(StandardCharsets.UTF_8), secret.getValue());
        assertFalse(secret.toString().contains("s3cr3t-value"));
        }

    @Test
    void remappedViewListsKeysAsItAcceptsThem()
        {
        Configuration view = ConfigUtil.remapped(cfg, "mysql.", "db.");

        Set<String> keys = view.getAllConfigurationKeysFlattened(ConfigUtil.ALL_SCOPES);
        assertTrue(keys.contains("db.host"), keys.toString());
        assertTrue(keys.contains("other"), keys.toString());
        assertFalse(keys.contains("mysql.host"), keys.toString());

        assertTrue(view.getAllConfigurationKeysWithSchemasFlattened(ConfigUtil.ALL_SCOPES).containsKey("db.port"));

        Set<String> walked = new HashSet<>();
        view.getEntryKeyTreeIterator().forEachRemaining(walked::add);
        assertTrue(walked.contains("db.host"), walked.toString());
        assertFalse(walked.contains("mysql.host"), walked.toString());
        }

    @Test
    void remappedViewShowsStateAndOriginOfItsSource()
        {
        Configuration view = ConfigUtil.remapped(cfg, "mysql.", "db.");

        assertEquals(cfg.getSourceLocations(), view.getSourceLocations());
        assertFalse(view.getSourceLocations().isEmpty());
        assertEquals(cfg.getConfigName(), view.getConfigName());
        assertEquals(cfg.isClosed(), view.isClosed());

        ConfigEntry entry = view.getConfigEntryFromFullKey("db.host", ConfigUtil.ALL_SCOPES);
        assertNotNull(entry);
        assertEquals(ConfigScope.RUNTIME, entry.getScope());
        assertNotNull(entry.getLocation());

        factory.close();
        assertEquals(cfg.isClosed(), view.isClosed());
        }

    @Test
    void overridesAreConvertedAndCheckedAgainstTheSchema()
        {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("mysql.host", "db2");
        overrides.put("mysql.port", 9090);
        overrides.put("mysql.mode", "safe");
        Configuration view = ConfigUtil.withOverrides(cfg, overrides);

        assertEquals("db2", view.getString("mysql.host"));
        assertEquals(9090, view.getInteger("mysql.port"));
        assertEquals("safe", view.getString("mysql.mode"));
        assertEquals("x", view.getString("other"), "not overridden: from the configuration");
        assertArrayEquals("s3cr3t-value".getBytes(StandardCharsets.UTF_8), view.getSecret("mysql.password").getValue());
        }

    @Test
    void overrideThatCannotBeConvertedIsAnError()
        {
        Configuration view = ConfigUtil.withOverrides(cfg, Collections.singletonMap("other", "not-a-number"));

        ConfigException thrown = assertThrows(ConfigException.class, ()->view.getInteger("other"));
        assertEquals(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE, thrown.getReason());
        assertFalse(thrown.getMessage().contains("not-a-number"), "the value stays out of the message");
        }

    @Test
    void overrideThatDoesNotMatchTheSchemaIsAnError()
        {
        Configuration view = ConfigUtil.withOverrides(cfg, Collections.singletonMap("mysql.mode", "reckless"));

        ConfigException thrown = assertThrows(ConfigException.class, ()->view.getString("mysql.mode"));
        assertEquals(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID, thrown.getReason());
        assertFalse(thrown.getMessage().contains("reckless"), "the value stays out of the message");
        }

    @Test
    void overrideEntryHasNoScopeAndNoLocation()
        {
        Configuration view = ConfigUtil.withOverrides(cfg, Collections.singletonMap("mysql.host", "db2"));

        ConfigEntry overridden = view.getConfigEntryFromFullKey("mysql.host", ConfigUtil.ALL_SCOPES);
        assertNotNull(overridden);
        assertNull(overridden.getScope());
        assertNull(overridden.getLocation());

        ConfigEntry fromSource = view.getConfigEntryFromFullKey("other", ConfigUtil.ALL_SCOPES);
        assertEquals(ConfigScope.RUNTIME, fromSource.getScope());

        assertTrue(view.getAllConfigurationKeysWithSchemasFlattened(ConfigUtil.ALL_SCOPES).containsKey("mysql.host"));
        }

    @Test
    void overrideOnASecretKeyIsASecretEntry()
        {
        Configuration view = ConfigUtil.withOverrides(cfg, Collections.singletonMap("mysql.password", "overridden-pw"));

        ConfigEntry entry = view.getConfigEntryFromFullKey("mysql.password", ConfigUtil.ALL_SCOPES);
        assertNotNull(entry);
        assertTrue(entry.isSecret(), "the schema marks the key SECRET; the override must not lose that");
        assertFalse(entry.toString().contains("overridden-pw"), entry.toString());
        assertArrayEquals("overridden-pw".getBytes(StandardCharsets.UTF_8), view.getSecret("mysql.password").getValue());

        assertFalse(view.getConfigEntryFromFullKey("mysql.host", ConfigUtil.ALL_SCOPES).isSecret(), "only SECRET keys");
        }

    @Test
    void listAndByteArrayOverridesKeepTheirForm()
        {
        byte[] key = {0, 1, (byte) 0xFE, (byte) 0xFF};
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("other", Arrays.asList("a", "b, c"));
        overrides.put("blob", key);
        Configuration view = ConfigUtil.withOverrides(cfg, overrides);

        assertEquals(Arrays.asList("a", "b, c"), view.getListOfStrings("other"));
        assertArrayEquals(key, view.getBytes("blob"));
        assertArrayEquals(key, view.getSecret("blob").getValue(), "binary survives the secret path, too");

        view.getBytes("blob")[0] = 42;
        key[1] = 42;
        assertArrayEquals(new byte[]{0, 1, (byte) 0xFE, (byte) 0xFF}, view.getBytes("blob"), "neither reader nor caller can change the override");
        }
}
//___EOF___
