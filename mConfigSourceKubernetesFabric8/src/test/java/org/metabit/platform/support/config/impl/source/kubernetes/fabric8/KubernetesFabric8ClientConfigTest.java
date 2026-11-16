package org.metabit.platform.support.config.impl.source.kubernetes.fabric8;

import io.fabric8.kubernetes.client.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.logging.NullLogging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * the Fabric8 client is configured from this module's settings, not from Fabric8 auto-configuration (MLIB-138).
 * Hermetic: the configuration is inspected, no request is sent.
 */
class KubernetesFabric8ClientConfigTest
{
    @TempDir
    Path dir;

    private KubernetesFabric8ConfigStorage storageWith(String endpoint, boolean allowInsecure) throws IOException
        {
        Files.write(dir.resolve("token"), "sa-token-value\n".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("namespace"), "team-a\n".getBytes(StandardCharsets.UTF_8));
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setString(KubernetesFabric8Features.API_ENDPOINT, endpoint);
        settings.setString(KubernetesFabric8Features.SA_TOKEN_PATH, dir.resolve("token").toString());
        settings.setString(KubernetesFabric8Features.SA_NAMESPACE_PATH, dir.resolve("namespace").toString());
        settings.setString(KubernetesFabric8Features.SA_CA_CERT_PATH, dir.resolve("no-ca.crt").toString());
        settings.setString(KubernetesFabric8Features.CONFIG_DIR_PATHS, dir.resolve("no-config").toString());
        settings.setString(KubernetesFabric8Features.SECRETS_DIR_PATHS, dir.resolve("no-secrets").toString());
        settings.setBoolean(KubernetesFabric8Features.ALLOW_INSECURE_TRANSPORT, allowInsecure);
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        ctx.setLogger(NullLogging.getSingletonInstance());
        KubernetesFabric8ConfigStorage storage = new KubernetesFabric8ConfigStorage();
        storage.init(ctx);
        storage.postInit(null);
        return storage;
        }

    @Test
    void endpointNamespaceAndTokenComeFromTheModuleSettings() throws IOException
        {
        KubernetesFabric8ConfigStorage storage = storageWith("https://k8s.example.test:6443", false);
        Config config = storage.getClient().getConfiguration();
        assertTrue(config.getMasterUrl().startsWith("https://k8s.example.test:6443"), config.getMasterUrl());
        assertEquals("team-a", config.getNamespace());
        assertNotNull(config.getOauthTokenProvider());
        assertEquals("sa-token-value", config.getOauthTokenProvider().getToken());
        assertFalse(config.isTrustCerts(), "certificates are checked unless explicitly allowed");
        storage.exit();
        }

    @Test
    void noTokenOverPlainHttp() throws IOException
        {
        KubernetesFabric8ConfigStorage storage = storageWith("http://127.0.0.1:8080", false);
        Config config = storage.getClient().getConfiguration();
        assertNull(config.getOauthTokenProvider(), "the token must not be sent over http");
        assertNull(config.getOauthToken());
        storage.exit();
        }

    @Test
    void insecureTransportOnlyWithOptIn() throws IOException
        {
        KubernetesFabric8ConfigStorage storage = storageWith("http://127.0.0.1:8080", true);
        Config config = storage.getClient().getConfiguration();
        assertNotNull(config.getOauthTokenProvider());
        assertTrue(config.isTrustCerts());
        storage.exit();
        }

    @Test
    void refusedInTestModeUnlessBypassed()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setBoolean(ConfigFeature.TEST_MODE, true);
        KubernetesFabric8ConfigStorage storage = new KubernetesFabric8ConfigStorage();
        assertFalse(storage.test(settings, NullLogging.getSingletonInstance()));
        settings.setBoolean(KubernetesFabric8Features.BYPASS_TEST, true);
        assertTrue(storage.test(settings, NullLogging.getSingletonInstance()));
        }
}
//___EOF___
