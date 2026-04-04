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

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

/**
 * mConfig source for HashiCorp Vault.
 * Default scope: CLOUD.
 */
public final class VaultConfigSource extends ConfigLocationImpl
{
    public VaultConfigSource(ConfigScope scope, ConfigStorageInterface storage, ConfigFormatInterface format, Object handle)
    {
        super(scope, storage, format, handle);
    }

    public static VaultConfigSourceBuilder builder()
    {
        return new VaultConfigSourceBuilder();
    }
}
