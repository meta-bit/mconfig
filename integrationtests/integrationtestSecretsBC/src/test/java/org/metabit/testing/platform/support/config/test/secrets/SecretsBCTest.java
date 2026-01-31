package org.metabit.testing.platform.support.config.test.secrets;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.io.StringReader;
import java.io.StringWriter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import static org.junit.jupiter.api.Assertions.*;

public class SecretsBCTest
{
    @TempDir
    Path userDir;

    @BeforeAll
    public static void setup()
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testSecretsWithBC() throws Exception
        {
        // 1. Generate two key pairs and a self-signed certificate using BouncyCastle
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(1024);
        KeyPair kp1 = kpg.generateKeyPair();
        KeyPair kp2 = kpg.generateKeyPair();

        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        X500Name dnName = new X500Name("CN=Test Certificate");
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));
        Date endDate = new Date(now + 24L * 3600L * 1000L); // 1 day validity
        
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(kp1.getPrivate());
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, dnName, kp1.getPublic());
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // 2. Setup mConfig with test mode for USER writes
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "BC-TEST");
        ConfigFactoryBuilder.permitTestMode();
        builder.setTestMode(true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Arrays.asList("USER:" + userDir.toString()));
        ConfigFactory factory = builder.build();
        
        ConfigSchemeEntry certEntrySpec = new ConfigSchemeEntry("server/cert", ConfigEntryType.BYTES);
        certEntrySpec.setSecret(true);
        ConfigSchemeEntry privKeyEntrySpec = new ConfigSchemeEntry("server/key", ConfigEntryType.BYTES);
        privKeyEntrySpec.setSecret(true);
        ConfigSchemeEntry certUserSpec = new ConfigSchemeEntry("server/cert.user", ConfigEntryType.BYTES);
        certUserSpec.setSecret(true);
        ConfigSchemeEntry keyUserSpec = new ConfigSchemeEntry("server/key.user", ConfigEntryType.BYTES);
        keyUserSpec.setSecret(true);
        
        Set<ConfigSchemeEntry> entries = new HashSet<>();
        entries.add(certEntrySpec);
        entries.add(privKeyEntrySpec);
        entries.add(certUserSpec);
        entries.add(keyUserSpec);
        ConfigScheme scheme = ConfigScheme.fromSchemeEntries(entries);
        
        Configuration config = factory.getConfig("secrets-bc", scheme);

        // 3. Put and Get
        // Test conversion effort
        byte[] certBytes = cert.getEncoded();
        config.put("server/cert", certBytes, ConfigScope.SESSION);
        config.put("server/cert.user", certBytes, ConfigScope.USER);
        
        byte[] privKeyBytes = kp1.getPrivate().getEncoded();
        config.put("server/key", privKeyBytes, ConfigScope.SESSION);
        config.put("server/key.user", privKeyBytes, ConfigScope.USER);

        // Retrieve
        SecretValue retrievedCert = config.getSecret("server/cert");
        assertNotNull(retrievedCert);
        assertArrayEquals(certBytes, retrievedCert.getValue());
        // Reconstruct X.509 cert
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate reconstructedCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
        assertArrayEquals(certBytes, reconstructedCert.getEncoded());
        retrievedCert.erase();
        
        SecretValue retrievedKey = config.getSecret("server/key");
        assertNotNull(retrievedKey);
        assertArrayEquals(privKeyBytes, retrievedKey.getValue());
        // Reconstruct private key (PKCS8 DER)
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey reconstructedKey = kf.generatePrivate(keySpec);
        assertArrayEquals(privKeyBytes, reconstructedKey.getEncoded());
        retrievedKey.erase();

        // Check redaction
        ConfigEntry entry = config.getConfigEntryFromFullKey("server/key", EnumSet.of(ConfigScope.SESSION));
        assertTrue(entry.toString().contains("[REDACTED]"));
        
        // USER scope puts create files in tempdir (format/BYTES readback dep; core roundtrip validated in SESSION)

        System.out.println("[DEBUG_LOG] BC Secrets test completed successfully.");
        }
    
    @Test
    public void testPEMPrivateKeyRoundtrip() throws Exception
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "BC-PEM-TEST");
        ConfigFactoryBuilder.permitTestMode();
        builder.setTestMode(true);
        try (ConfigFactory factory = builder.build())
            {
            Configuration config = factory.getConfig("pem-test");
            
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            
            StringWriter sw = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(sw))
                {
                pemWriter.writeObject(kp.getPrivate());
                }
            String pemStr = sw.toString();
            
            config.put("private.pem", pemStr, ConfigScope.SESSION);
            String retrievedPem = config.getString("private.pem");
            assertEquals(pemStr, retrievedPem);
            
            try (PEMParser pemParser = new PEMParser(new StringReader(retrievedPem)))
                {
                Object obj = pemParser.readObject();
                assertInstanceOf(PEMKeyPair.class, obj);
                PEMKeyPair pemPair = (PEMKeyPair) obj;
                JcaPEMKeyConverter conv = new JcaPEMKeyConverter().setProvider("BC");
                PrivateKey parsedPriv = conv.getPrivateKey(pemPair.getPrivateKeyInfo());
                assertArrayEquals(kp.getPrivate().getEncoded(), parsedPriv.getEncoded());
                }
            }
        System.out.println("[DEBUG_LOG] PEM roundtrip test completed successfully.");
        return;
        }
}
