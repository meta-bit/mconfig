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

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.testcontainers.vault.VaultContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class VaultSourceIT
{
    @Container
    public static VaultContainer<?> vault = new VaultContainer<>("hashicorp/vault:1.13.1")
            .withVaultToken("test-token")
            .withInitCommand("kv put secret/test-config key=secret-value");

    @Test
    void testVaultResolution()
        {
        ConfigFactory factory = ConfigFactoryBuilder.create("test", "app")
                .addSource(VaultConfigSource.builder()
                        .withAddress(vault.getHttpHostAddress())
                        .withToken("test-token")
                        .withVaultPath("test-config")
                        .build())
                .build();

        Configuration config = factory.getConfig("test-config");
        SecretValue value = config.getSecret("key");

        assertEquals("secret-value", new String(value.getValue()));
        assertTrue(config.toString().contains("[REDACTED]"));
        }
}
