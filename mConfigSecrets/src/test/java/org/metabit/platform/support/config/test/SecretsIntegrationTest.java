package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.impl.secrets.SecretsConfigLayer;
import org.metabit.platform.support.config.impl.secrets.SecretsStorage;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.interfaces.WatchHandle;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;
import org.metabit.platform.support.config.scheme.ConfigSchemeFactory;
import org.mockito.Mockito;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class SecretsIntegrationTest
{

    @Test
    void testSecretsIntegration()
            throws ConfigCheckedException
        {
        // Use ConsoleLogging for observability
        ConsoleLogging logger = new ConsoleLogging("SecretsIntegrationTest");
        logger.setLevel("TRACE");

        // 1. Create a mock secrets provider
        MockSecretsProvider mockProvider = new MockSecretsProvider();
        mockProvider.addSecret("db/password", "supersecret", SecretType.PLAIN_TEXT);

        // 2. Test SecretConfigEntryLeaf
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.APPLICATION, new SecretsStorage(), null, null);
        SecretValue secretValue = mockProvider.getSecret("db/password", null);
        SecretConfigEntryLeaf entry = new SecretConfigEntryLeaf("db/password", secretValue, location);

        assertEquals("db/password", entry.getKey());
        assertEquals(ConfigEntryType.STRING, entry.getType());
        assertTrue(entry.isSecret());
        assertEquals("supersecret", entry.getValueAsString());
        assertArrayEquals("supersecret".getBytes(), entry.getValueAsBytes());
        assertTrue(entry.toString().contains("[REDACTED]"));
        assertFalse(entry.toString().contains("supersecret"));

        // 3. Test SecretsConfigLayer
        SecretsConfigLayer layer = new SecretsConfigLayer(mockProvider, ConfigScope.APPLICATION);
        ConfigEntry layerEntry = layer.getEntry("db/password");
        assertNotNull(layerEntry);
        assertTrue(layerEntry instanceof SecretConfigEntryLeaf);
        assertEquals("supersecret", layerEntry.getValueAsString());

        assertNull(layer.getEntry("nonexistent"));

        // 4. Test SecretTypes
        mockProvider.addSecret("keys/private", "privatekeydata", SecretType.PRIVATE_KEY);
        ConfigEntry privateKeyEntry = layer.getEntry("keys/private");
        assertEquals(SecretType.PRIVATE_KEY, ((SecretConfigEntryLeaf) privateKeyEntry).getSecretValue().getType());
        assertEquals("privatekeydata", privateKeyEntry.getValueAsString());
        }

    @Test
    void testSchemeBasedSecret()
            throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "TEST");
        // No additional user directories here, we use SESSION scope which is in-memory and safe.
        ConfigFactory factory = builder.build();

        // Create a scheme manually
        ConfigSchemeFactory schemeFactory = ConfigSchemeFactory.create();
        ConfigScheme scheme = schemeFactory.createScheme();
        ConfigSchemeEntry schemeEntry = schemeFactory.createEntry("api/key", ConfigEntryType.STRING);
        schemeEntry.setSecret(true);
        scheme.addSchemeEntry(schemeEntry);

        Configuration config = factory.getConfig("test", scheme);
        config.put("api/key", "my-api-key", ConfigScope.SESSION);

        ConfigEntry entry = config.getConfigEntryFromFullKey("api/key", EnumSet.allOf(ConfigScope.class));
        assertNotNull(entry);
        assertTrue(entry.isSecret(), "Entry should be recognized as a secret");
        assertEquals("my-api-key", entry.getValueAsString());
        assertTrue(entry.toString().contains("[REDACTED]"), "toString should redact secret value");
        assertFalse(entry.toString().contains("my-api-key"), "toString should NOT contain the secret value");

        SecretValue secret = config.getSecret("api/key");
        assertNotNull(secret);
        assertArrayEquals("my-api-key".getBytes(), secret.getValue());
        }

    @Test
    void testConfigDrivenSecretProvider()
            throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "TEST");

        // Mock a secrets provider that uses config to initialize
        ConfigSecretsProviderInterface provider = Mockito.mock(ConfigSecretsProviderInterface.class);
        Mockito.when(provider.getProviderID()).thenReturn("config-aware-provider");

        SecretValue mockSecretValue = Mockito.mock(SecretValue.class);
        Mockito.when(mockSecretValue.getValue()).thenReturn("data-for-my-api-key".getBytes());
        Mockito.when(mockSecretValue.getType()).thenReturn(SecretType.PLAIN_TEXT);

        Mockito.when(provider.getSecret(Mockito.eq("secret.data"), Mockito.any())).thenReturn(mockSecretValue);

        // Usage check
        assertEquals("data-for-my-api-key", new String(provider.getSecret("secret.data", null).getValue()));
        }

    public static class MockSecretsProvider implements ConfigSecretsProviderInterface
    {
        private Map<String, SecretValue> secrets = new HashMap<>();

        public void addSecret(String path, String value, SecretType type)
            {
            secrets.put(path, new MockSecretValue(value.getBytes(), type));
            }

        @Override
        public String getProviderID() { return "mock"; }

        @Override
        public void init(Map<String, Object> config) { }

        @Override
        public void init(Map<String, Object> config, Configuration contextConfig) { }

        @Override
        public SecretValue getSecret(String path, Map<String, Object> opts) { return secrets.get(path); }

        @Override
        public List<String> listSecrets(String prefix) { return new ArrayList<>(secrets.keySet()); }

        @Override
        public WatchHandle watchSecret(String path, Consumer<SecretValue> callback)
            {
            return ()->
                { };
            }

        @Override
        public void renewAuth() { }
    }

    public static class MockSecretValue implements SecretValue
    {
        private final byte[] value;
        private final SecretType type;

        public MockSecretValue(byte[] value, SecretType type)
            {
            this.value = value;
            this.type = type;
            }

        @Override
        public byte[] getValue() { return value; }

        @Override
        public SecretType getType() { return type; }

        @Override
        public Map<String, String> getMetadata() { return Collections.emptyMap(); }
    }
}
