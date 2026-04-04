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

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Implementation of Vault storage for mConfig.
 */
public final class VaultConfigStorage implements ConfigStorageInterface
{
    public static final int HTTP_OK = 200;
    private VaultConfig vaultConfig;
    private ConfigLoggingInterface         logger;
    private ConfigFactoryInstanceContext   ctx;
    private Vault                          vault;
    private String                 enginePath;
    private int                    kvVersion;
    private long                   lastCheck    = 0;
    private long                   pollInterval;
    private boolean                initialized  = false;
    private String                 addressOverride;
    private String                 tokenOverride;

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
    public void postInit(ConfigFactory factory)
        {
        // Self-configuration from lower-precedence parts of mConfig
        String bootstrapName = ctx.getSettings().getString(VaultFeatures.BOOTSTRAP_CONFIG_NAME);
        Configuration cfg = factory.getConfig(bootstrapName);
        try
            {
            String address = (addressOverride != null) ? addressOverride : cfg.getString("address");
            
            this.enginePath = cfg.getString("engine");
            this.kvVersion = cfg.getInteger("kvVersion");
            this.pollInterval = cfg.getLong("pollInterval");

            this.vaultConfig = new VaultConfig()
                    .address(address)
                    .build();

            this.vault = new Vault(vaultConfig);
            authenticate(cfg);
            this.initialized = true;
            }
        catch (Exception e)
            {
            if (logger != null)
                {
                logger.warn("Failed to initialize Vault storage: " + e.getMessage());
                }
            }
        }

    private void authenticate(Configuration cfg) throws VaultException
        {
        // 1. Kubernetes Auth (Auto-detect)
        String saTokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        if (Files.exists(Paths.get(saTokenPath)))
            {
            try
                {
                String token = new String(Files.readAllBytes(Paths.get(saTokenPath)), StandardCharsets.UTF_8);
                String role = cfg.getString("auth.kubernetes.role");
                String mount = cfg.getString("auth.kubernetes.mount");
                
                // Note: bettercloud vault-java-driver 5.1.0's loginByKubernetes(role, jwt) 
                // uses the default "kubernetes" mount path. If a custom mount is needed,
                // it would require manual implementation via logical().write("auth/" + mount + "/login", ...).
                AuthResponse response = vault.auth().loginByKubernetes(role, token);
                this.vaultConfig = new VaultConfig().address(vaultConfig.getAddress()).token(response.getAuthClientToken()).build();
                this.vault = new Vault(vaultConfig);
                if (logger != null)
                    {
                    logger.info("Authenticated to Vault via Kubernetes (role: " + role + ")");
                    }
                return;
                }
            catch (Exception e)
                {
                if (logger != null)
                    {
                    logger.debug("Kubernetes auth failed: " + e.getMessage());
                    }
                }
            }

        // 2. Token Fallback
        String token = (tokenOverride != null) ? tokenOverride : cfg.getString("token");
        if (token != null)
            {
            this.vaultConfig = new VaultConfig().address(vaultConfig.getAddress()).token(token).build();
            this.vault = new Vault(vaultConfig);
            }
        }

    @Override
    public void updateConfigurationLayers(String name, ConfigLocation location, LayeredConfigurationInterface layered)
        {
        if (!initialized)
            {
            return;
            }
        try
            {
            String path = (String) location.getStorageInstanceHandle();
            String fullPath = (kvVersion >= 2) ? enginePath + "/data/" + path : enginePath + "/" + path;
            LogicalResponse response = vault.logical().read(fullPath);

            if (response.getRestResponse().getStatus() == HTTP_OK)
                {
                Map<String, String> data = response.getData();
                layered.add(new VaultConfigLayer(location, data), location);
                }
            }
        catch (Exception e)
            {
            if (logger != null)
                {
                logger.debug("Could not read from Vault path '" + location.toLocationString() + "': " + e.getMessage());
                }
            }
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

    public void setAddressOverride(String addressOverride)
        {
        this.addressOverride = addressOverride;
        }

    public void setTokenOverride(String tokenOverride)
        {
        this.tokenOverride = tokenOverride;
        }

    private static class VaultConfigLayer implements ConfigLayerInterface
    {
        private final ConfigLocation                location;
        private final Map<String, String>           data;

        public VaultConfigLayer(ConfigLocation location, Map<String, String> data)
            {
            this.location = location;
            this.data = data != null ? new HashMap<>(data) : Collections.emptyMap();
            }

        @Override
        public ConfigEntry getEntry(String hierarchicalKey)
            {
            String value = data.get(hierarchicalKey);
            if (value == null)
                {
                return null;
                }

            SecretValue secretValue = new BasicSecretValue(value.getBytes(StandardCharsets.UTF_8), SecretType.PLAIN_TEXT);
            return new SecretConfigEntryLeaf(hierarchicalKey, secretValue, location);
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
