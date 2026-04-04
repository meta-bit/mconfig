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
module org.metabit.platform.support.config.source.vault {
    requires metabit.mconfig.core;
    requires metabit.mconfig.secrets;
    requires vault.java.driver;
    
    exports org.metabit.platform.support.config.source.vault;

    provides org.metabit.platform.support.config.interfaces.ConfigStorageInterface 
        with org.metabit.platform.support.config.source.vault.VaultConfigStorage;
}
