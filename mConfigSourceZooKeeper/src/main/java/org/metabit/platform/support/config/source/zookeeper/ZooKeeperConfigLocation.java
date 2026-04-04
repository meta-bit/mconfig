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

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

/**
 * ZooKeeper specific ConfigLocation.
 */
public class ZooKeeperConfigLocation extends ConfigLocationImpl
{
    private final String znodePath;

    public ZooKeeperConfigLocation(ConfigStorageInterface storage, ConfigScope scope, String znodePath)
    {
        super(scope, storage, null, znodePath);
        this.znodePath = znodePath;
    }

    @Override
    public String toLocationString()
    {
        return znodePath;
    }
}
