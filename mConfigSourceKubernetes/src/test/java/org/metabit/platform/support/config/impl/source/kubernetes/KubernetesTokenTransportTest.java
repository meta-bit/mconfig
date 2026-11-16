package org.metabit.platform.support.config.impl.source.kubernetes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The ServiceAccount token is sent to the API endpoint only over https, unless explicitly allowed.
 * Uses a local fake API server (no cluster, no container).
 */
class KubernetesTokenTransportTest
{
    private static final String SA_TOKEN = "test-service-account-token";

    private ServerSocket       server;
    private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();
    private Path               tokenFile;
    private KubernetesConfigStorage storage;

    @BeforeEach
    void setUp() throws IOException
        {
        tokenFile = Files.createTempFile("k8s-sa-token", "");
        Files.write(tokenFile, SA_TOKEN.getBytes(StandardCharsets.UTF_8));

        server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        Thread t = new Thread(()->
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
            }, "fake-k8s-api");
        t.setDaemon(true);
        t.start();
        }

    @AfterEach
    void tearDown() throws IOException
        {
        if (storage != null) storage.exit();
        server.close();
        Files.deleteIfExists(tokenFile);
        }

    @Test
    void tokenNotSentOverPlainHttp()
        {
        readConfigMap(false);
        assertEquals(1, authorizationHeaders.size(), "one request expected");
        assertEquals("", authorizationHeaders.get(0), "no Authorization header over http");
        }

    @Test
    void tokenSentOverHttpWhenExplicitlyAllowed()
        {
        readConfigMap(true);
        assertEquals(1, authorizationHeaders.size(), "one request expected");
        assertEquals("Bearer "+SA_TOKEN, authorizationHeaders.get(0));
        }

    @Test
    void transportCheck()
        {
        assertTrue(KubernetesConfigStorage.isSecureTransport("https://kubernetes.default.svc"));
        assertFalse(KubernetesConfigStorage.isSecureTransport("http://127.0.0.1:8080"));
        assertFalse(KubernetesConfigStorage.isSecureTransport("https-proxy.local"));
        assertFalse(KubernetesConfigStorage.isSecureTransport(null));
        }

    private void readConfigMap(boolean allowInsecure)
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setString(KubernetesFeatures.API_ENDPOINT, "http://127.0.0.1:"+server.getLocalPort());
        settings.setString(KubernetesFeatures.SA_TOKEN_PATH, tokenFile.toString());
        settings.setBoolean(KubernetesFeatures.ALLOW_INSECURE_TRANSPORT, allowInsecure);
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        ctx.setLogger(new ConsoleLogging("k8s-test"));

        storage = new KubernetesConfigStorage();
        storage.init(ctx);
        storage.postInit(null);

        ConfigLocation location = mock(ConfigLocation.class);
        when(location.getStorage()).thenReturn(storage);
        when(location.getStorageInstanceHandle()).thenReturn("configmap/app-config");
        storage.updateConfigurationLayers("app", location, mock(LayeredConfigurationInterface.class));
        }

    private void handle(Socket socket) throws IOException
        {
        InputStream in = new BufferedInputStream(socket.getInputStream());
        readLine(in); // request line
        String authorization = "";
        String header;
        while (!(header = readLine(in)).isEmpty())
            {
            if (header.toLowerCase().startsWith("authorization:"))
                {
                authorization = header.substring(14).trim();
                }
            }
        authorizationHeaders.add(authorization);
        byte[] response = "{}".getBytes(StandardCharsets.UTF_8); // empty object: nothing to process
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 404 Not Found\r\nContent-Type: application/json\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
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
}
//___EOF___
