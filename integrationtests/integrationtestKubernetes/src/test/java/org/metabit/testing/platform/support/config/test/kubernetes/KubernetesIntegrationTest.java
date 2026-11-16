package org.metabit.testing.platform.support.config.test.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.source.kubernetes.fabric8.KubernetesFabric8ConfigStorage;
import org.metabit.platform.support.config.impl.source.kubernetes.fabric8.KubernetesFabric8Features;

import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

@EnableKubernetesMockClient(crud = true)
public class KubernetesIntegrationTest
{
    private static KubernetesClient client;
    private static KubernetesMockServer mockServer;

    private static Path tempDir;
    private static Path configBase;
    private static Path secretsBase;

    @BeforeAll
    static void setup()
            throws IOException
        {
        tempDir = Files.createTempDirectory("k8s-test-");
        // For testing standard path discovery, we can point directly to the directory containing files
        configBase = Files.createDirectories(tempDir.resolve("config"));
        secretsBase = Files.createDirectories(tempDir.resolve("secrets"));

        Files.write(configBase.resolve("app.properties"), "key1=value1\nkey2=value2".getBytes(StandardCharsets.UTF_8));
        Files.write(secretsBase.resolve("db.password.properties"), "db.password=secret-pass".getBytes(StandardCharsets.UTF_8));

        System.out.println("[DEBUG_LOG] Temp dir: "+tempDir.toAbsolutePath());
        System.out.println("[DEBUG_LOG] Config base: "+configBase.toAbsolutePath());

        // Prepare mock Kubernetes data
        client.configMaps().inNamespace("default").resource(new ConfigMapBuilder()
                .withNewMetadata().withName("app-cm").endMetadata()
                .addToData("api.key", "api-value")
                .build()).create();

        System.out.println("[DEBUG_LOG] Created ConfigMap in mock client.");
        ConfigMap verifyCm = client.configMaps().inNamespace("default").withName("app-cm").get();
        System.out.println("[DEBUG_LOG] Verify CM exists in mock client: " + (verifyCm != null));
        if (verifyCm != null) System.out.println("[DEBUG_LOG] CM data: " + verifyCm.getData());

        client.secrets().inNamespace("default").resource(new SecretBuilder()
                .withNewMetadata().withName("app-secret").endMetadata()
                .addToData("db.password", Base64.getEncoder().encodeToString("secret-api-pass".getBytes(StandardCharsets.UTF_8)))
                .build()).create();
        }

    @AfterAll
    static void tearDown()
            throws IOException
        {
        // Simple recursive delete
        if (tempDir != null && Files.exists(tempDir))
            {
            Files.walk(tempDir)
                    .sorted((a, b)->b.compareTo(a))
                    .forEach(p->
                        {
                        try
                            {
                            Files.delete(p);
                            }
                        catch (IOException e)
                            {
                            // ignore
                            }
                        });
            }
        }

    @Test
    void testStandardPathDiscovery()
            throws IOException
        {
        System.out.println("[DEBUG_LOG] STARTING testStandardPathDiscovery");
        
        System.out.println("[DEBUG_LOG] Checking ServiceLoader for ConfigStorageInterface:");
        ServiceLoader<ConfigStorageInterface> loader = ServiceLoader.load(ConfigStorageInterface.class);
        for (ConfigStorageInterface storage : loader)
            {
            System.out.println("[DEBUG_LOG] Found storage: " + storage.getStorageID() + " (" + storage.getClass().getName() + ")");
            }

        System.setProperty("mconfig.runtime.kubernetes.config.dir", configBase.toAbsolutePath().toString());
        System.setProperty("mconfig.runtime.kubernetes.secrets.dir", secretsBase.toAbsolutePath().toString());
        System.out.println("[DEBUG_LOG] Set properties: " + System.getProperty("mconfig.runtime.kubernetes.config.dir"));

        try
            {
            // NEW: Build the factory ONLY after system properties are set
            ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
                    .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
                    .setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true)
                    .setFeature(KubernetesFabric8Features.BYPASS_TEST, true)
                    .build();
            System.out.println("[DEBUG_LOG] Factory built.");

            List<ConfigLocation> searchList = factory.getSearchList();

            // Verify that the custom paths were added to the search list
            System.out.println("[DEBUG_LOG] Search list size: "+searchList.size());
            for (ConfigLocation loc : searchList)
                {
                System.out.println("[DEBUG_LOG] Search list entry: scope="+loc.getScope()+", storage="+loc.getStorage().getStorageID()+", handle="+loc.getStorageInstanceHandle());
                }

            String expectedPath = configBase.toAbsolutePath().normalize().toString();
            boolean foundConfig = false;
            for (ConfigLocation loc : searchList)
                {
                if (loc.getStorageInstanceHandle() != null)
                    {
                    String actualPath = Paths.get(loc.getStorageInstanceHandle().toString()).toAbsolutePath().normalize().toString();
                    if (actualPath.equals(expectedPath))
                        {
                        foundConfig = true;
                        break;
                        }
                    }
                }

            assertTrue(foundConfig, "Config directory not found in search list");

            Configuration config = factory.getConfig("app");
            assertEquals("value1", config.getString("key1"));
            assertEquals("value2", config.getString("key2"));

            Configuration secrets = factory.getConfig("db.password");
            assertEquals("secret-pass", secrets.getString("db.password"));
            }
        finally
            {
            System.clearProperty("mconfig.runtime.kubernetes.config.dir");
            System.clearProperty("mconfig.runtime.kubernetes.secrets.dir");
            }
        }

    @Test
    void testKubernetesApiSource()
        {
        System.out.println("[DEBUG_LOG] STARTING testKubernetesApiSource");
        // The storage configures its client from the module settings only (no Fabric8 auto-configuration),
        // so the mock server is set as the API endpoint; it has no CA certificate file, hence the insecure opt-in.
        ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
                .setFeature(KubernetesFabric8Features.BYPASS_TEST, true)
                .setFeature(KubernetesFabric8Features.API_ENDPOINT, client.getConfiguration().getMasterUrl())
                .setFeature(KubernetesFabric8Features.ALLOW_INSECURE_TRANSPORT, true)
                .build();

        Configuration config = factory.getConfig("api-source");
        // In a real scenario, the schema would define the source.
        // For testing, we can manually trigger updateConfigurationLayers if we have a handle to the storage.

        KubernetesFabric8ConfigStorage storage = new KubernetesFabric8ConfigStorage();
        // Access context via casting if possible, or use reflection for internal test access if needed.
        // However, we can also just use a Mock context if we wanted, but let's try to get it from factory.
        
        // Use reflection or internal knowledge to get the context for the test
        org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext context = null;
        try {
            java.lang.reflect.Field field = org.metabit.platform.support.config.impl.DefaultConfigFactory.class.getDeclaredField("ctx");
            field.setAccessible(true);
            context = (org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext) field.get(factory);
        } catch (Exception e) {
            fail("Could not access internal context: " + e.getMessage());
        }

        storage.init(context);
        storage.postInit((org.metabit.platform.support.config.interfaces.BootstrapConfigContext) factory);

        org.metabit.platform.support.config.impl.LayeredConfiguration layered = new org.metabit.platform.support.config.impl.LayeredConfiguration("api-source", null, context, (ConfigFactory) factory);
        ConfigLocation loc = new ConfigLocationImpl(ConfigScope.CLUSTER, storage, (org.metabit.platform.support.config.interfaces.ConfigFormatInterface) null, "default/configmap/app-cm");

        storage.updateConfigurationLayers("api-source", loc, layered);

        assertEquals("api-value", layered.getString("api.key"));
        }
}
