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

/**
 * Builder for VaultConfigSource.
 */
public final class VaultConfigSourceBuilder
{
    private ConfigScope scope = ConfigScope.CLOUD;
    private String      vaultPath;
    private String      address;
    private String      token;

    public VaultConfigSourceBuilder withScope(ConfigScope scope)
        {
        this.scope = scope;
        return this;
        }

    public VaultConfigSourceBuilder withVaultPath(String vaultPath)
        {
        this.vaultPath = vaultPath;
        return this;
        }

    public VaultConfigSourceBuilder withAddress(String address)
        {
        this.address = address;
        return this;
        }

    public VaultConfigSourceBuilder withToken(String token)
        {
        this.token = token;
        return this;
        }

    public VaultConfigSource build()
        {
        VaultConfigStorage storage = new VaultConfigStorage();
        if (address != null)
            {
            storage.setAddressOverride(address);
            }
        if (token != null)
            {
            storage.setTokenOverride(token);
            }
        // The address and token here are for initial setup if not using self-config
        // but typically mConfig sources are registered with a handle.
        return new VaultConfigSource(scope, storage, null, vaultPath);
        }
}
