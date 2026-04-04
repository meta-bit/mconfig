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

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureBase;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureRegistry;

/**
 * Configuration features for the AWS Secrets Manager configuration source.
 */
public final class AwsFeatures
{
    private AwsFeatures() {}

    /**
     * The configuration name used to bootstrap AWS Secrets Manager settings.
     */
    public static final ConfigFeatureInterface BOOTSTRAP_CONFIG_NAME = new ConfigFeatureBase("AWS_BOOTSTRAP_CONFIG_NAME", ConfigFeature.ValueType.STRING, "aws");

    static
    {
        ConfigFeatureRegistry.register(BOOTSTRAP_CONFIG_NAME);
    }
}
