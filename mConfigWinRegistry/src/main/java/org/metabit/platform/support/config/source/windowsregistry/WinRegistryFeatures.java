package org.metabit.platform.support.config.source.windowsregistry;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureBase;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureRegistry;

/**
 * Configuration features for the Windows Registry configuration source.
 */
public final class WinRegistryFeatures
{
    private WinRegistryFeatures() {}

    /**
     * when using Windows Registry as config source, this is the root path to use.
     * if not set, it defaults to Software\&lt;companyName&gt;\&lt;applicationName&gt;\.
     * the root path is appended to the Hive (e.g. HKEY_CURRENT_USER).
     */
    public static final ConfigFeatureInterface REGISTRY_BASE_PATH = new ConfigFeatureBase("REGISTRY_BASE_PATH", ConfigFeature.ValueType.STRING);

    static
    {
        ConfigFeatureRegistry.register(REGISTRY_BASE_PATH);
    }
}
