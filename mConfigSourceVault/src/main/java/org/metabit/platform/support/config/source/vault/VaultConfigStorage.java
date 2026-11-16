/*
 * Copyright 2018-2026 metabit GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metabit.platform.support.config.source.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.EventRecorder;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Implementation of Vault storage for mConfig.
 */
public final class VaultConfigStorage implements ConfigStorageInterface
{
    public static final int HTTP_OK        = 200;
    static final int        HTTP_FORBIDDEN = 403;
    static final int        HTTP_NOT_FOUND = 404;
    static final String DEFAULT_SA_TOKEN_PATH            = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    static final String DEFAULT_KUBERNETES_MOUNT         = "kubernetes";
    static final String KUBERNETES_SA_TOKEN_PATH_FEATURE = "KUBERNETES_SA_TOKEN_PATH";
    // login and the swap of client and config are guarded by authLock; reads use the current client without locking.
    // Change checks run on another thread than the application's reads, so both may hit an expired token at once.
    private final    Object                 authLock = new Object();
    private volatile VaultConfig            vaultConfig;
    private volatile Vault                  vault;
    private EnvironmentLoader      environmentLoader;
    private boolean                allowInsecureTransport = false;
    private ConfigLoggingInterface         logger;
    private ConfigFactoryInstanceContext   ctx;
    private String                 enginePath;
    private int                    kvVersion;
    private long                   lastCheck    = 0;
    private long                   pollInterval;
    private volatile boolean       initialized  = false;
    private String                 addressOverride;
    private String                 tokenOverride;
    // Kubernetes login settings, kept for the re-login; resolved once in postInit()
    private Path                   kubernetesTokenPath;
    private String                 kubernetesRole;
    private String                 kubernetesMount;
    private boolean                kubernetesAuth = false;

    @Override
    public String getStorageName()
        {
        return "HashiCorp Vault";
        }

    @Override
    public String getStorageID()
        {
        return "vault";
        }

    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        if (settings.getBoolean(ConfigFeature.TEST_MODE) && !settings.getBoolean(VaultFeatures.BYPASS_TEST))
            {
            return false;
            }
        return true;
        }

    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.logger = ctx.getLogger();
        this.ctx = ctx;
        return true;
        }

    @Override
    public void postInit(BootstrapConfigContext bootstrapCtx)
        {
        // Self-configuration from lower-precedence parts of mConfig.
        // Scope is restricted to local sources to prevent circular reads against this (not yet initialized) storage.
        String bootstrapName = ctx.getSettings().getString(VaultFeatures.BOOTSTRAP_CONFIG_NAME);
        Configuration cfg = bootstrapCtx.getRestrictedConfig(bootstrapName, EnumSet.of(ConfigScope.PRODUCT, ConfigScope.HOST, ConfigScope.USER, ConfigScope.APPLICATION));
        String address = null;
        try
            {
            // The module stays inactive unless a Vault address was configured explicitly.
            // There is no default address: credentials must only go to an endpoint the operator chose.
            address = (addressOverride != null) ? addressOverride : optString(cfg, "address");
            if (address == null || address.trim().isEmpty())
                {
                if (logger != null)
                    {
                    logger.info("Vault storage inactive: no address configured (key 'address' in the '"+LogSafe.text(bootstrapName)+"' configuration).");
                    }
                return;
                }
            this.allowInsecureTransport = Boolean.TRUE.equals(optBoolean(cfg, "auth.allowInsecureTransport"));
            this.environmentLoader = new AllowlistEnvironmentLoader(allowInsecureTransport);

            this.enginePath = cfg.getString("engine");
            this.kvVersion = cfg.getInteger("kvVersion");
            this.pollInterval = cfg.getLong("pollInterval");

            this.kubernetesTokenPath = Paths.get(resolveTokenPath(cfg));
            this.kubernetesRole = optString(cfg, "auth.kubernetes.role");
            if (kubernetesRole == null || kubernetesRole.trim().isEmpty())
                {
                kubernetesRole = ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME);
                }
            this.kubernetesMount = optString(cfg, "auth.kubernetes.mount");
            if (kubernetesMount == null || kubernetesMount.trim().isEmpty())
                {
                kubernetesMount = DEFAULT_KUBERNETES_MOUNT;
                }

            synchronized (authLock)
                {
                setClient(newVaultConfig(address.trim(), null));
                authenticate(cfg);
                }
            this.initialized = true;
            }
        catch (Exception e)
            {
            EventRecorder.recordSourceUnavailable(ctx, null, "VAULT_INIT_FAILED",
                    "Vault storage failed to initialize ("+LogSafe.text(address)+"): "+LogSafe.text(e.getMessage()));
            }
        }

    /**
     * an explicitly configured token (builder {@code withToken}, or {@code token} in the bootstrap configuration)
     * is used as given; the Kubernetes login is only the automatic choice when no token is configured.
     */
    private void authenticate(Configuration cfg) throws VaultException
        {
        // 1. Explicit token
        // Security note (low):
        // The token should ideally be a secret, not an immutable String.
        // But the Vault Java client (bettercloud.vault.VaultConfig) only accepts a plain String for .token().
        // Even if we retrieved a SecretValue, we would have to call .asString() for passing.
        // So getString() is used here to avoid unnecessary wrapping.
        String token = (tokenOverride != null) ? tokenOverride : optString(cfg, "token");
        if (token != null)
            {
            if (!isSecureTransport(vaultConfig.getAddress()) && logger != null)
                {
                logger.warn("Vault: the configured token is sent over an unencrypted connection to "+LogSafe.text(vaultConfig.getAddress()));
                }
            setClient(newVaultConfig(vaultConfig.getAddress(), token));
            return;
            }

        // 2. Kubernetes Auth (auto-detected by the ServiceAccount token file)
        // Without either, the driver falls back to the VAULT_TOKEN environment variable (see AllowlistEnvironmentLoader).
        if (Files.isRegularFile(kubernetesTokenPath))
            {
            if (!isSecureTransport(vaultConfig.getAddress()) && !allowInsecureTransport)
                {
                if (logger != null)
                    {
                    logger.info("Vault: Kubernetes login skipped, the address is not https ("+LogSafe.text(vaultConfig.getAddress())+"). "
                            +"Expected when using a local Vault Agent; otherwise set auth.allowInsecureTransport=true (development only).");
                    }
                }
            else
                {
                this.kubernetesAuth = true; // even if this first login fails, a later read retries it
                loginByKubernetes();
                }
            }
        }

    /**
     * logs in with the pod's ServiceAccount token. Caller holds {@code authLock}.
     * The token file is read fresh at every login: projected tokens are rotated by the kubelet.
     *
     * @return true if the login succeeded
     */
    private boolean loginByKubernetes()
        {
        byte[] tokenBytes = null;
        try
            {
            tokenBytes = Files.readAllBytes(kubernetesTokenPath);
            String jwt = new String(tokenBytes, StandardCharsets.UTF_8).trim();
            // log in without the previous client token; it may be the one that expired
            Vault loginClient = new Vault(newVaultConfig(vaultConfig.getAddress(), null));
            AuthResponse response = loginClient.auth().loginByJwt(kubernetesMount, kubernetesRole, jwt);
            setClient(newVaultConfig(vaultConfig.getAddress(), response.getAuthClientToken()));
            if (logger != null)
                {
                logger.info("Authenticated to Vault via Kubernetes (mount: "+LogSafe.text(kubernetesMount)+", role: "+LogSafe.text(kubernetesRole)+")");
                }
            return true;
            }
        catch (Exception e)
            {
            if (logger != null)
                {
                logger.warn("Vault: Kubernetes login failed (mount: "+LogSafe.text(kubernetesMount)+", role: "+LogSafe.text(kubernetesRole)+"): "+LogSafe.text(e.getMessage()));
                }
            return false;
            }
        finally
            {
            if (tokenBytes != null)
                {
                java.util.Arrays.fill(tokenBytes, (byte) 0);
                }
            }
        }

    /**
     * whether the client's token is still accepted ({@code auth/token/lookup-self}, allowed by Vault's default policy).
     * A refused read with a valid token is a policy decision, and a new login would only create another token.
     * If the lookup itself fails (expired token, or no answer), the caller tries a fresh login.
     */
    private boolean isTokenValid(Vault client)
        {
        try
            {
            client.auth().lookupSelf();
            return true;
            }
        catch (Exception e)
            {
            return false;
            }
        }

    /**
     * one fresh Kubernetes login after a read was refused with the given client.
     * If another thread has replaced the client meanwhile, its login is used instead of a second one.
     *
     * @param refusedClient the client whose read was refused
     * @return true if a client from a newer login is available
     */
    private boolean reLogin(Vault refusedClient)
        {
        synchronized (authLock)
            {
            if (vault != refusedClient)
                {
                return true;
                }
            return loginByKubernetes();
            }
        }

    /* caller holds authLock (or is in postInit, before any read) */
    private void setClient(VaultConfig config)
        {
        Vault client = new Vault(config);
        this.vaultConfig = config;
        this.vault = client;
        }

    /**
     * token path: {@code auth.kubernetes.tokenPath}, else an explicitly set {@code KUBERNETES_SA_TOKEN_PATH}
     * (shared with the Kubernetes modules), else the standard ServiceAccount token path.
     */
    private String resolveTokenPath(Configuration cfg)
        {
        String path = optString(cfg, "auth.kubernetes.tokenPath");
        if (path != null && !path.trim().isEmpty())
            {
            return path.trim();
            }
        ConfigFeatureInterface k8sTokenPath = ConfigFeatureRegistry.get(KUBERNETES_SA_TOKEN_PATH_FEATURE);
        ConfigFactorySettings settings = ctx.getSettings();
        if (k8sTokenPath != null && settings.isSet(k8sTokenPath))
            {
            String fromFeature = settings.getString(k8sTokenPath);
            if (fromFeature != null && !fromFeature.trim().isEmpty())
                {
                return fromFeature.trim();
                }
            }
        return DEFAULT_SA_TOKEN_PATH;
        }

    private VaultConfig newVaultConfig(String address, String token) throws VaultException
        {
        VaultConfig config = new VaultConfig().environmentLoader(environmentLoader).address(address);
        // the driver inserts "data/" after the mount for KV v2 reads, and unwraps the v2 response
        config.engineVersion(kvVersion >= 2 ? 2 : 1);
        if (enginePath != null && !enginePath.isEmpty())
            {
            config.prefixPath(enginePath);
            }
        if (token != null)
            {
            config.token(token);
            }
        return config.build();
        }

    static boolean isSecureTransport(String address)
        {
        return address != null && address.trim().toLowerCase(Locale.ROOT).startsWith("https://");
        }

    private static String optString(Configuration cfg, String key)
        {
        try
            {
            return cfg.getString(key);
            }
        catch (ConfigException e) // missing entry, with EXCEPTION_ON_MISSING_ENTRY
            {
            return null;
            }
        }

    private static Boolean optBoolean(Configuration cfg, String key)
        {
        try
            {
            return cfg.getBoolean(key);
            }
        catch (ConfigException e) // missing entry, with EXCEPTION_ON_MISSING_ENTRY
            {
            return null;
            }
        }

    /**
     * controls which environment variables the Vault driver may read on its own.
     * <p>
     * The driver consults the environment in {@code VaultConfig.build()} and {@code SslConfig.build()}.
     * Allowed: {@code VAULT_TOKEN} (environment only; the driver's fallback to the file {@code ~/.vault-token} is not used),
     * {@code VAULT_SSL_CERT}, {@code VAULT_OPEN_TIMEOUT}, {@code VAULT_READ_TIMEOUT}.
     * {@code VAULT_SSL_VERIFY} only with {@code auth.allowInsecureTransport}, since {@code false} disables TLS verification.
     * {@code VAULT_ADDR} is not taken from here: the address is configured through mConfig.
     */
    static final class AllowlistEnvironmentLoader extends EnvironmentLoader
    {
        private final boolean allowInsecureTransport;

        AllowlistEnvironmentLoader(boolean allowInsecureTransport)
            {
            this.allowInsecureTransport = allowInsecureTransport;
            }

        @Override
        public String loadVariable(String name)
            {
            if (!isAllowed(name, allowInsecureTransport))
                {
                return null;
                }
            return System.getenv(name);
            }

        static boolean isAllowed(String name, boolean allowInsecureTransport)
            {
            switch (name)
                {
                case "VAULT_TOKEN":
                case "VAULT_SSL_CERT":
                case "VAULT_OPEN_TIMEOUT":
                case "VAULT_READ_TIMEOUT":
                    return true;
                case "VAULT_SSL_VERIFY":
                    return allowInsecureTransport;
                default:
                    return false;
                }
            }
    }

    @Override
    public void updateConfigurationLayers(String name, ConfigLocation location, LayeredConfigurationInterface layered)
        {
        if (!initialized)
            {
            return;
            }
        Object handle = location.getStorageInstanceHandle();
        if (!(handle instanceof String))
            {
            EventRecorder.recordSourceUnavailable(ctx, location, "VAULT_READ_FAILED",
                    "Vault location without a secret path: '"+LogSafe.text(location.toLocationString())+"'");
            return;
            }
        // KV v2: the driver inserts "data/" after the mount path (see newVaultConfig)
        String fullPath = enginePath+"/"+handle;
        boolean deniedByPolicy = false;
        try
            {
            Vault client = this.vault;
            LogicalResponse response = client.logical().read(fullPath);
            int status = response.getRestResponse().getStatus();
            if (status == HTTP_FORBIDDEN && kubernetesAuth)
                {
                if (isTokenValid(client))
                    {
                    // the token works; the policy does not allow this path. A login would not change that.
                    deniedByPolicy = true;
                    }
                else if (reLogin(client))
                    {
                    // token expired or revoked: one fresh login, one retry
                    response = this.vault.logical().read(fullPath);
                    status = response.getRestResponse().getStatus();
                    }
                }

            if (status == HTTP_OK)
                {
                Map<String, String> data = response.getData();
                Map<String, byte[]> byteData = new HashMap<>();
                for (Map.Entry<String, String> entry : data.entrySet())
                    {
                    if (entry.getValue() != null)
                        {
                        byteData.put(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                layered.add(new VaultConfigLayer(location, byteData), location);
                }
            else if (status == HTTP_NOT_FOUND)
                {
                if (logger != null)
                    {
                    logger.debug("Vault: nothing stored at '"+LogSafe.text(location.toLocationString())+"'");
                    }
                }
            else if (status == HTTP_FORBIDDEN)
                {
                String reason = deniedByPolicy ? "; the token is valid, so the policy does not allow this path"
                        : kubernetesAuth ? ", also after a fresh Kubernetes login"
                        : ". The token may have expired; a static token cannot be renewed by mConfig";
                EventRecorder.recordSourceUnavailable(ctx, location, "VAULT_ACCESS_DENIED",
                        "Vault refused access to '"+LogSafe.text(fullPath)+"' (HTTP 403)"+reason);
                }
            else
                {
                EventRecorder.recordSourceUnavailable(ctx, location, "VAULT_READ_FAILED",
                        "Vault read of '"+LogSafe.text(fullPath)+"' failed (HTTP "+status+")");
                }
            }
        catch (Exception e)
            {
            // Vault is a secret store: a failure while handling a response may quote its content.
            // Report error type and HTTP status only, as for Kubernetes Secrets.
            EventRecorder.recordSourceUnavailable(ctx, location, "VAULT_READ_FAILED",
                    "Vault read of '"+LogSafe.text(fullPath)+"' failed: "+describeReadFailure(e));
            }
        }

    /** error type and, if the client knows it, the HTTP status; never the exception's message. */
    static String describeReadFailure(Exception e)
        {
        String description = e.getClass().getSimpleName();
        if (e instanceof VaultException && ((VaultException) e).getHttpStatusCode() > 0)
            { description += " (HTTP "+((VaultException) e).getHttpStatusCode()+")"; }
        return description;
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object handle)
        {
        if (!initialized || pollInterval <= 0)
            {
            return false;
            }

        // Polling-based change detection
        long now = System.currentTimeMillis();
        if (now - lastCheck > pollInterval)
            {
            lastCheck = now;
            return true; // Trigger re-read
            }
        return false;
        }

    @Override
    public void triggerChangeCheck(Object handle)
        {
        // Polling doesn't need explicit triggers, but we could force a poll
        lastCheck = 0;
        }

    @Override
    public ConfigLayerInterface createConfigurationLayer(String configName, ConfigLocation location, ConfigSchema configSchema, LayeredConfiguration layeredConfiguration)
        {
        return null;
        }

    @Override
    public void updateBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        }

    @Override
    public Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        return Collections.emptySet();
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
        }

    @Override
    public URI getURIforConfigLocation(ConfigLocation loc, String k, String f)
        {
        return URI.create("vault://" + loc.toLocationString() + "/" + k);
        }

    @Override
    public void exit()
        {
        }

    /* a source built with VaultConfigSource.builder() brings its own instance (own address, token) */
    @Override
    @SuppressWarnings("removal") // stopgap until MLIB-155
    public boolean isInitializedWhenAddedAsSource()
        {
        return true;
        }

    public void setAddressOverride(String addressOverride)
        {
        this.addressOverride = addressOverride;
        }

    public void setTokenOverride(String tokenOverride)
        {
        this.tokenOverride = tokenOverride;
        }

    // for tests
    boolean isInitialized()
        {
        return initialized;
        }

    // for tests
    String getClientToken()
        {
        return (vaultConfig == null) ? null : vaultConfig.getToken();
        }

    private static class VaultConfigLayer implements ConfigLayerInterface
    {
        private final ConfigLocation                location;
        private final Map<String, byte[]>           data;

        public VaultConfigLayer(ConfigLocation location, Map<String, byte[]> data)
            {
            this.location = location;
            this.data = data != null ? new HashMap<>(data) : Collections.emptyMap();
            }

        @Override
        public ConfigEntry getEntry(String hierarchicalKey)
            {
            byte[] value = data.get(hierarchicalKey);
            if (value == null)
                {
                return null;
                }

            SecretValue secretValue = new BasicSecretValue(value, SecretType.PLAIN_TEXT);
            return new SecretConfigEntryLeaf(hierarchicalKey, secretValue, new ConfigEntryMetadata(getSource()));
            }

        @Override
        public void erase()
            {
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
        public boolean isWriteable()
            {
            return false;
            }

        @Override
        public boolean isEmpty()
            {
            return data.isEmpty();
            }

        @Override
        public ConfigScope getScope()
            {
            return location.getScope();
            }

        @Override
        public void writeEntry(ConfigEntry entryToWrite)
            {
            throw new UnsupportedOperationException("Vault layer is read-only");
            }

        @Override
        public int flush()
            {
            return 0;
            }

        @Override
        public ConfigSource getSource()
            {
            if (location instanceof ConfigSource)
                {
                return (ConfigSource) location;
                }
            return null;
            }

        @Override
        public int compareTo(ConfigLayerInterface o)
            {
            return getScope().compareTo(o.getScope());
            }

        @Override
        public Iterator<String> tryToGetKeyIterator()
            {
            return data.keySet().iterator();
            }
    }
}
//___EOF___
