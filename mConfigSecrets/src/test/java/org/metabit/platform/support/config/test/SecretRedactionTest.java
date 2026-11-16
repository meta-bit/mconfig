package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.SecretRedaction;
import org.metabit.platform.support.config.impl.entry.SimpleKeyConfigEntryWrapper;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.impl.secrets.SecretsStorage;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MLIB-130: the central decision which values must not be exported or shown.
 */
class SecretRedactionTest
{
    @Test
    void secretsFromSourcesAreConfidential()
        {
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.APPLICATION, new SecretsStorage(), null, null);
        SecretsIntegrationTest.MockSecretsProvider provider = new SecretsIntegrationTest.MockSecretsProvider();
        provider.addSecret("db/password", "s3cr3t", SecretType.PLAIN_TEXT);
        SecretValue value = provider.getSecret("db/password", null);
        ConfigEntry secret = new SecretConfigEntryLeaf("db/password", value, location);

        assertTrue(SecretRedaction.isConfidential(secret));
        // wrappers used by the cursor must keep the flag
        assertTrue(SecretRedaction.isConfidential(new SimpleKeyConfigEntryWrapper(secret, "password")));
        assertFalse(secret.toString().contains("s3cr3t"));
        assertTrue(secret.toString().contains(SecretRedaction.REDACTED));
        }

    @Test
    void plainEntriesAreNotConfidential()
        {
        ConfigEntry plain = new GenericConfigEntryLeaf("db/host", "dbserver", ConfigEntryType.STRING, new ConfigEntryMetadata((ConfigSource) null));
        assertFalse(SecretRedaction.isConfidential(plain));
        assertFalse(SecretRedaction.isConfidential(null));
        }
}
//___EOF___
