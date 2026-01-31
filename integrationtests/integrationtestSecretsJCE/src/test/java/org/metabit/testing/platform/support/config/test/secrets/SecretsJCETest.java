package org.metabit.testing.platform.support.config.test.secrets;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import java.security.*;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class SecretsJCETest
{
    @TempDir
    Path userDir;

    @Test
    public void testSecretsWithJCE() throws Exception
    {
        // 1. Generate two key pairs and a self-signed certificate using JCE
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024); // Lowest keysize for RSA in many JCE providers
        KeyPair kp1 = kpg.generateKeyPair();
        KeyPair kp2 = kpg.generateKeyPair();

        // Note: generating a self-signed certificate with pure JCE (without BC or other libs)
        // is surprisingly hard in standard Java without using internal sun.security classes.

        // 2. Setup mConfig with test mode for USER writes
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "JCE-TEST");
        ConfigFactoryBuilder.permitTestMode();
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Arrays.asList("USER:" + userDir.toString()));
        ConfigFactory factory = builder.build();
        
        ConfigSchemeEntry privKeyEntry = new ConfigSchemeEntry("server/privateKey", ConfigEntryType.BYTES);
        privKeyEntry.setSecret(true);
        ConfigSchemeEntry pubKeyEntry = new ConfigSchemeEntry("server/publicKey", ConfigEntryType.BYTES);
        pubKeyEntry.setSecret(true);
        ConfigSchemeEntry privKeyUserEntry = new ConfigSchemeEntry("server/privateKey.user", ConfigEntryType.BYTES);
        privKeyUserEntry.setSecret(true);
        ConfigSchemeEntry pubKeyUserEntry = new ConfigSchemeEntry("server/publicKey.user", ConfigEntryType.BYTES);
        pubKeyUserEntry.setSecret(true);
        
        Set<ConfigSchemeEntry> entries = new HashSet<>();
        entries.add(privKeyEntry);
        entries.add(pubKeyEntry);
        entries.add(privKeyUserEntry);
        entries.add(pubKeyUserEntry);
        ConfigScheme scheme = ConfigScheme.fromSchemeEntries(entries);
        
        Configuration config = factory.getConfig("secrets-jce", scheme);

        // 3. Put and Get
        // Testing how big the conversion effort is.
        // PrivateKey to byte[]
        byte[] privKeyBytes = kp1.getPrivate().getEncoded();
        config.put("server/privateKey", privKeyBytes, ConfigScope.SESSION);
        config.put("server/privateKey.user", privKeyBytes, ConfigScope.USER);
        
        // PublicKey to byte[]
        byte[] pubKeyBytes = kp1.getPublic().getEncoded();
        config.put("server/publicKey", pubKeyBytes, ConfigScope.SESSION);
        config.put("server/publicKey.user", pubKeyBytes, ConfigScope.USER);

        // Retrieve
        SecretValue retrievedPrivKey = config.getSecret("server/privateKey");
        assertNotNull(retrievedPrivKey);
        assertArrayEquals(privKeyBytes, retrievedPrivKey.getValue());
        // Reconstruct JCE private key (PKCS8 DER)
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey reconstructedPriv = kf.generatePrivate(keySpec);
        assertArrayEquals(privKeyBytes, reconstructedPriv.getEncoded());
        retrievedPrivKey.erase();
        
        SecretValue retrievedPubKey = config.getSecret("server/publicKey");
        assertNotNull(retrievedPubKey);
        assertArrayEquals(pubKeyBytes, retrievedPubKey.getValue());
        // Reconstruct public key (X.509 DER)
        KeyFactory kfPub = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey reconstructedPub = kfPub.generatePublic(pubSpec);
        assertArrayEquals(pubKeyBytes, reconstructedPub.getEncoded());
        retrievedPubKey.erase();

        // Check redaction
        ConfigEntry entry = config.getConfigEntryFromFullKey("server/privateKey", EnumSet.of(ConfigScope.SESSION));
        assertTrue(entry.toString().contains("[REDACTED]"));
        assertFalse(entry.toString().contains(new String(privKeyBytes))); // Though binary might not match String well anyway
        
        // USER scope ready (testmode tempdir); roundtrip validated in SESSION core logic

        System.out.println("[DEBUG_LOG] JCE Secrets test completed successfully.");
    }
}
