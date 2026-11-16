package org.metabit.platform.support.config.source.vault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.BootstrapConfigContext;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kubernetes login and activation rules of the Vault storage, against a local fake Vault (no container).
 * The bootstrap settings come from src/test/resources/.config/vaulttest/&lt;scenario&gt;/vault.properties.
 * Those files also set engine, kvVersion and pollInterval: under surefire's module path, the module's own
 * vault.mconfig-schema.json (in target/classes) is not discovered, so its defaults are missing.
 */
class VaultAuthenticationTest
{
    private static final String COMPANY  = "vaulttest";
    private static final String SA_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test-service-account-jwt";
    private static final Path   TOKEN_FILE = Paths.get("target", "vault-test", "sa-token"); // matches auth.kubernetes.tokenPath in the test resources

    private static final String SECRET_JSON = "{\"data\":{\"data\":{\"db/password\":\"s3cr3t\"},\"metadata\":{\"version\":1}}}";

    private ServerSocket         server;
    private Thread               serverThread;
    private final List<String>   requests = new CopyOnWriteArrayList<>();
    private String               address;
    // status codes for the next secret reads, in order; afterwards 200
    private final Deque<Integer> readStatus = new ConcurrentLinkedDeque<>();
    private final AtomicInteger  logins = new AtomicInteger();
    // answer of auth/token/lookup-self: false = the token has expired (403), true = valid (200)
    private volatile boolean     tokenValid = false;

    @BeforeEach
    void startFakeVault() throws IOException
        {
        Files.createDirectories(TOKEN_FILE.getParent());
        Files.write(TOKEN_FILE, SA_TOKEN.getBytes(StandardCharsets.UTF_8));

        // minimal HTTP/1.1 responder; java.base only (the test runs on the module path)
        server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        serverThread = new Thread(()->
            {
            while (!server.isClosed())
                {
                try (Socket socket = server.accept())
                    {
                    handle(socket);
                    }
                catch (IOException e)
                    {
                    // closed, or client gone
                    }
                }
            }, "fake-vault");
        serverThread.setDaemon(true);
        serverThread.start();
        address = "http://127.0.0.1:"+server.getLocalPort();
        }

    private void handle(Socket socket) throws IOException
        {
        InputStream in = new BufferedInputStream(socket.getInputStream());
        String requestLine = readLine(in);
        int contentLength = 0;
        String header;
        while (!(header = readLine(in)).isEmpty())
            {
            if (header.toLowerCase().startsWith("content-length:"))
                {
                contentLength = Integer.parseInt(header.substring(15).trim());
                }
            }
        byte[] body = new byte[contentLength];
        int read = 0;
        while (read < contentLength)
            {
            int n = in.read(body, read, contentLength-read);
            if (n < 0) break;
            read += n;
            }
        String[] parts = requestLine.split(" ");
        requests.add(parts[0]+" "+parts[1]+" "+new String(body, 0, read, StandardCharsets.UTF_8));

        int status = 200;
        byte[] response;
        if (parts[1].endsWith("/login"))
            {
            String clientToken = "client-token-from-login-"+logins.incrementAndGet();
            response = ("{\"renewable\":false,\"auth\":{\"client_token\":\""+clientToken+"\",\"policies\":[],\"lease_duration\":3600,\"renewable\":true}}").getBytes(StandardCharsets.UTF_8);
            }
        else if (parts[1].equals("/v1/auth/token/lookup-self"))
            {
            status = tokenValid ? 200 : 403;
            // shaped like a real Vault answer; the driver parses all of these fields
            String valid = "{\"data\":{\"accessor\":\"acc\",\"creation_time\":1700000000,\"creation_ttl\":3600,\"display_name\":\"kubernetes-test\","
                    +"\"entity_id\":\"\",\"explicit_max_ttl\":0,\"id\":\"client-token\",\"meta\":null,\"num_uses\":0,\"orphan\":true,"
                    +"\"path\":\"auth/kubernetes/login\",\"policies\":[\"default\"],\"renewable\":true,\"ttl\":3600}}";
            response = (tokenValid ? valid : "{\"errors\":[\"permission denied\"]}").getBytes(StandardCharsets.UTF_8);
            }
        else if (parts[1].startsWith("/v1/secret/data/"))
            {
            Integer next = readStatus.poll();
            status = (next == null) ? 200 : next;
            response = (status == 200 ? SECRET_JSON : "{\"errors\":[\"permission denied\"]}").getBytes(StandardCharsets.UTF_8);
            }
        else
            {
            status = 404;
            response = "{\"errors\":[]}".getBytes(StandardCharsets.UTF_8);
            }
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 "+status+" X\r\nContent-Type: application/json\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(response);
        out.flush();
        }

    private static String readLine(InputStream in) throws IOException
        {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) >= 0 && c != '\n')
            {
            if (c != '\r') sb.append((char) c);
            }
        return sb.toString();
        }

    @AfterEach
    void stopFakeVault() throws IOException
        {
        server.close();
        Files.deleteIfExists(TOKEN_FILE);
        }

    @Test
    void inactiveWithoutAddress()
        {
        VaultConfigStorage storage = initStorage("noaddress", null);
        assertFalse(storage.isInitialized(), "no address configured: the storage must stay inactive "
                +"(a vault.properties under .config/metabit/mConfig/ must not be read; bootstrap configs are per company/app)");
        assertTrue(requests.isEmpty());
        }

    @Test
    void noKubernetesLoginOverPlainHttpByDefault()
        {
        VaultConfigStorage storage = initStorage("k8s", address);
        assertTrue(storage.isInitialized());
        assertTrue(requests.stream().noneMatch(r->r.contains("/login")), "the ServiceAccount token must not be sent over http: "+requests);
        }

    @Test
    void kubernetesLoginWhenInsecureTransportAllowed()
        {
        VaultConfigStorage storage = initStorage("k8sinsecure", address);
        assertTrue(storage.isInitialized());
        assertEquals(1, requests.size(), requests.toString());
        String login = requests.get(0);
        assertTrue(login.startsWith("POST /v1/auth/kubernetes/login "), login);
        assertTrue(login.contains("\"role\":\"k8sinsecure\""), "role defaults to the application name: "+login);
        assertTrue(login.contains(SA_TOKEN), "the token is read from auth.kubernetes.tokenPath: "+login);
        assertEquals("client-token-from-login-1", storage.getClientToken());
        }

    @Test
    void customMountAndRole()
        {
        initStorage("k8scustom", address);
        assertEquals(1, requests.size(), requests.toString());
        String login = requests.get(0);
        assertTrue(login.startsWith("POST /v1/auth/custom-k8s/login "), login);
        assertTrue(login.contains("\"role\":\"myrole\""), login);
        }

    @Test
    void readsKvVersion2WithoutDoubleDataSegment()
        {
        VaultConfigStorage storage = initStorage("k8sinsecure", address);
        Map<String, String> values = read(storage, "myapp");
        assertEquals("s3cr3t", values.get("db/password"), requests.toString());
        assertTrue(requests.stream().anyMatch(r->r.startsWith("GET /v1/secret/data/myapp ")), requests.toString());
        }

    @Test
    void reLoginOnceAfterForbiddenRead()
        {
        VaultConfigStorage storage = initStorage("k8sinsecure", address);
        assertEquals(1, logins.get());
        readStatus.add(403);
        Map<String, String> values = read(storage, "myapp");
        assertEquals("s3cr3t", values.get("db/password"), "the retry after the fresh login delivers the value: "+requests);
        assertEquals(2, logins.get(), "exactly one fresh login: "+requests);
        assertEquals("client-token-from-login-2", storage.getClientToken());
        }

    @Test
    void forbiddenAfterReLoginIsReportedNotRetriedEndlessly()
        {
        VaultConfigStorage storage = initStorage("k8sinsecure", address);
        readStatus.add(403);
        readStatus.add(403);
        readStatus.add(403);
        assertTrue(read(storage, "myapp").isEmpty());
        assertEquals(2, logins.get(), "one fresh login, one retry: "+requests);
        assertEquals(1, readStatus.size(), "two reads (original and retry) only: "+requests);
        }

    @Test
    void forbiddenWithValidTokenIsPolicyDenialWithoutLogin()
        {
        VaultConfigStorage storage = initStorage("k8sinsecure", address);
        assertEquals(1, logins.get());
        tokenValid = true;
        readStatus.add(403);
        readStatus.add(403);
        assertTrue(read(storage, "myapp").isEmpty(), requests.toString());
        assertTrue(read(storage, "myapp").isEmpty(), requests.toString());
        assertEquals(1, logins.get(), "a policy denial must not trigger new logins: "+requests);
        assertTrue(requests.stream().anyMatch(r->r.startsWith("GET /v1/auth/token/lookup-self ")), requests.toString());
        }

    @Test
    void explicitTokenWinsOverKubernetesLogin()
        {
        // the ServiceAccount token file exists and Kubernetes login over http is allowed, but a token is configured
        VaultConfigStorage storage = initStorage("tokenandk8s", address);
        assertTrue(storage.isInitialized());
        assertEquals(0, logins.get(), "an explicitly configured token is used as given: "+requests);
        assertEquals("static-test-token", storage.getClientToken());
        readStatus.add(403);
        assertTrue(read(storage, "myapp").isEmpty());
        assertEquals(0, logins.get(), "a refused read must not switch to the pod's Kubernetes role: "+requests);
        }

    @Test
    void staticTokenIsNotReLoggedIn()
        {
        VaultConfigStorage storage = initStorage("statictoken", address);
        assertTrue(storage.isInitialized());
        readStatus.add(403);
        assertTrue(read(storage, "myapp").isEmpty());
        assertEquals(0, logins.get(), "a static token cannot log in again: "+requests);
        }

    @Test
    void builderAddedSourceIsInitialisedAndRead()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY, "k8sinsecure");
        builder.setTestMode(true);
        builder.setFeature(VaultFeatures.BYPASS_TEST, true); // intentionally against the fake Vault
        builder.addSource(VaultConfigSource.builder().withAddress(address).withVaultPath("myapp").build());
        try (ConfigFactory factory = builder.build())
            {
            Configuration config = factory.getConfig("myapp");
            SecretValue secret = config.getSecret("db/password");
            assertNotNull(secret, "the builder's own Vault storage must be initialized and read: "+requests);
            assertEquals("s3cr3t", new String(secret.getChars()));
            assertEquals(1, logins.get(), requests.toString());
            }
        }

    @Test
    void builderAddedSourceIsNotUsedInTestModeWithoutBypass()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY, "k8sinsecure");
        builder.setTestMode(true);
        builder.addSource(VaultConfigSource.builder().withAddress(address).withVaultPath("myapp").build());
        try (ConfigFactory factory = builder.build())
            {
            factory.getConfig("myapp");
            assertEquals(0, logins.get(), "test mode without BYPASS_TEST must not reach Vault: "+requests);
            assertTrue(requests.isEmpty(), requests.toString());
            }
        }

    @Test
    void refusedReadIsReportedAsSourceUnavailable()
        {
        readStatus.add(403);
        readStatus.add(403);
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY, "k8sinsecure");
        builder.setTestMode(true);
        builder.setFeature(VaultFeatures.BYPASS_TEST, true);
        builder.addSource(VaultConfigSource.builder().withAddress(address).withVaultPath("myapp").build());
        try (ConfigFactory factory = builder.build())
            {
            factory.getConfig("myapp");
            ConfigEvent event = factory.getEvents().stream()
                    .filter(e->e.getKind() == ConfigEvent.Kind.SOURCE_UNAVAILABLE)
                    .findFirst().orElse(null);
            assertNotNull(event, "a refused read must not fall back silently");
            assertEquals("VAULT_ACCESS_DENIED", event.getDetailCode());
            assertEquals(ConfigEvent.Severity.WARNING, event.getSeverity());
            assertNotNull(event.getLocation(), "the event names the location");
            }
        }

    private Map<String, String> read(VaultConfigStorage storage, String vaultPath)
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY, "readprobe");
        builder.setTestMode(true);
        try (ConfigFactory factory = builder.build())
            {
            Map<String, String> result = new HashMap<>();
            LayeredConfigurationInterface layered = (LayeredConfigurationInterface) factory.getConfig("readprobe");
            storage.updateConfigurationLayers("readprobe", new VaultConfigSource(ConfigScope.CLOUD, storage, null, vaultPath), layered);
            ConfigEntry entry = layered.getConfigEntryFromFullKey("db/password", EnumSet.of(ConfigScope.CLOUD));
            if (entry != null)
                {
                try
                    {
                    result.put("db/password", entry.getValueAsString());
                    }
                catch (ConfigCheckedException e)
                    {
                    fail(e);
                    }
                }
            return result;
            }
        }

    @Test
    void transportCheck()
        {
        assertTrue(VaultConfigStorage.isSecureTransport("https://vault.example.com:8200"));
        assertTrue(VaultConfigStorage.isSecureTransport(" HTTPS://vault"));
        assertFalse(VaultConfigStorage.isSecureTransport("http://127.0.0.1:8200"));
        assertFalse(VaultConfigStorage.isSecureTransport("vault.example.com"));
        assertFalse(VaultConfigStorage.isSecureTransport(null));
        }

    @Test
    void driverEnvironmentAllowlist()
        {
        for (String name : new String[]{"VAULT_TOKEN", "VAULT_SSL_CERT", "VAULT_OPEN_TIMEOUT", "VAULT_READ_TIMEOUT"})
            {
            assertTrue(VaultConfigStorage.AllowlistEnvironmentLoader.isAllowed(name, false), name);
            }
        assertFalse(VaultConfigStorage.AllowlistEnvironmentLoader.isAllowed("VAULT_SSL_VERIFY", false), "may disable TLS verification");
        assertTrue(VaultConfigStorage.AllowlistEnvironmentLoader.isAllowed("VAULT_SSL_VERIFY", true));
        assertFalse(VaultConfigStorage.AllowlistEnvironmentLoader.isAllowed("VAULT_ADDR", false), "the address comes from mConfig");
        assertFalse(VaultConfigStorage.AllowlistEnvironmentLoader.isAllowed("VAULT_NAMESPACE", true));
        }

    private VaultConfigStorage initStorage(String application, String addressOverride)
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY, application);
        builder.setTestMode(true);
        ConfigFactory factory = builder.build();

        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setString(ConfigFeature.APPLICATION_NAME, application);
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        ctx.setLogger(new org.metabit.platform.support.config.impl.logging.ConsoleLogging("vault-test"));

        VaultConfigStorage storage = new VaultConfigStorage();
        storage.init(ctx);
        if (addressOverride != null)
            {
            storage.setAddressOverride(addressOverride);
            }
        storage.postInit(new BootstrapConfigContext()
            {
            @Override
            public Configuration getRestrictedConfig(String configName, EnumSet<ConfigScope> scopes)
                {
                return factory.getConfig(configName);
                }

            @Override
            public ConfigLoggingInterface getLogger()
                {
                return ctx.getLogger();
                }
            });
        factory.close();
        return storage;
        }
}
//___EOF___
