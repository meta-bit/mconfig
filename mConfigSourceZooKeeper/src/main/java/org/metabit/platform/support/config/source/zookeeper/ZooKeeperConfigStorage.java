package org.metabit.platform.support.config.source.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

    /**
     * Experimental ZooKeeper configuration storage.
     * 
     * FUTURE EXECUTION PLAN:
     * This storage currently uses a BLOB-based approach (one ZNode = one full config file).
     * A plan for transitioning to a Tree-based design (hierarchical ZNodes = hierarchical keys)
     * is documented in 'planned_tree_design.md' in this module's root.
     */
public class ZooKeeperConfigStorage implements ConfigStorageInterface
{
    private ConfigLoggingInterface logger;
    private CuratorFramework client;
    private String rootPath;
    private boolean initialized = false;
    private final Map<String, CuratorCache> caches = new ConcurrentHashMap<>();
    private final Set<String> changedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public String getStorageName()
    {
        return "ZooKeeper";
    }

    @Override
    public String getStorageID()
    {
        return "zookeeper";
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
        ConfigFactorySettings settings = ctx.getSettings();

        String bootstrapName = settings.getString(ConfigFeature.ZOOKEEPER_BOOTSTRAP_CONFIG_NAME);
        Configuration bootstrapConfig = null;
        try
        {
            bootstrapConfig = ctx.getFactory().getConfig(bootstrapName);
        }
        catch (Exception e)
        {
            logger.info("ZooKeeper bootstrap config '" + bootstrapName + "' not found, skipping ZooKeeper storage activation.");
            return false;
        }

        String connectString = null;
        try {
            connectString = bootstrapConfig.getString("zookeeper/connectString");
        } catch (Exception e) {
            // ignore
        }
        if (connectString == null)
        {
            connectString = settings.getString(ConfigFeature.ZOOKEEPER_CONNECT_STRING);
        }

        if (connectString == null || connectString.isEmpty())
        {
            logger.debug("ZooKeeper connect string not found, ZooKeeper storage remains inactive.");
            return false;
        }

        this.rootPath = null;
        if (bootstrapConfig != null)
        {
            try
            {
                this.rootPath = bootstrapConfig.getString("zookeeper/rootPath");
            }
            catch (Exception e)
            {
                // ignore
            }
        }
        if (this.rootPath == null)
        {
            this.rootPath = settings.getString(ConfigFeature.ZOOKEEPER_ROOT_PATH);
        }

        Integer sessionTimeout = null;
        if (bootstrapConfig != null)
        {
            try
            {
                sessionTimeout = bootstrapConfig.getInteger("zookeeper/sessionTimeoutMs");
            }
            catch (Exception e)
            {
                // ignore
            }
        }
        if (sessionTimeout == null)
        {
            sessionTimeout = settings.getInteger(ConfigFeature.ZOOKEEPER_SESSION_TIMEOUT_MS);
        }

        Integer retryBaseSleepMs = null;
        if (bootstrapConfig != null)
        {
            try
            {
                retryBaseSleepMs = bootstrapConfig.getInteger("zookeeper/retryBaseSleepMs");
            }
            catch (Exception e)
            {
                // ignore
            }
        }
        if (retryBaseSleepMs == null)
        {
            retryBaseSleepMs = settings.getInteger(ConfigFeature.ZOOKEEPER_RETRY_BASE_SLEEP_MS);
        }

        Integer retryMaxRetries = null;
        if (bootstrapConfig != null)
        {
            try
            {
                retryMaxRetries = bootstrapConfig.getInteger("zookeeper/retryMaxRetries");
            }
            catch (Exception e)
            {
                // ignore
            }
        }
        if (retryMaxRetries == null)
        {
            retryMaxRetries = settings.getInteger(ConfigFeature.ZOOKEEPER_RETRY_MAX_RETRIES);
        }

        try
        {
            this.client = CuratorFrameworkFactory.builder()
                    .connectString(connectString)
                    .sessionTimeoutMs(sessionTimeout)
                    .retryPolicy(new ExponentialBackoffRetry(retryBaseSleepMs, retryMaxRetries))
                    .build();
            this.client.start();
            this.initialized = true;
            logger.info("ZooKeeper storage activated, connected to " + connectString);

            // Register default locations for CLUSTER and ORGANIZATION scopes
            ctx.getSearchList().insertAtScopeEnd(new ZooKeeperConfigLocation(this, ConfigScope.CLUSTER, rootPath + "/cluster"), ConfigScope.CLUSTER);
            ctx.getSearchList().insertAtScopeEnd(new ZooKeeperConfigLocation(this, ConfigScope.ORGANIZATION, rootPath + "/organization"), ConfigScope.ORGANIZATION);

            return true;
        }
        catch (Exception e)
        {
            logger.error("Failed to initialize ZooKeeper client", e);
            return false;
        }
    }

    @Override
    public void exit()
    {
        for (CuratorCache cache : caches.values())
        {
            cache.close();
        }
        caches.clear();
        if (client != null)
        {
            client.close();
        }
    }

    @Override
    public boolean isGenerallyWriteable()
    {
        return true;
    }

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
    {
        return URI.create("zookeeper://" + configLocation.toLocationString() + "/" + key);
    }

    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation location, LayeredConfigurationInterface layeredCfg)
    {
        if (!initialized) return;

        String path = location.toLocationString() + "/" + sanitizedConfigName;
        
        // Ensure we have a cache/watcher for this path if we're in SERVICE mode (implied by checking for updates)
        ensureWatcher(path);

        try
        {
            byte[] data = null;
            CuratorCache cache = caches.get(path);
            if (cache != null)
            {
                Optional<ChildData> childData = cache.get(path);
                if (childData.isPresent())
                {
                    data = childData.get().getData();
                }
            }
            
            if (data == null) // Fallback to direct read if not in cache yet
            {
                if (client.checkExists().forPath(path) != null)
                {
                    data = client.getData().forPath(path);
                }
            }

            if (data != null && data.length > 0)
            {
                // **format detection heuristics**
                // For now, we assume JSON if it starts with { or [, otherwise Properties.
                // Ideally, we'd check all formats, but this is a simplified experimental version.
                String content = new String(data, StandardCharsets.UTF_8).trim();
                ConfigFormatInterface format;
                if (content.startsWith("{") || content.startsWith("["))
                {
                    format = layeredCfg.getContext().getConfigFormats().get("JSON");
                    if (format == null) format = layeredCfg.getContext().getConfigFormats().get("JSONwithJackson");
                    if (format == null) format = layeredCfg.getContext().getConfigFormats().get("json");
                }
                else
                {
                    format = layeredCfg.getContext().getConfigFormats().get("properties");
                    if (format == null) format = layeredCfg.getContext().getConfigFormats().get("PROPERTIES");
                }

                if (format instanceof ConfigFileFormatInterface)
                {
                    // Use the znode path as the storage instance handle, so we can track changes properly
                    ConfigLayerInterface layer = ((ConfigFileFormatInterface) format).readStream(
                            new java.io.ByteArrayInputStream(data), 
                            new ZooKeeperConfigSource(location, (ConfigFormatInterface) format, path)
                    );
                    if (layer != null)
                    {
                        layeredCfg.add(layer, layer.getSource());
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error reading from ZooKeeper at " + path, e);
        }
    }

    /**
     * Check if the configuration has changed since the last check.
     * 
     * LESSONS LEARNED (Real-time updates):
     * 1. Handle Mapping: mConfig passes different types of handles depending on where the check is triggered.
     *    - Location handles (String path) for top-level search list checks.
     *    - Stream handles (InputStream) when checking specific instantiated layers.
     * 2. Re-read Triggering: Returning 'true' here causes mConfig to call tryToReadConfigurationLayers again.
     * 3. Cache Consistency: After a change is detected, we must clear the local change tracking to avoid 
     *    infinite update loops, while ensuring the re-read gets the latest data from ZooKeeper.
     */
    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
    {
        String path = null;
        if (storageInstanceHandle instanceof String)
        {
            path = (String) storageInstanceHandle;
        }
        else if (storageInstanceHandle instanceof java.io.InputStream)
        {
            // Fallback for stream-based handles: if ANY watched path in this storage changed, 
            // signal a potential change to trigger re-evaluation of the layer.
            boolean anyChanged = !changedPaths.isEmpty();
            if (anyChanged) {
                changedPaths.clear(); // Prevent infinite re-read loops
                logger.info("CHANGE detected in ZooKeeper (stream handle matched)");
                return true;
            }
            return false;
        }

        if (path != null)
        {
            // Exact path match
            boolean changed = changedPaths.remove(path);
            
            // Directory/Prefix match (if handle is a location root)
            if (!changed)
            {
                Iterator<String> it = changedPaths.iterator();
                while (it.hasNext())
                {
                    String changedPath = it.next();
                    if (changedPath.startsWith(path))
                    {
                        it.remove();
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                logger.info("CHANGE detected in ZooKeeper path " + path);
            }
            return changed;
        }
        return false;
    }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
    {
        if (storageInstanceHandle instanceof String)
        {
            changedPaths.add((String) storageInstanceHandle);
        }
    }

    private void ensureWatcher(String path)
    {
        caches.computeIfAbsent(path, p -> {
            logger.debug("Creating ZooKeeper watcher for " + p);
            CuratorCache cache = CuratorCache.build(client, p, CuratorCache.Options.SINGLE_NODE_CACHE);
            cache.listenable().addListener(CuratorCacheListener.builder()
                    .forChanges((oldNode, newNode) -> {
                        logger.debug("ZooKeeper node changed: " + p);
                        changedPaths.add(p);
                    })
                    .forCreates(node -> {
                        logger.debug("ZooKeeper node created: " + p);
                        changedPaths.add(p);
                    })
                    .forDeletes(node -> {
                        logger.debug("ZooKeeper node deleted: " + p);
                        changedPaths.add(p);
                    })
                    .build());
            cache.start();
            return cache;
        });
    }

    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
    {
        return null; // Writing not yet fully implemented in this experimental version
    }

    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
    {
        // Not implemented for ZK yet
    }

    @Override
    public Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
    {
        if (!initialized) return Collections.emptySet();
        String path = location.toLocationString();
        try
        {
            if (client.checkExists().forPath(path) != null)
            {
                List<String> children = client.getChildren().forPath(path);
                Set<ConfigDiscoveryInfo> result = new HashSet<>();
                for (String child : children)
                {
                    result.add(new ConfigDiscoveryInfo(child, location.getScope(), getURIforConfigLocation(location, child, null), "unknown", true));
                }
                return result;
            }
        }
        catch (Exception e)
        {
            logger.error("Error listing ZooKeeper configurations at " + path, e);
        }
        return Collections.emptySet();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
