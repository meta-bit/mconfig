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
package org.metabit.platform.support.config.source.zookeeper;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureBase;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureRegistry;

/**
 * Configuration features for the ZooKeeper configuration source.
 */
public final class ZooKeeperFeatures
{
    private ZooKeeperFeatures() {}

    /**
     * ZooKeeper connection string.
     */
    public static final ConfigFeatureInterface CONNECT_STRING = new ConfigFeatureBase("ZOOKEEPER_CONNECT_STRING", ConfigFeature.ValueType.STRING);

    /**
     * ZooKeeper root path for mConfig.
     */
    public static final ConfigFeatureInterface ROOT_PATH = new ConfigFeatureBase("ZOOKEEPER_ROOT_PATH", ConfigFeature.ValueType.STRING, "/mconfig");

    /**
     * ZooKeeper session timeout in milliseconds.
     */
    public static final ConfigFeatureInterface SESSION_TIMEOUT_MS = new ConfigFeatureBase("ZOOKEEPER_SESSION_TIMEOUT_MS", ConfigFeature.ValueType.NUMBER, Integer.class, 60000);

    /**
     * ZooKeeper retry policy base sleep time in milliseconds.
     */
    public static final ConfigFeatureInterface RETRY_BASE_SLEEP_MS = new ConfigFeatureBase("ZOOKEEPER_RETRY_BASE_SLEEP_MS", ConfigFeature.ValueType.NUMBER, Integer.class, 1000);

    /**
     * ZooKeeper retry policy maximum number of retries.
     */
    public static final ConfigFeatureInterface RETRY_MAX_RETRIES = new ConfigFeatureBase("ZOOKEEPER_RETRY_MAX_RETRIES", ConfigFeature.ValueType.NUMBER, Integer.class, 3);

    /**
     * The configuration name used to bootstrap ZooKeeper settings (indirectly).
     */
    public static final ConfigFeatureInterface BOOTSTRAP_CONFIG_NAME = new ConfigFeatureBase("ZOOKEEPER_BOOTSTRAP_CONFIG_NAME", ConfigFeature.ValueType.STRING, "zookeeper");

    static
    {
        ConfigFeatureRegistry.register(CONNECT_STRING);
        ConfigFeatureRegistry.register(ROOT_PATH);
        ConfigFeatureRegistry.register(SESSION_TIMEOUT_MS);
        ConfigFeatureRegistry.register(RETRY_BASE_SLEEP_MS);
        ConfigFeatureRegistry.register(RETRY_MAX_RETRIES);
        ConfigFeatureRegistry.register(BOOTSTRAP_CONFIG_NAME);
    }
}
