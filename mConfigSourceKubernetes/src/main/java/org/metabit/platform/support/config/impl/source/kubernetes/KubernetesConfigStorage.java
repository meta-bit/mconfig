package org.metabit.platform.support.config.impl.source.kubernetes;

import org.metabit.library.format.json.JsonStreamParser;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.source.core.InMemoryLayer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Lightweight Kubernetes configuration storage module for mConfig.
 * Uses JDK 11 HttpClient and zero external dependencies.
 */
public class KubernetesConfigStorage implements ConfigStorageInterface
{
    private static final String STORAGE_ID = "kubernetes";
    private static final String NAME       = "Kubernetes API ConfigSource";

    private static final String DEFAULT_API_ENDPOINT = "https://kubernetes.default.svc";
    private static final String API_V1_NAMESPACES    = "/api/v1/namespaces/";
    private static final String CONFIGMAPS           = "/configmaps/";
    private static final String SECRETS              = "/secrets/";
    private static final String WATCH_PARAMS         = "?watch=true&fieldSelector=metadata.name=";

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String AUTH_BEARER_PREFIX   = "Bearer ";

    private static final String DEFAULT_SA_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String DEFAULT_SA_NS_PATH    = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";
    private static final String DEFAULT_SA_CA_PATH    = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

    private static final String DEFAULT_CONFIG_DIR  = "/etc/config";
    private static final String DEFAULT_SECRETS_DIR = "/etc/secrets";

    private          ConfigFactoryInstanceContext context;
    private          ConfigLoggingInterface       logger;
    private          java.net.http.HttpClient     httpClient;
    private final    Map<String, WatchHandle>     watches = new ConcurrentHashMap<>();
    private volatile boolean                      changed = false;

    private          long                       maxKeySize  = 1024L*1024L;
    private          String                     namespace   = "default";
    private          String                     apiEndpoint = DEFAULT_API_ENDPOINT;
    private volatile byte[]                     token;
    private          boolean                    allowInsecureTransport = false;
    private          boolean                    tokenTransportAllowed  = false;
    private          java.nio.file.WatchService tokenWatchService;
    private          Thread                     tokenWatchThread;

    private String       saTokenPath     = DEFAULT_SA_TOKEN_PATH;
    private String       saNamespacePath = DEFAULT_SA_NS_PATH;
    private String       saCaCertPath    = DEFAULT_SA_CA_PATH;
    private List<String> configDirPaths  = new ArrayList<>(Collections.singletonList(DEFAULT_CONFIG_DIR));
    private List<String> secretsDirPaths = new ArrayList<>(Collections.singletonList(DEFAULT_SECRETS_DIR));

    @Override
    public String getStorageName()
        {
        return NAME;
        }

    @Override
    public String getStorageID()
        {
        return STORAGE_ID;
        }

    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        if (settings.getBoolean(KubernetesFeatures.BYPASS_TEST))
            {
            return true;
            }

        // If in test mode, and not bypassed, we disable the API client to prevent accidental network calls.
        if (settings.getBoolean(ConfigFeature.TEST_MODE))
            {
            logger.debug("Kubernetes: TEST_MODE active without BYPASS_TEST, disabling API client.");
            return false;
            }

        String testSaTokenPath = settings.getString(KubernetesFeatures.SA_TOKEN_PATH);

        File saToken = new File(testSaTokenPath);
        boolean exists = saToken.exists();
        if (!exists)
            {
            logger.debug("Kubernetes: ServiceAccount token not found at "+LogSafe.text(testSaTokenPath)+", disabling storage.");
            }
        return exists;
        }

    @Override
    public boolean init(ConfigFactoryInstanceContext context)
        {
        this.context = context;
        this.logger = context.getLogger();

        resolveSettingsFromFactorySettings(context.getSettings());

        // Detect namespace
        File nsFile = new File(saNamespacePath);
        if (nsFile.exists())
            {
            try
                {
                this.namespace = Files.readString(nsFile.toPath()).trim();
                }
            catch (IOException e)
                {
                logger.warn("Kubernetes: Failed to read namespace: "+LogSafe.text(e.getMessage()));
                }
            }

        addStandardPaths();
        return true;
        }

    private void resolveSettingsFromFactorySettings(ConfigFactorySettings settings)
        {
        Object maxKeySizeObj = settings.get(KubernetesFeatures.MAX_KEY_SIZE);
        if (maxKeySizeObj instanceof Number)
            {
            this.maxKeySize = ((Number) maxKeySizeObj).longValue();
            }

        String endpoint = settings.getString(KubernetesFeatures.API_ENDPOINT);
        if (endpoint != null)
            {
            this.apiEndpoint = endpoint;
            }

        this.allowInsecureTransport = settings.getBoolean(KubernetesFeatures.ALLOW_INSECURE_TRANSPORT);

        String configPaths = settings.getString(KubernetesFeatures.CONFIG_DIR_PATHS);
        if (configPaths != null)
            {
            this.configDirPaths = Arrays.asList(configPaths.split(","));
            }

        String secretsPaths = settings.getString(KubernetesFeatures.SECRETS_DIR_PATHS);
        if (secretsPaths != null)
            {
            this.secretsDirPaths = Arrays.asList(secretsPaths.split(","));
            }

        String saTokenPathStr = settings.getString(KubernetesFeatures.SA_TOKEN_PATH);
        if (saTokenPathStr != null) this.saTokenPath = saTokenPathStr;

        String saNamespacePathStr = settings.getString(KubernetesFeatures.SA_NAMESPACE_PATH);
        if (saNamespacePathStr != null) this.saNamespacePath = saNamespacePathStr;

        String saCaCertPathStr = settings.getString(KubernetesFeatures.SA_CA_CERT_PATH);
        if (saCaCertPathStr != null) this.saCaCertPath = saCaCertPathStr;
        }

    private void resolveSettingsFromConfiguration(Configuration config)
        {
        Long maxKeySizeVal = readBootstrapSetting(config, KubernetesFeatures.MAX_KEY_SIZE.name(), config::getLong);
        if (maxKeySizeVal != null) this.maxKeySize = maxKeySizeVal;

        String endpoint = readBootstrapSetting(config, KubernetesFeatures.API_ENDPOINT.name(), config::getString);
        if (endpoint != null) this.apiEndpoint = endpoint;

        Boolean allowInsecure = readBootstrapSetting(config, KubernetesFeatures.ALLOW_INSECURE_TRANSPORT.name(), config::getBoolean);
        if (allowInsecure != null) this.allowInsecureTransport = allowInsecure;

        String configPaths = readBootstrapSetting(config, KubernetesFeatures.CONFIG_DIR_PATHS.name(), config::getString);
        if (configPaths != null) this.configDirPaths = Arrays.asList(configPaths.split(","));

        String secretsPaths = readBootstrapSetting(config, KubernetesFeatures.SECRETS_DIR_PATHS.name(), config::getString);
        if (secretsPaths != null) this.secretsDirPaths = Arrays.asList(secretsPaths.split(","));

        String saTokenPathStr = readBootstrapSetting(config, KubernetesFeatures.SA_TOKEN_PATH.name(), config::getString);
        if (saTokenPathStr != null) this.saTokenPath = saTokenPathStr;

        String saNamespacePathStr = readBootstrapSetting(config, KubernetesFeatures.SA_NAMESPACE_PATH.name(), config::getString);
        if (saNamespacePathStr != null) this.saNamespacePath = saNamespacePathStr;

        String saCaCertPathStr = readBootstrapSetting(config, KubernetesFeatures.SA_CA_CERT_PATH.name(), config::getString);
        if (saCaCertPathStr != null) this.saCaCertPath = saCaCertPathStr;
        }

    /**
     * read a single optional bootstrap setting. A missing entry yields null (also with EXCEPTION_ON_MISSING_ENTRY);
     * an invalid value is logged and ignored, so the previously resolved setting stays in effect.
     */
    private <T> T readBootstrapSetting(Configuration config, String key, Function<String, T> getter)
        {
        try
            {
            return getter.apply(key);
            }
        catch (ConfigException e)
            {
            if (e.getReason() != ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY)
                { logger.warn("Kubernetes: ignoring invalid bootstrap setting "+key+": "+LogSafe.text(e.getMessage())); }
            return null;
            }
        }

    @Override
    public void postInit(BootstrapConfigContext bootstrapCtx)
        {
        if (httpClient != null) return;

        if (bootstrapCtx != null)
            {
            // Unlike Vault, AWS and ZooKeeper, which read only the four local scopes, Kubernetes also reads
            // SESSION and RUNTIME, deliberately: in a pod, the environment is set by the pod spec and the kubelet
            // (deployment configuration, not foreign input), and RUNTIME carries intentional overrides from code.
            // Neither scope is served by a source that depends on this storage, so there is no circular read.
            Configuration k8sConfig = bootstrapCtx.getRestrictedConfig("kubernetes", EnumSet.of(ConfigScope.PRODUCT, ConfigScope.HOST, ConfigScope.USER, ConfigScope.APPLICATION, ConfigScope.SESSION, ConfigScope.RUNTIME));
            resolveSettingsFromConfiguration(k8sConfig);
            }

        // The ServiceAccount token is sent only over https, unless explicitly allowed.
        this.tokenTransportAllowed = isSecureTransport(apiEndpoint) || allowInsecureTransport;
        if (!tokenTransportAllowed)
            {
            logger.warn("Kubernetes: the API endpoint is not https ("+LogSafe.text(apiEndpoint)+"); the ServiceAccount token is not sent. "
                    +"Set "+KubernetesFeatures.ALLOW_INSECURE_TRANSPORT.name()+"=true to allow it (development only).");
            }

        try
            {
            // Load token
            File tokenFile = new File(saTokenPath);
            if (tokenFile.exists())
                {
                this.token = readTokenFile(tokenFile);
                if (this.token != null)
                    {
                    logger.debug("Kubernetes: Loaded token from "+LogSafe.text(saTokenPath));
                    }
                }
            else
                {
                logger.debug("Kubernetes: Token file not found at "+LogSafe.text(saTokenPath));
                }

            HttpClient.Builder builder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1);

            // Setup SSLContext with CA cert from saCaCertPath
            File caCertFile = new File(saCaCertPath);
            if (caCertFile.exists())
                {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate caCert;
                try (InputStream is = Files.newInputStream(caCertFile.toPath()))
                    {
                    caCert = (X509Certificate) cf.generateCertificate(is);
                    }

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", caCert);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                builder.sslContext(sslContext);
                }
            else if (allowInsecureTransport)
                {
                // For local/mock testing without a CA certificate: accept any server certificate.
                // Only with the explicit opt-in; never derived from the endpoint's name.
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] {
                        new X509TrustManager()
                        {
                            public X509Certificate[] getAcceptedIssuers() { return null; }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) { }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                        }
                }, new java.security.SecureRandom());
                builder.sslContext(sslContext);
                }

            this.httpClient = builder.build();
            logger.info("Kubernetes: HttpClient initialized (endpoint: "+LogSafe.text(apiEndpoint)+", namespace: "+LogSafe.text(namespace)+")");

            startTokenWatcher();
            }
        catch (Exception e)
            {
            logger.warn("Kubernetes: Failed to initialize HttpClient: "+LogSafe.text(e.getMessage()));
            }
        }

    private void addAuthorization(HttpRequest.Builder reqBuilder)
        {
        byte[] currentToken = this.token;
        if (currentToken != null && tokenTransportAllowed)
            {
            reqBuilder.header(HEADER_AUTHORIZATION, AUTH_BEARER_PREFIX+new String(currentToken, StandardCharsets.UTF_8));
            }
        }

    static boolean isSecureTransport(String endpoint)
        {
        return endpoint != null && endpoint.trim().toLowerCase(java.util.Locale.ROOT).startsWith("https://");
        }

    private void addPathsToSearchList(List<String> paths, ConfigStorageInterface fileStorage)
        {
        for (String pathStr : paths)
            {
            File configDir = new File(pathStr);
            if (configDir.exists() && configDir.isDirectory())
                {
                logger.info("Kubernetes: Adding "+LogSafe.text(pathStr)+" to search list");
                context.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.CLUSTER, fileStorage, null, Paths.get(pathStr)), ConfigScope.CLUSTER);
                }
            }
        }

    private void addStandardPaths()
        {
        ConfigStorageInterface fileStorage = context.getConfigStorages().get("files");
        if (fileStorage == null) return;

        addPathsToSearchList(configDirPaths, fileStorage);
        addPathsToSearchList(secretsDirPaths, fileStorage);
        }

    @Override
    public void updateConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        if (httpClient == null || !STORAGE_ID.equals(possibleSource.getStorage().getStorageID()))
            {
            return;
            }

        Object handle = possibleSource.getStorageInstanceHandle();
        if (handle == null) return;

        K8sResourceSpec spec = K8sResourceSpec.parse(handle.toString(), this.namespace);
        if (spec == null)
            {
            logger.warn("Kubernetes: Invalid resource spec: "+LogSafe.text(handle));
            return;
            }

        String path;
        if ("configmap".equalsIgnoreCase(spec.kind))
            {
            path = API_V1_NAMESPACES+spec.namespace+CONFIGMAPS+spec.name;
            }
        else if ("secret".equalsIgnoreCase(spec.kind) || "secrets".equalsIgnoreCase(spec.kind))
            {
            path = API_V1_NAMESPACES+spec.namespace+SECRETS+spec.name;
            }
        else
            {
            logger.warn("Kubernetes: Unsupported resource kind: "+LogSafe.text(spec.kind));
            return;
            }

        boolean isSecretResource = "secret".equalsIgnoreCase(spec.kind) || "secrets".equalsIgnoreCase(spec.kind);
        try
            {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint+path))
                    .GET();

            addAuthorization(reqBuilder);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
                {
                K8sResourceData resourceData = parseK8sResponse(response.body(), isSecretResource);
                processResourceData(resourceData, possibleSource, layeredCfg, isSecretResource);
                ensureWatch(spec.namespace, spec.kind, spec.name);
                }
            else if (response.statusCode() != 404)
                {
                EventRecorder.recordSourceUnavailable(context, possibleSource, "KUBERNETES_READ_FAILED",
                        "Kubernetes: Failed to fetch "+LogSafe.text(spec)+" (HTTP "+response.statusCode()+"): "+LogSafe.text(response.body()));
                }
            }
        catch (Exception e)
            {
            // a failure while handling a Secret's response may quote its content; name the type only
            EventRecorder.recordSourceUnavailable(context, possibleSource, "KUBERNETES_READ_FAILED",
                    "Kubernetes: Failed to fetch "+LogSafe.text(spec)+": "
                    +(isSecretResource ? e.getClass().getSimpleName() : LogSafe.text(e.getMessage())));
            }
        }


    /** format IDs of mConfigFormatRawFile; their layers have no keys of their own. */
    static final Set<String> RAW_FORMAT_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("text_file", "binary_file")));

    /**
     * the file format for a ConfigMap/Secret key by its extension, e.g. "app.yaml" -> the YAML format.
     * Formats list their extensions with the leading dot (".yaml").
     *
     * The raw formats (mConfigFormatRawFile: .txt, .cfg, .bin) are left out: their layer holds the whole content
     * under the empty key, so the key's name would be lost, and several such keys would collide.
     *
     * @return the format, or null if the key has no extension or no structured format claims it
     */
     static ConfigFileFormatInterface findFormatForKey(String key, Collection<ConfigFormatInterface> formats)
        {
        int lastDot = key.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == key.length()-1)
            { return null; }
        String extension = key.substring(lastDot); // with the dot
        for (ConfigFormatInterface fmt : formats)
            {
            if (fmt instanceof ConfigFileFormatInterface
                    && !RAW_FORMAT_IDS.contains(fmt.getFormatID())
                    && ((ConfigFileFormatInterface) fmt).getFilenameExtensions().contains(extension))
                { return (ConfigFileFormatInterface) fmt; }
            }
        return null;
        }

    void processResourceData(K8sResourceData resourceData, ConfigLocation location, LayeredConfigurationInterface layeredCfg, boolean isSecret)
        {
        Map<String, byte[]> data = resourceData.getData();
        if (data == null || data.isEmpty()) return;

        // Collect formats
        Map<String, ConfigFormatInterface> formats = context.getConfigFormats();

        for (Map.Entry<String, byte[]> entry : data.entrySet())
            {
            String key = entry.getKey();
            byte[] content = entry.getValue();

            // ConfigMaps only: a format layer would hold a Secret's values as plain entries, without SecretValue,
            // redaction or wiping. A Secret key stays one secret entry, whatever its extension.
            ConfigFormatInterface detectedFormat = isSecret ? null : findFormatForKey(key, formats.values());

            if (detectedFormat instanceof ConfigFileFormatInterface)
                {
                try (InputStream is = new ByteArrayInputStream(content))
                    {
                    layeredCfg.add(((ConfigFileFormatInterface) detectedFormat).readStream(is, location), location);
                    }
                catch (ConfigParseException e)
                    {
                    // a Secret's content never appears in the report, whatever the schema says
                    ParseProblemReporter.report(e, location, layeredCfg, isSecret);
                    }
                catch (IOException e)
                    {
                    logger.warn("Kubernetes: Failed to parse entry "+LogSafe.text(key)+" with format "+(detectedFormat != null ? detectedFormat.getFormatID() : "unknown")+": "+LogSafe.text(e.getMessage()));
                    }
                }
            else
                {
                // No format detected or simple key-value
                if (isSecret)
                    {
                    layeredCfg.add(new KubernetesSecretLayer(context, location, Collections.singletonMap(key, content)), location);
                    }
                else
                    {
                    layeredCfg.add(new KubernetesConfigLayer(context, location, Collections.singletonMap(key, new String(content, StandardCharsets.UTF_8))), location);
                    }
                }
            }
        }

    private void ensureWatch(String ns, String kind, String name)
        {
        String watchKey = ns+"/"+kind+"/"+name;
        if (watches.containsKey(watchKey)) return;

        String path = API_V1_NAMESPACES+ns+"/"+kind+"s"+WATCH_PARAMS+name;

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint+path))
                .GET();

        addAuthorization(reqBuilder);

        WatchHandle handle = new WatchHandle();
        watches.put(watchKey, handle);

        httpClient.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response->
                    {
                    if (response.statusCode() == 200)
                        {
                        handle.setStream(response.body());
                        new Thread(()->
                            {
                            try (java.util.stream.Stream<String> lines = response.body())
                                {
                                lines.forEach(line->
                                    {
                                    if (line.contains("\"MODIFIED\"") || line.contains("\"ADDED\"") || line.contains("\"DELETED\""))
                                        {
                                        logger.info("Kubernetes: Change detected for "+LogSafe.text(watchKey));
                                        changed = true;
                                        }
                                    });
                                }
                            catch (Exception e)
                                {
                                logger.debug("Kubernetes: Watch stream closed for "+LogSafe.text(watchKey)+": "+LogSafe.text(e.getMessage()));
                                }
                            finally
                                {
                                watches.remove(watchKey);
                                handle.close();
                                }
                            }, "k8s-watch-"+watchKey).start();
                        }
                    else
                        {
                        logger.warn("Kubernetes: Watch failed for "+LogSafe.text(watchKey)+" (HTTP "+response.statusCode()+")");
                        watches.remove(watchKey);
                        }
                    })
                .exceptionally(ex->
                    {
                    logger.warn("Kubernetes: Watch error for "+LogSafe.text(watchKey)+": "+LogSafe.text(ex.getMessage()));
                    watches.remove(watchKey);
                    return null;
                    });
        }

    /**
     * @param json   the API response for a ConfigMap or Secret
     * @param secret true for a Secret: its {@code data} is Base64, as {@code binaryData} always is.
     *               A ConfigMap's {@code data} is plain text (until 0.8.17 it was Base64-decoded as well, so
     *               ordinary values were dropped as "not base64" or decoded into garbage).
     */
    K8sResourceData parseK8sResponse(String json, final boolean secret)
        {
        final Map<String, byte[]> data = new HashMap<>();
        final Map<String, byte[]> binaryData = new HashMap<>();

        try
            {
            JsonStreamParser.parseJson(json, new JsonStreamParser.JsonStreamConsumer()
            {
                private String  currentKey;
                private boolean inData       = false;
                private boolean inBinaryData = false;

                @Override
                public void consumeObjectEntryStart(int line, int column, int level, String key)
                    {
                    if (level == 1)
                        {
                        inData = "data".equals(key);
                        inBinaryData = "binaryData".equals(key);
                        }
                    if (level == 2 && (inData || inBinaryData))
                        {
                        currentKey = key;
                        }
                    }

                @Override
                public void consumeString(int line, int column, int level, String string)
                    {
                    if (level == 2 && (inData || inBinaryData) && currentKey != null)
                        {
                        try
                            {
                            // binaryData is Base64, and so is a Secret's data; a ConfigMap's data is plain UTF-8 text
                            byte[] decoded = (inBinaryData || secret)
                                    ? Base64.getDecoder().decode(string.trim())
                                    : string.getBytes(StandardCharsets.UTF_8);
                            if (inBinaryData)
                                {
                                binaryData.put(currentKey, decoded);
                                }
                            else
                                {
                                data.put(currentKey, decoded);
                                }
                            }
                        catch (IllegalArgumentException e)
                            {
                            logger.warn("Kubernetes: Failed to decode "+(inBinaryData ? "binaryData" : "data")+" for key "+LogSafe.text(currentKey)+" (not base64)");
                            }
                        }
                    }

                @Override
                public void consumeObjectEntryEnd(int line, int column, int level)
                    {
                    currentKey = null;
                    }

                @Override
                public void consumeNull(int line, int column, int level) { }

                @Override
                public void consumeFalse(int line, int column, int level) { }

                @Override
                public void consumeTrue(int line, int column, int level) { }

                @Override
                public void consumeNumberInteger(int line, int column, int level, int i) { }

                @Override
                public void consumeNumberLong(int line, int column, int level, long l) { }

                @Override
                public void consumeNumberDouble(int line, int column, int level, double v) { }

                @Override
                public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger) { }

                @Override
                public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal) { }

                @Override
                public void consumeObjectStart(int line, int column, int level) { }

                @Override
                public void consumeObjectEnd(int line, int column, int level)
                    {
                    if (level == 1)
                        {
                        inData = false;
                        inBinaryData = false;
                        }
                    }

                @Override
                public void consumeArrayStart(int line, int column, int level) { }

                @Override
                public void consumeArrayEnd(int line, int column, int level) { }
            });
            }
        catch (Exception e)
            {
            logger.warn("Kubernetes: JSON parsing failed: "+LogSafe.text(e.getMessage()));
            }
        return new K8sResourceData(data, binaryData);
        }

    @Override
    public ConfigLayerInterface createConfigurationLayer(String configName, ConfigLocation location, org.metabit.platform.support.config.schema.ConfigSchema configSchema, LayeredConfiguration layeredConfiguration)
        {
        if (location == null || !STORAGE_ID.equals(location.getStorage().getStorageID()))
            {
            return null;
            }
        // Kubernetes sources are read-only in this implementation.
        return null;
        }

    @Override
    public void provideAdditionalLayers(String sanitizedConfigName, LayeredConfigurationInterface layeredCfg)
        {
        // For Kubernetes, we don't need to add global additional layers here
        // as we use explicit ConfigLocations in the search list.
        }

    @Override
    public void updateBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        if (httpClient == null || !STORAGE_ID.equals(location.getStorage().getStorageID()))
            {
            return;
            }

        K8sResourceSpec spec = K8sResourceSpec.parse(location.getStorageInstanceHandle().toString(), this.namespace);
        if (spec == null) return;

        String path;
        if ("configmap".equalsIgnoreCase(spec.kind))
            {
            path = API_V1_NAMESPACES+spec.namespace+CONFIGMAPS+spec.name;
            }
        else if ("secret".equalsIgnoreCase(spec.kind))
            {
            path = API_V1_NAMESPACES+spec.namespace+SECRETS+spec.name;
            }
        else return;

        try
            {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint+path))
                    .GET();

            addAuthorization(reqBuilder);

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
                {
                K8sResourceData resourceData = parseK8sResponse(response.body(), "secret".equalsIgnoreCase(spec.kind));
                for (Map.Entry<String, byte[]> entry : resourceData.getData().entrySet())
                    {
                    byte[] bytes = entry.getValue();
                    if (bytes.length > maxKeySize)
                        {
                        logger.warn("Kubernetes: Skipping key "+LogSafe.text(entry.getKey())+" in "+LogSafe.text(spec)+" because it exceeds maxKeySize");
                        continue;
                        }
                    blobConfig.putBlob(bytes, location.getScope());
                    }
                for (Map.Entry<String, byte[]> entry : resourceData.getBinaryData().entrySet())
                    {
                    if (entry.getValue().length > maxKeySize)
                        {
                        logger.warn("Kubernetes: Skipping binary key "+LogSafe.text(entry.getKey())+" in "+LogSafe.text(spec)+" because it exceeds maxKeySize");
                        continue;
                        }
                    blobConfig.putBlob(entry.getValue(), location.getScope());
                    }
                }
            }
        catch (Exception e)
            {
            logger.warn("Kubernetes: Failed to fetch binary data for "+LogSafe.text(spec)+": "+LogSafe.text(e.getMessage()));
            }
        }

    @Override
    public Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        // Not implemented for Kubernetes API sources yet.
        return Collections.emptySet();
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        boolean state = changed;
        changed = false;
        return state;
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        // Change check is primarily handled by background watches.
        }

    @Override
    public void exit()
        {
        stopTokenWatcher();
        for (WatchHandle handle : watches.values())
            {
            handle.close();
            }
        watches.clear();
        httpClient = null;
        }

    /**
     * Starts a token watcher for changes in the service account token file.
     * NB:
     * The token file will not be updated by writing.
     * Instead, it will be updated by Kubernetes by rotating the symlink.
     * Using exponential backoff to ensure we read the token once it is ready.
     */
    private void startTokenWatcher()
        {
        if (saTokenPath == null) return;
        File tokenFile = new File(saTokenPath);
        File parentDir = tokenFile.getParentFile();
        if (parentDir == null || !parentDir.exists()) return;

        try
            {
            this.tokenWatchService = java.nio.file.FileSystems.getDefault().newWatchService();
            parentDir.toPath().register(tokenWatchService,
                    java.nio.file.StandardWatchEventKinds.ENTRY_CREATE,
                    java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY,
                    java.nio.file.StandardWatchEventKinds.ENTRY_DELETE);
            tokenWatchThread = new Thread(()->
                {
                try
                    {
                    while (!Thread.currentThread().isInterrupted())
                        {
                        java.nio.file.WatchKey key = tokenWatchService.take();
                        boolean tokenChanged = false;
                        for (java.nio.file.WatchEvent<?> event : key.pollEvents())
                            {
                            java.nio.file.Path changedPath = (java.nio.file.Path) event.context();
                            String filename = changedPath.toString();
                            // Kubernetes rotates tokens by updating symlinks, often involving "..data"
                            if (filename.equals(tokenFile.getName()) || filename.equals("..data"))
                                {
                                tokenChanged = true;
                                }
                            }
                        if (tokenChanged)
                            {
                            // brief pause to allow Kubernetes symlink rotation to complete before re-reading the token
                            // we use an exponential backoff to ensure we read the token once it is ready
                            long[] backoffs = {0, 50, 100, 200, 1000, 5000};
                            boolean success = false;
                            for (long backoff : backoffs)
                                {
                                if (backoff > 0)
                                    {
                                    Thread.sleep(backoff);
                                    }
                                if (reloadTokenInternal())
                                    {
                                    success = true;
                                    break;
                                    }
                                }
                            if (!success)
                                {
                                logger.warn("Kubernetes: Failed to reload token after multiple attempts");
                                }
                            }
                        if (!key.reset()) break;
                        }
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }
                catch (Exception e)
                    {
                    logger.warn("Kubernetes: Token watcher thread error: "+LogSafe.text(e.getMessage()));
                    }
                }, "k8s-token-watcher");
            tokenWatchThread.setDaemon(true);
            tokenWatchThread.start();
            logger.info("Kubernetes: Started token watcher for "+LogSafe.text(saTokenPath));
            }
        catch (IOException e)
            {
            logger.warn("Kubernetes: Failed to start token watcher: "+LogSafe.text(e.getMessage()));
            }
        }

    private synchronized void reloadToken()
        {
        reloadTokenInternal();
        }

    private synchronized boolean reloadTokenInternal()
        {
        try
            {
            File tokenFile = new File(saTokenPath);
            if (tokenFile.exists())
                {
                byte[] newToken = readTokenFile(tokenFile);
                if (newToken == null || newToken.length == 0)
                    {
                    return false;
                    }
                if (!Arrays.equals(newToken, this.token))
                    {
                    if (this.token != null)
                        {
                        Arrays.fill(this.token, (byte) 0);
                        }
                    this.token = newToken;
                    logger.info("Kubernetes: ServiceAccount token reloaded");
                    }
                else
                    {
                    Arrays.fill(newToken, (byte) 0);
                    }
                return true;
                }
            }
        catch (Exception e)
            {
            logger.warn("Kubernetes: Failed to reload token: "+LogSafe.text(e.getMessage()));
            }
        return false;
        }

    private byte[] readTokenFile(File file)
        {
        try (InputStream is = Files.newInputStream(file.toPath()))
            {
            byte[] raw = is.readAllBytes();
            // trim whitespace
            int start = 0;
            while (start < raw.length && Character.isWhitespace(raw[start])) start++;
            int end = raw.length;
            while (end > start && Character.isWhitespace(raw[end-1])) end--;

            if (start > 0 || end < raw.length)
                {
                byte[] trimmed = Arrays.copyOfRange(raw, start, end);
                Arrays.fill(raw, (byte) 0);
                return trimmed;
                }
            return raw;
            }
        catch (IOException e)
            {
            logger.warn("Kubernetes: Failed to read token file "+LogSafe.text(file)+": "+LogSafe.text(e.getMessage()));
            return null;
            }
        }

    private void stopTokenWatcher()
        {
        if (tokenWatchThread != null)
            {
            tokenWatchThread.interrupt();
            tokenWatchThread = null;
            }
        if (tokenWatchService != null)
            {
            try
                {
                tokenWatchService.close();
                }
            catch (IOException e)
                {
                // ignore
                }
            tokenWatchService = null;
            }
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
        }

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        Object handle = configLocation.getStorageInstanceHandle();
        return URI.create("kubernetes://"+(handle != null ? handle.toString() : "unknown")+(key != null ? "/"+key : ""));
        }

    @Override
    public boolean isPreferredTestLocation(ConfigLocation location)
        {
        return false;
        }

    private static class WatchHandle
    {
        private java.util.stream.Stream<String> stream;

        public synchronized void setStream(java.util.stream.Stream<String> stream)
            {
            this.stream = stream;
            }

        public synchronized void close()
            {
            if (stream != null)
                {
                try { stream.close(); } catch (Exception ignored) { }
                stream = null;
                }
            }
    }

    private static final class K8sResourceSpec
    {
        final String namespace;
        final String kind;
        final String name;

        K8sResourceSpec(String namespace, String kind, String name)
            {
            this.namespace = namespace;
            this.kind = kind;
            this.name = name;
            }

        static K8sResourceSpec parse(String resourceSpec, String defaultNamespace)
            {
            String[] parts = resourceSpec.split("/");
            if (parts.length == 3)
                {
                return new K8sResourceSpec(parts[0], parts[1], parts[2]);
                }
            else if (parts.length == 2)
                {
                return new K8sResourceSpec(defaultNamespace, parts[0], parts[1]);
                }
            return null;
            }

        @Override
        public String toString()
            {
            return namespace+"/"+kind+"/"+name;
            }
    }

    static class K8sResourceData
    {
        private final Map<String, byte[]> data;
        private final Map<String, byte[]> binaryData;

        public K8sResourceData(Map<String, byte[]> data, Map<String, byte[]> binaryData)
            {
            this.data = data;
            this.binaryData = binaryData;
            }

        public Map<String, byte[]> getData() { return data; }

        public Map<String, byte[]> getBinaryData() { return binaryData; }
    }

    private static class KubernetesConfigLayer extends InMemoryLayer
    {
        public KubernetesConfigLayer(ConfigFactoryInstanceContext ctx, ConfigLocation location, Map<String, String> data)
            {
            super(ctx, location, location.getScope());
            if (data != null)
                {
                for (Map.Entry<String, String> entry : data.entrySet())
                    {
                    putEntry(entry.getKey(), new GenericConfigEntryLeaf(entry.getKey(), entry.getValue(), ConfigEntryType.STRING, new ConfigEntryMetadata((ConfigSource) location)));
                    }
                }
            }
    }

    static class KubernetesSecretLayer implements ConfigLayerInterface
    {
        private final ConfigFactoryInstanceContext context;
        private final ConfigLocation               location;
        private final Map<String, byte[]>          data;

        public KubernetesSecretLayer(ConfigFactoryInstanceContext context, ConfigLocation location, Map<String, byte[]> data)
            {
            this.context = context;
            this.location = location;
            this.data = data != null ? new HashMap<>(data) : Collections.emptyMap();
            }

        @Override
        public ConfigEntry getEntry(String hierarchicalKey)
            {
            byte[] value = data.get(hierarchicalKey);
            if (value == null) return null;

            SecretValue secretValue = new BasicSecretValue(value, SecretType.PLAIN_TEXT);

            return new SecretConfigEntryLeaf(hierarchicalKey, secretValue, new ConfigEntryMetadata((ConfigSource) location));
            }

        @Override
        public void erase()
            {
            context.getLogger().info("Kubernetes: Erasing secret layer for "+location);
            for (byte[] buffer : data.values())
                {
                if (buffer != null)
                    {
                    java.util.Arrays.fill(buffer, (byte) 0);
                    }
                }
            data.clear();
            }

        @Override
        public boolean isWriteable() { return false; }

        @Override
        public boolean isEmpty() { return data.isEmpty(); }

        @Override
        public ConfigScope getScope() { return location.getScope(); }

        @Override
        public void writeEntry(ConfigEntry entryToWrite) { throw new UnsupportedOperationException(); }

        @Override
        public int flush() { return 0; }

        @Override
        public ConfigSource getSource() { return (location instanceof ConfigSource) ? (ConfigSource) location : null; }

        @Override
        public int compareTo(ConfigLayerInterface o)
            {
            if (o == null) return 1;
            return getScope().compareTo(o.getScope());
            }

        @Override
        public Iterator<String> tryToGetKeyIterator() { return data.keySet().iterator(); }

        @Override
        public String toString()
            {
            return "KubernetesSecretLayer{location="+location+", entries="+data.size()+"}";
            }
    }
}
//___EOF___