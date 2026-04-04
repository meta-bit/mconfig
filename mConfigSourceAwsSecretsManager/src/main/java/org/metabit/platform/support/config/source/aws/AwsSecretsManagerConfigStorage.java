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
package org.metabit.platform.support.config.source.aws;

import org.metabit.library.format.json.JsonStreamParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
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
import java.util.*;

/**
 * AWS Secrets Manager implementation for mConfig.
 */
public final class AwsSecretsManagerConfigStorage implements ConfigStorageInterface
{
    private SecretsManagerClient           client;
    private ConfigLoggingInterface         logger;
    private ConfigFactoryInstanceContext   ctx;
    private long                   lastCheck    = 0;
    private long                   pollInterval;
    private boolean                initialized  = false;
    private String                 regionOverride;

    @Override
    public String getStorageName()
        {
        return "AWS Secrets Manager";
        }

    @Override
    public String getStorageID()
        {
        return "aws-secrets";
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
        String bootstrapName = ctx.getSettings().getString(AwsFeatures.BOOTSTRAP_CONFIG_NAME);
        Configuration cfg = factory.getConfig(bootstrapName);
        try
            {
            String region = (regionOverride != null) ? regionOverride : cfg.getString("region");
            if (region != null)
                {
                this.client = SecretsManagerClient.builder()
                        .region(Region.of(region))
                        .build();
                }
            else
                {
                this.client = SecretsManagerClient.builder().build();
                }
            
            this.pollInterval = cfg.getLong("pollInterval");
            this.initialized = true;
            }
        catch (Exception e)
        {
            if (logger != null)
            {
                logger.warn("AWS Secrets Manager client failed to start: " + e.getMessage());
            }
        }
        }

    @Override
    public void updateConfigurationLayers(String name, ConfigLocation location, LayeredConfigurationInterface layered)
        {
        if (!initialized || client == null)
            {
            return;
            }

        String secretId = (String) location.getStorageInstanceHandle();
        try
            {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretId)
                    .build();
            GetSecretValueResponse response = client.getSecretValue(request);

            Map<String, String> data = null;
            String secretString = response.secretString();
            if (secretString != null)
                {
                if (secretString.trim().startsWith("{"))
                    {
                    data = parseJson(secretString);
                    }
                else
                    {
                    data = new HashMap<>();
                    data.put("value", secretString);
                    }
                }
            else if (response.secretBinary() != null)
                {
                data = new HashMap<>();
                data.put("value", Base64.getEncoder().encodeToString(response.secretBinary().asByteArray()));
                }

            if (data != null)
                {
                layered.add(new AwsSecretsManagerConfigLayer(location, data), location);
                }
            }
        catch (Exception e)
            {
            if (logger != null)
                {
                logger.debug("AWS Secret '" + secretId + "' could not be read: " + e.getMessage());
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

        // AWS polling
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
        return URI.create("aws-secrets://" + loc.toLocationString() + "/" + k);
        }

    @Override
    public void exit()
        {
        if (client != null)
            {
            client.close();
            }
        }

    public void setRegionOverride(String regionOverride)
        {
        this.regionOverride = regionOverride;
        }

    private Map<String, String> parseJson(String json)
        {
        Map<String, String> data = new HashMap<>();
        try
            {
            JsonStreamParser.parseJson(json, new JsonStreamParser.JsonStreamConsumer()
            {
                private String currentKey;

                @Override public void consumeObjectEntryStart(int line, int column, int level, String key) { currentKey = key; }
                @Override public void consumeObjectEntryEnd(int line, int column, int level) { currentKey = null; }
                @Override public void consumeString(int line, int column, int level, String string) { if (currentKey != null) data.put(currentKey, string); }
                @Override public void consumeNumberInteger(int line, int column, int level, int i) { if (currentKey != null) data.put(currentKey, String.valueOf(i)); }
                @Override public void consumeNumberLong(int line, int column, int level, long l) { if (currentKey != null) data.put(currentKey, String.valueOf(l)); }
                @Override public void consumeNumberDouble(int line, int column, int level, double v) { if (currentKey != null) data.put(currentKey, String.valueOf(v)); }
                @Override public void consumeTrue(int line, int column, int level) { if (currentKey != null) data.put(currentKey, "true"); }
                @Override public void consumeFalse(int line, int column, int level) { if (currentKey != null) data.put(currentKey, "false"); }
                @Override public void consumeNull(int line, int column, int level) { if (currentKey != null) data.put(currentKey, null); }
                @Override public void consumeNumberBigInteger(int line, int column, int level, java.math.BigInteger bigInteger) { if (currentKey != null) data.put(currentKey, bigInteger.toString()); }
                @Override public void consumeNumberBigDecimal(int line, int column, int level, java.math.BigDecimal bigDecimal) { if (currentKey != null) data.put(currentKey, bigDecimal.toString()); }
                @Override public void consumeObjectStart(int line, int column, int level) {}
                @Override public void consumeObjectEnd(int line, int column, int level) {}
                @Override public void consumeArrayStart(int line, int column, int level) {}
                @Override public void consumeArrayEnd(int line, int column, int level) {}
            });
            }
        catch (Exception e)
            {
            // Fallback to raw string if parsing fails or is not a flat object
            if (data.isEmpty())
                {
                data.put("value", json);
                }
            }
        return data;
        }

    private static class AwsSecretsManagerConfigLayer implements ConfigLayerInterface
    {
        private final ConfigLocation      location;
        private final Map<String, String> data;

        public AwsSecretsManagerConfigLayer(ConfigLocation location, Map<String, String> data)
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
            throw new UnsupportedOperationException("AWS layer is read-only");
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
