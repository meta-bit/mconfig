/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.net.URI;

/**
 * interface for accessing a type of config storage adapter.
 * <p/>
 * In most cases, it is not the storage itself or a representation thereof,
 * but an adapter to the specific type of storage. It unifies storage access+API.
 * <p/>
 * A ConfigStorage is stateful, may keep network connections, cached files,
 * and so on.
 *
 * <p>
 * Created by jw on 2019-12-28.
 * <p>
 * see old ConfigStorageAdapter (not ConfigStore)
 * rename to ConfigSource, ConfigRepository or something?
 * <p>
 * There's two things:
 * - the general storage adapter, which provides technical means to access a configuration storage (e.g. files, network). interface = this.
 * - the specific store, which is referring to an instance within. This is referred to by a ConfigStoreHandle. The type is local to the ConfigStore instance.
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigStorageInterface extends Cloneable
{
    /*
     * get the name. The name is mostly for human consumption.
     * this should be static, but Java doesn't allow static members on interfacing.
     *
     * @return the storage name (human use)
     */
    /**
     * <p>getStorageName.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getStorageName();

    /*
     * get the ID for technical use (settings, module matching)
     * this should be static, but Java doesn't allow static members on interfacing.
     *
     * @return the storage ID (programmatic use; only regular identifier characters allowed,
     * case-insensitive matching
     */
    /**
     * <p>getStorageID.</p>
     *
     * @return a {@link java.lang.String} object
     */
    String getStorageID();

    /*
     * initialize a storage instance for use.
     *
     * @return true if successful and the config source is usable, false if there was a problem and it should not be used.
     */
    /**
     * <p>test.</p>
     *
     * @param settings a {@link org.metabit.platform.support.config.impl.ConfigFactorySettings} object
     * @param logger a {@link org.metabit.platform.support.config.interfaces.ConfigLoggingInterface} object
     * @return a boolean
     */
    boolean test(final ConfigFactorySettings settings, ConfigLoggingInterface logger);

    /*
     * internal initialization; mandatory
     * @param ctx context to use
     * @return if init failed - don't use if this was false.
     */
    /**
     * <p>init.</p>
     *
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     * @return a boolean
     */
    boolean init(ConfigFactoryInstanceContext ctx);

    /**
     * cleanup after use / close access handles.
     * This should not throw exceptions; if possible, perform all cleanup quietly.
     * exit, clean up and free handles after use.
     */
    void exit();

    /**
     * has this storage the possibility to be written to at all?
     * it still depends on the individual Configuration, but some storage locations are not writeable at all.
     *
     * @return true if the chance exists that configs may be written; false if no attempt need to be made.
     */
    boolean isGenerallyWriteable();

    /**
     * get an URI for a ConfigLocation within the ConfigStorage
     *
     * @param configLocation   the location to use
     * @param key the key~name of the entry
     * @param optionalFragment an optional fragment you may want to include in the URI
     * @return the URI generated
     */
    URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment); // with specifics.

    /*
    When building a LayeredConfiguration, we need to collect all possible layers.
    A single ConfigSource may add several such layers.
    Returning temporary hierarchical maps just to combine them (plus ordering logic) is overhead without additional use.
    Instead, we allow the ConfigStorage to write its respective findings to the LayeredConfiguration
     */
    /**
     * <p>tryToReadConfigurationLayers.</p>
     *
     * @param sanitizedConfigName a {@link java.lang.String} object
     * @param possibleSource a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @param layeredCfg a {@link org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface} object
     */
    void tryToReadConfigurationLayers(final String sanitizedConfigName, final ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg);

    /**
     * Optional: provide additional layers for a configuration.
     * This can be used by specialized storages like secrets to add their layers.
     * @param sanitizedConfigName name of the configuration
     * @param layeredCfg the layered configuration being built
     */
    default void provideAdditionalLayers(String sanitizedConfigName, LayeredConfigurationInterface layeredCfg) {}

    /**
     * <p>tryToCreateConfiguration.</p>
     *
     * @param configName a {@link java.lang.String} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @param configScheme a {@link org.metabit.platform.support.config.scheme.ConfigScheme} object
     * @param layeredConfiguration a {@link org.metabit.platform.support.config.impl.LayeredConfiguration} object
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     */
    ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration);

    /**
     *
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param location location to look in
     * @param blobConfig the blob config we're working on
     */
    void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig);


    /**
     * list all available configurations in the given location.
     *
     * @param location the location to search in
     * @return a set of configuration discovery info found.
     */
    java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(org.metabit.platform.support.config.ConfigLocation location);

    /**
     * <p>hasChangedSincePreviousCheck.</p>
     *
     * @param storageInstanceHandle a {@link java.lang.Object} object
     * @return a boolean
     */
    boolean hasChangedSincePreviousCheck(Object storageInstanceHandle);

    /**
     * Trigger a check for changes, or signal that a change has occurred.
     *
     * @param storageInstanceHandle the handle for the storage instance
     */
    void triggerChangeCheck(Object storageInstanceHandle);

    // writing:
    // Layer writes delegated to ConfigLayerInterface impls via writeEntry()
    // (coordinated by LayeredConfiguration).
}
//___EOF___
