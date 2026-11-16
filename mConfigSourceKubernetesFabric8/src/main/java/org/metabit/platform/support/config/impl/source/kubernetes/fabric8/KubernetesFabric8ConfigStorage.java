package org.metabit.platform.support.config.impl.source.kubernetes.fabric8;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.EventRecorder;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.BootstrapConfigContext;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.source.core.InMemoryLayer;

import java.io.File;
import java.net.URI;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Kubernetes-based configuration storage module for mConfig.
 * Supports both API-based (ConfigMaps/Secrets) and filesystem-based (/etc/config, /etc/secrets) lookups.
 */
public class KubernetesFabric8ConfigStorage implements ConfigStorageInterface
{
    private static final String STORAGE_ID = "kubernetes-fabric8";
    private static final String NAME       = "Kubernetes API ConfigSource (Fabric8)";

    private static final String DEFAULT_CONFIG_DIR = "/etc/config";
    private static final String DEFAULT_SECRETS_DIR = "/etc/secrets";
    private static final String DEFAULT_SA_TOKEN = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String DEFAULT_SA_NAMESPACE = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";
    private static final String DEFAULT_SA_CA_CERT = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    private static final String DEFAULT_API_ENDPOINT = "https://kubernetes.default.svc";

    private          ConfigFactoryInstanceContext context;
    private          ConfigLoggingInterface       logger;
    private          KubernetesClient             client;
    private final    Map<String, Watch>           watches = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean                      changed = false;

    // defaults are Standard paths; they can be overridden e.g. for testing
    private String configDirPath  = DEFAULT_CONFIG_DIR;
    private String secretsDirPath = DEFAULT_SECRETS_DIR;
    private String saTokenPath    = DEFAULT_SA_TOKEN;
    private String saNamespacePath = DEFAULT_SA_NAMESPACE;
    private String saCaCertPath   = DEFAULT_SA_CA_CERT;
    private String apiEndpoint    = DEFAULT_API_ENDPOINT;
    private String namespace      = null;
    private boolean allowInsecureTransport = false;
    private long   maxKeySize     = 1024L*1024L;

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
        if (settings.getBoolean(KubernetesFabric8Features.BYPASS_TEST))
            {
            return true;
            }
        // in test mode, no connection to a real cluster unless bypassed (as in mConfigSourceKubernetes)
        if (settings.getBoolean(ConfigFeature.TEST_MODE))
            {
            return false;
            }

        String testSaTokenPath = settings.getString(KubernetesFabric8Features.SA_TOKEN_PATH);

        // Check for common Kubernetes environment indicators
        File saToken = new File(testSaTokenPath);
        return saToken.exists();
        }

    @Override
    public boolean init(ConfigFactoryInstanceContext context)
        {
        this.context = context;
        this.logger = context.getLogger();

        resolveSettingsFromFactorySettings(context.getSettings());

        // Auto-discover Kubernetes-typical filesystem search paths
        addStandardPaths();
        return true;
        }

    private void resolveSettingsFromFactorySettings(ConfigFactorySettings settings)
        {
        Object maxKeySizeObj = settings.get(KubernetesFabric8Features.MAX_KEY_SIZE);
        if (maxKeySizeObj instanceof Number)
            {
            this.maxKeySize = ((Number) maxKeySizeObj).longValue();
            }

        String configDirPathStr = settings.getString(KubernetesFabric8Features.CONFIG_DIR_PATHS);
        if (configDirPathStr != null) this.configDirPath = configDirPathStr;

        String secretsDirPathStr = settings.getString(KubernetesFabric8Features.SECRETS_DIR_PATHS);
        if (secretsDirPathStr != null) this.secretsDirPath = secretsDirPathStr;

        String saTokenPathStr = settings.getString(KubernetesFabric8Features.SA_TOKEN_PATH);
        if (saTokenPathStr != null) this.saTokenPath = saTokenPathStr;

        String endpoint = settings.getString(KubernetesFabric8Features.API_ENDPOINT);
        if (endpoint != null) this.apiEndpoint = endpoint.trim(); // the https check and the client see the same value

        String ns = settings.getString(KubernetesFabric8Features.NAMESPACE);
        if (ns != null && !ns.trim().isEmpty()) this.namespace = ns.trim();

        String saNamespacePathStr = settings.getString(KubernetesFabric8Features.SA_NAMESPACE_PATH);
        if (saNamespacePathStr != null) this.saNamespacePath = saNamespacePathStr;

        String saCaCertPathStr = settings.getString(KubernetesFabric8Features.SA_CA_CERT_PATH);
        if (saCaCertPathStr != null) this.saCaCertPath = saCaCertPathStr;

        this.allowInsecureTransport = settings.getBoolean(KubernetesFabric8Features.ALLOW_INSECURE_TRANSPORT);
        }

    private void resolveSettingsFromConfiguration(Configuration config)
        {
        Long maxKeySizeVal = readBootstrapSetting(config, KubernetesFabric8Features.MAX_KEY_SIZE.name(), config::getLong);
        if (maxKeySizeVal != null) this.maxKeySize = maxKeySizeVal;

        String configDirPathStr = readBootstrapSetting(config, KubernetesFabric8Features.CONFIG_DIR_PATHS.name(), config::getString);
        if (configDirPathStr != null) this.configDirPath = configDirPathStr;

        String secretsDirPathStr = readBootstrapSetting(config, KubernetesFabric8Features.SECRETS_DIR_PATHS.name(), config::getString);
        if (secretsDirPathStr != null) this.secretsDirPath = secretsDirPathStr;

        String saTokenPathStr = readBootstrapSetting(config, KubernetesFabric8Features.SA_TOKEN_PATH.name(), config::getString);
        if (saTokenPathStr != null) this.saTokenPath = saTokenPathStr;

        String endpoint = readBootstrapSetting(config, KubernetesFabric8Features.API_ENDPOINT.name(), config::getString);
        if (endpoint != null) this.apiEndpoint = endpoint.trim(); // the https check and the client see the same value

        String ns = readBootstrapSetting(config, KubernetesFabric8Features.NAMESPACE.name(), config::getString);
        if (ns != null && !ns.trim().isEmpty()) this.namespace = ns.trim();

        String saNamespacePathStr = readBootstrapSetting(config, KubernetesFabric8Features.SA_NAMESPACE_PATH.name(), config::getString);
        if (saNamespacePathStr != null) this.saNamespacePath = saNamespacePathStr;

        String saCaCertPathStr = readBootstrapSetting(config, KubernetesFabric8Features.SA_CA_CERT_PATH.name(), config::getString);
        if (saCaCertPathStr != null) this.saCaCertPath = saCaCertPathStr;

        Boolean allowInsecure = readBootstrapSetting(config, KubernetesFabric8Features.ALLOW_INSECURE_TRANSPORT.name(), config::getBoolean);
        if (allowInsecure != null) this.allowInsecureTransport = allowInsecure;
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
                { logger.warn("KubernetesFabric8ConfigStorage: ignoring invalid bootstrap setting "+key+": "+LogSafe.text(e.getMessage())); }
            return null;
            }
        }

    @Override
    public void postInit(BootstrapConfigContext bootstrapCtx)
        {
        if (bootstrapCtx != null)
            {
            // Re-resolve in case more settings were added between init and postInit.
            // Unlike Vault, AWS and ZooKeeper, which read only the four local scopes, Kubernetes also reads
            // SESSION and RUNTIME, deliberately: in a pod, the environment is set by the pod spec and the kubelet
            // (deployment configuration, not foreign input), and RUNTIME carries intentional overrides from code.
            // Neither scope is served by a source that depends on this storage, so there is no circular read.
            Configuration k8sConfig = bootstrapCtx.getRestrictedConfig("kubernetes", EnumSet.of(ConfigScope.PRODUCT, ConfigScope.HOST, ConfigScope.USER, ConfigScope.APPLICATION, ConfigScope.SESSION, ConfigScope.RUNTIME));
            resolveSettingsFromConfiguration(k8sConfig);
            }

        // and re-add paths if they were not added in init or changed
        addStandardPaths();

        try
            {
            // Fabric8 client uses Jackson internally for JSON/YAML processing.
            // In some environments (like K8s sidecars or specific microservices),
            // ensure that the runtime classpath contains compatible Jackson versions.
            this.client = new KubernetesClientBuilder().withConfig(buildClientConfig()).build();
            logger.info("KubernetesFabric8ConfigStorage: API client initialized (endpoint: "+LogSafe.text(apiEndpoint)+", namespace: "+LogSafe.text(client.getNamespace())+")");
            }
        catch (Exception e)
            {
            logger.warn("KubernetesFabric8ConfigStorage: Failed to initialize API client: "+LogSafe.text(e.getMessage()));
            }
        }

    /**
     * the client configuration, from this module's settings only.
     * Fabric8's auto-configuration (kubeconfig file, environment variables, system properties) is not used:
     * it could direct the ServiceAccount token to an endpoint nobody configured for this module.
     * The token is sent only over https, unless ALLOW_INSECURE_TRANSPORT is set.
     */
    Config buildClientConfig()
        {
        ConfigBuilder builder = new ConfigBuilder(Config.empty()).withMasterUrl(apiEndpoint);
        String ns = (namespace != null) ? namespace : readTrimmedFile(saNamespacePath);
        if (ns != null && !ns.isEmpty())
            {
            builder.withNamespace(ns);
            }
        File caCert = new File(saCaCertPath);
        if (caCert.isFile())
            {
            builder.withCaCertFile(caCert.getAbsolutePath());
            }
        else if (allowInsecureTransport)
            {
            // for local and mock testing without a CA certificate; only with the explicit opt-in
            builder.withTrustCerts(true);
            }
        if (isSecureTransport(apiEndpoint) || allowInsecureTransport)
            {
            // read at every request: projected tokens are rotated by the kubelet
            builder.withOauthTokenProvider(()->readTrimmedFile(saTokenPath));
            }
        else
            {
            logger.warn("KubernetesFabric8ConfigStorage: the API endpoint is not https ("+LogSafe.text(apiEndpoint)+"); the ServiceAccount token is not sent. "
                    +"Set "+KubernetesFabric8Features.ALLOW_INSECURE_TRANSPORT.name()+"=true to allow it (development only).");
            }
        return builder.build();
        }

    static boolean isSecureTransport(String address)
        {
        return address != null && address.trim().toLowerCase(Locale.ROOT).startsWith("https://");
        }

    private String readTrimmedFile(String path)
        {
        if (path == null)
            { return null; }
        File file = new File(path);
        if (!file.isFile())
            { return null; }
        try
            {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
            }
        catch (IOException e)
            {
            logger.warn("KubernetesFabric8ConfigStorage: Failed to read "+LogSafe.text(path)+": "+LogSafe.text(e.getMessage()));
            return null;
            }
        }

    // for tests
    KubernetesClient getClient()
        {
        return client;
        }

    /**
     * Adds standard configuration paths to the search list.
     * As of now, no hot-reload if these do not exist at startup. For a K8s environment,
     * these are expected to exist in the container before code inside of it is started.
     */
    private final Set<String> addedPaths = new HashSet<>();

    private void addStandardPaths()
        {
        if (configDirPath != null && !addedPaths.contains(configDirPath))
        {
            File configDir = new File(configDirPath);
            if (configDir.exists() && configDir.isDirectory())
                {
                logger.info("KubernetesFabric8ConfigStorage: Adding "+LogSafe.text(configDirPath)+" to search list");
                ConfigStorageInterface fileStorage = context.getConfigStorages().get("files");
                if (fileStorage != null)
                    {
                    context.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.CLUSTER, fileStorage, null, Paths.get(configDirPath)), ConfigScope.CLUSTER);
                    addedPaths.add(configDirPath);
                    }
                }
        }

        if (secretsDirPath != null && !addedPaths.contains(secretsDirPath))
        {
            File secretsDir = new File(secretsDirPath);
            if (secretsDir.exists() && secretsDir.isDirectory())
                {
                logger.info("KubernetesFabric8ConfigStorage: Adding "+LogSafe.text(secretsDirPath)+" to search list");
                ConfigStorageInterface fileStorage = context.getConfigStorages().get("files");
                if (fileStorage != null)
                    {
                    context.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.CLUSTER, fileStorage, null, Paths.get(secretsDirPath)), ConfigScope.CLUSTER);
                    addedPaths.add(secretsDirPath);
                    }
                }
        }
    }

    @Override
    public void updateConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        if (client == null || !STORAGE_ID.equals(possibleSource.getStorage().getStorageID()))
            {
            return;
            }

        Object handle = possibleSource.getStorageInstanceHandle();
        if (handle == null) return;

        K8sResourceSpec spec = K8sResourceSpec.parse(handle.toString(), client.getNamespace());
        if (spec == null)
            {
            logger.warn("KubernetesFabric8ConfigStorage: Invalid resource spec: "+LogSafe.text(handle));
            return;
            }

        try
            {
            if ("configmap".equalsIgnoreCase(spec.kind))
                {
                ConfigMap cm = client.configMaps().inNamespace(spec.namespace).withName(spec.name).get();
                if (cm != null && cm.getData() != null)
                    {
                    Map<String, String> filteredData = new HashMap<>();
                    for (Map.Entry<String, String> entry : cm.getData().entrySet())
                        {
                        if (entry.getValue() != null && entry.getValue().length() > maxKeySize)
                            {
                            logger.warn("KubernetesFabric8ConfigStorage: Skipping key "+LogSafe.text(entry.getKey())+" in ConfigMap "+LogSafe.text(spec)+" because it exceeds maxKeySize ("+maxKeySize+")");
                            continue;
                            }
                        filteredData.put(entry.getKey(), entry.getValue());
                        }
                    layeredCfg.add(new KubernetesConfigLayer(context, possibleSource, filteredData), possibleSource);
                    ensureWatch(spec.namespace, spec.kind, spec.name);
                    }
                }
            else if ("secret".equalsIgnoreCase(spec.kind))
                {
                Secret secret = client.secrets().inNamespace(spec.namespace).withName(spec.name).get();
                if (secret != null && secret.getData() != null)
                    {
                    Map<String, String> filteredData = new HashMap<>();
                    for (Map.Entry<String, String> entry : secret.getData().entrySet())
                        {
                        // Fabric8 getData() returns decoded strings for secret data.
                        if (entry.getValue() != null && entry.getValue().length() > maxKeySize)
                            {
                            logger.warn("KubernetesFabric8ConfigStorage: Skipping key "+LogSafe.text(entry.getKey())+" in Secret "+LogSafe.text(spec)+" because it exceeds maxKeySize ("+maxKeySize+")");
                            continue;
                            }
                        filteredData.put(entry.getKey(), entry.getValue());
                        }
                    layeredCfg.add(new KubernetesSecretLayer(context, possibleSource, filteredData), possibleSource);
                    ensureWatch(spec.namespace, spec.kind, spec.name);
                    }
                }
            }
        catch (Exception e)
            {
            EventRecorder.recordSourceUnavailable(context, possibleSource, "KUBERNETES_READ_FAILED",
                    "KubernetesFabric8ConfigStorage: Failed to fetch "+LogSafe.text(spec)+": "+LogSafe.text(e.getMessage()));
            }
        }

    private void onWatchClose(String watchKey, WatcherException cause)
        {
        if (cause != null)
            {
            logger.warn("KubernetesFabric8ConfigStorage: Watch closed for "+LogSafe.text(watchKey)+" due to error: "+LogSafe.text(cause.getMessage()));
            }
        else
            {
            logger.info("KubernetesFabric8ConfigStorage: Watch closed normally for "+LogSafe.text(watchKey));
            }
        watches.remove(watchKey);
        }

    private void ensureWatch(String namespace, String kind, String name)
        {
        String watchKey = namespace+"/"+kind+"/"+name;
        if (watches.containsKey(watchKey)) return;

        try
            {
            if ("configmap".equalsIgnoreCase(kind))
                {
                Watch watch = client.configMaps().inNamespace(namespace).withName(name).watch(new Watcher<ConfigMap>()
                {
                    @Override
                    public void eventReceived(Action action, ConfigMap resource)
                        {
                        logger.info("KubernetesFabric8ConfigStorage: Change detected for ConfigMap "+LogSafe.text(watchKey)+" ("+action+")");
                        changed = true;
                        }

                    @Override
                    public void onClose(WatcherException cause)
                        {
                        onWatchClose(watchKey, cause);
                        }
                });
                watches.put(watchKey, watch);
                }
            else if ("secret".equalsIgnoreCase(kind))
                {
                Watch watch = client.secrets().inNamespace(namespace).withName(name).watch(new Watcher<Secret>()
                {
                    @Override
                    public void eventReceived(Action action, Secret resource)
                        {
                        logger.info("KubernetesFabric8ConfigStorage: Change detected for Secret "+LogSafe.text(watchKey)+" ("+action+")");
                        changed = true;
                        }

                    @Override
                    public void onClose(WatcherException cause)
                        {
                        onWatchClose(watchKey, cause);
                        }
                });
                watches.put(watchKey, watch);
                }
            }
        catch (Exception e)
            {
            logger.warn("KubernetesFabric8ConfigStorage: Failed to start watch for "+LogSafe.text(watchKey)+": "+LogSafe.text(e.getMessage()));
            }
        }

    @Override
    public ConfigLayerInterface createConfigurationLayer(String configName, ConfigLocation location, ConfigSchema configSchema, LayeredConfiguration layeredConfiguration)
        {
        if (!STORAGE_ID.equals(location.getStorage().getStorageID()))
            {
            return null;
            }

        // Handle specialized creation logic if needed
        return null;
        }

    @Override
    public void provideAdditionalLayers(String sanitizedConfigName, LayeredConfigurationInterface layeredCfg)
        {
        // This is where Secrets would be injected into the layered configuration.
        // In this implementation, we handle both ConfigMaps and Secrets in updateConfigurationLayers
        // based on the ConfigLocation handle.
        }

    @Override
    public Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
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
        // Change check is handled by Watches, but we could force a refresh here if needed
        }

    @Override
    public void exit()
        {
        for (Watch watch : watches.values())
            {
            try { watch.close(); } catch (Exception ignored) { }
            }
        watches.clear();

        if (client != null)
            {
            try { client.close(); } catch (Exception ignored) { }
            client = null;
            }
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
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

    private static class KubernetesSecretLayer implements ConfigLayerInterface
    {
        private final ConfigFactoryInstanceContext context;
        private final ConfigLocation               location;
        private final Map<String, String>          data;

        public KubernetesSecretLayer(ConfigFactoryInstanceContext context, ConfigLocation location, Map<String, String> data)
            {
            this.context = context;
            this.location = location;
            this.data = data != null ? new HashMap<>(data) : Collections.emptyMap();
            }

        @Override
        public ConfigEntry getEntry(String hierarchicalKey)
            {
            String value = data.get(hierarchicalKey);
            if (value == null) return null;

            byte[] tmpBytes = value.getBytes(StandardCharsets.UTF_8);
            SecretValue secretValue = new BasicSecretValue(tmpBytes, SecretType.PLAIN_TEXT);
            java.util.Arrays.fill(tmpBytes, (byte) 0); // the constructor copied it

            ConfigEntryMetadata meta = new ConfigEntryMetadata((ConfigSource) location);
            
            // Set feature flag to metadata if supported by SecretConfigEntryLeaf 
            // (e.g. via flags in metadata or similar)
            
            return new SecretConfigEntryLeaf(hierarchicalKey, secretValue, meta);
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

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        // Example: kubernetes://namespace/configmap/name#key
        Object handle = configLocation.getStorageInstanceHandle();
        return URI.create("kubernetes://"+(handle != null ? handle.toString() : "unknown")+(key != null ? "/"+key : ""));
        }

    @Override
    public void updateBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        if (client == null || !STORAGE_ID.equals(location.getStorage().getStorageID()))
            {
            return;
            }

        Object handle = location.getStorageInstanceHandle();
        if (handle == null) return;

        K8sResourceSpec spec = K8sResourceSpec.parse(handle.toString(), client.getNamespace());
        if (spec == null) return;

        try
            {
            if ("configmap".equalsIgnoreCase(spec.kind))
                {
                ConfigMap cm = client.configMaps().inNamespace(spec.namespace).withName(spec.name).get();
                if (cm != null)
                    {
                    Map<String, String> binaryData = cm.getBinaryData();
                    if (binaryData != null)
                        {
                        for (Map.Entry<String, String> entry : binaryData.entrySet())
                            {
                            // Binary data in fabric8 is base64 encoded strings
                            byte[] decoded = Base64.getDecoder().decode(entry.getValue());
                            if (decoded.length > maxKeySize)
                                {
                                logger.warn("KubernetesFabric8ConfigStorage: Skipping binary key "+LogSafe.text(entry.getKey())+" in ConfigMap "+LogSafe.text(spec)+" because it exceeds maxKeySize ("+maxKeySize+")");
                                continue;
                                }
                            blobConfig.putBlob(decoded, location.getScope());
                            }
                        }
                    Map<String, String> data = cm.getData();
                    if (data != null)
                        {
                        for (Map.Entry<String, String> entry : data.entrySet())
                            {
                            byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                            if (bytes.length > maxKeySize)
                                {
                                logger.warn("KubernetesFabric8ConfigStorage: Skipping key "+LogSafe.text(entry.getKey())+" in ConfigMap "+LogSafe.text(spec)+" because it exceeds maxKeySize ("+maxKeySize+")");
                                continue;
                                }
                            blobConfig.putBlob(bytes, location.getScope());
                            }
                        }
                    }
                }
            else if ("secret".equalsIgnoreCase(spec.kind))
                {
                Secret secret = client.secrets().inNamespace(spec.namespace).withName(spec.name).get();
                if (secret != null && secret.getData() != null)
                    {
                    for (Map.Entry<String, String> entry : secret.getData().entrySet())
                        {
                        // Fabric8 getData() returns base64 encoded strings for Secrets
                        byte[] decoded = Base64.getDecoder().decode(entry.getValue());
                        if (decoded.length > maxKeySize)
                            {
                            logger.warn("KubernetesFabric8ConfigStorage: Skipping secret key "+LogSafe.text(entry.getKey())+" in Secret "+LogSafe.text(spec)+" because it exceeds maxKeySize ("+maxKeySize+")");
                            continue;
                            }
                        blobConfig.putBlob(decoded, location.getScope());
                        }
                    }
                }
            }
        catch (Exception e)
            {
            logger.warn("KubernetesFabric8ConfigStorage: Failed to fetch binary data for "+LogSafe.text(spec)+": "+LogSafe.text(e.getMessage()));
            }
        }

    @Override
    public boolean isPreferredTestLocation(ConfigLocation location)
        {
        return false;
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
            return namespace + "/" + kind + "/" + name;
            }
    }
}
