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
import org.metabit.platform.support.config.schema.ConfigSchema;

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
 * There's two things:
 * - the general storage adapter, which provides technical means to access a configuration storage (e.g. files, network). interface = this.
 * - the specific place within that storage, described by a {@link ConfigLocation}; the storage-specific part is its
 *   storage instance handle ({@link ConfigLocation#getStorageInstanceHandle()}). The handle type is local to the storage implementation.
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
     * @return true if successful and the storage is usable, false if there was a problem and it should not be used.
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
     * Second-phase initialization hook called after the full {@link org.metabit.platform.support.config.ConfigFactory}
     * and all scopes/layers are assembled.
     *
     * <p>This is the recommended place for remote or secret-manager storages to perform
     * self-configuration, establish network connections, authenticate, and start
     * watchers or pollers.</p>
     *
     * <p>The {@link BootstrapConfigContext} intentionally limits access to
     * {@link BootstrapConfigContext#getRestrictedConfig(String, java.util.EnumSet)} to prevent
     * circular references: use a scope set containing only low-precedence local scopes
     * (e.g. {@code PRODUCT}, {@code HOST}, {@code USER}, {@code APPLICATION}).</p>
     *
     * @param bootstrapCtx restricted context for reading bootstrap configuration safely
     */
    default void postInit(final BootstrapConfigContext bootstrapCtx)
        {
        }

    /**
     * whether an instance of this storage, created by the application and passed in with
     * {@link org.metabit.platform.support.config.ConfigFactoryBuilder#addSource(org.metabit.platform.support.config.ConfigLocation)},
     * is to be initialized by the factory.
     *
     * <p>If true, the factory calls {@link #init(ConfigFactoryInstanceContext)} and {@link #postInit(BootstrapConfigContext)}
     * on that instance, as it does for discovered storages, and {@link #exit()} when it is closed.
     * {@link #test(ConfigFactorySettings, ConfigLoggingInterface)} is not consulted: adding the source is an explicit choice.
     * Only storages whose {@code init()} does not register search-list entries of its own should return true;
     * otherwise those entries would be added a second time.</p>
     *
     * <p>Instances found by discovery are initialized regardless of this setting.</p>
     *
     * @return true if an instance added with a source is to be initialized by the factory. Default: false.
     * @since 0.8.16
     * @deprecated stopgap for 0.8.x. In 0.9 storage types and storage instances become separate SPI types,
     * and the factory owns the lifecycle of every instance (MLIB-155); this method is then removed.
     */
    @Deprecated(since = "0.8.16", forRemoval = true)
    default boolean isInitializedWhenAddedAsSource()
        {
        return false;
        }

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
    A single ConfigLocation may add several such layers.
    Returning temporary hierarchical maps just to combine them (plus ordering logic) is overhead without additional use.
    Instead, we allow the ConfigStorage to write its respective findings to the LayeredConfiguration
     */
    /**
     * Update configuration layers by reading from the specified location.
     * <p>
     * A single {@code ConfigLocation} may contribute multiple {@code ConfigLayer}s.
     * This method is responsible for identifying and adding those layers to the {@code LayeredConfigurationInterface}.
     *
     * @param sanitizedConfigName name of the configuration to read
     * @param possibleSource the location where to look for configuration data
     * @param layeredCfg the configuration facade that collects the layers
     */
    void updateConfigurationLayers(final String sanitizedConfigName, final ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg);

    /**
     * Optional: provide additional layers for a configuration.
     * This can be used by specialized storages like secrets to add their layers.
     *
     * @param sanitizedConfigName name of the configuration
     * @param layeredCfg the layered configuration being built
     */
    default void provideAdditionalLayers(String sanitizedConfigName, LayeredConfigurationInterface layeredCfg) {}

    /**
     * Create a new configuration layer for writing.
     *
     * @param configName name of the configuration
     * @param location the location where the layer should be created
     * @param configSchema optional schema for validation
     * @param layeredConfiguration the parent layered configuration
     * @return a new {@code ConfigLayerInterface} instance, or {@code null} if not supported
     */
    ConfigLayerInterface createConfigurationLayer(String configName, ConfigLocation location, ConfigSchema configSchema, LayeredConfiguration layeredConfiguration);

    /**
     * Update BLOB configuration data from the specified location.
     *
     * @param sanitizedConfigName name of the configuration
     * @param location location to look in
     * @param blobConfig the blob configuration instance to update
     */
    void updateBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig);


    /**
     * list all available configurations in the given location.
     *
     * @param location the location to search in
     * @return a set of configuration discovery info found.
     */
    java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(org.metabit.platform.support.config.ConfigLocation location);

    /**
     * check if the given location is an additional test directory.
     * This is useful for knowing if we should prefer it for writing in test mode.
     *
     * @param location the location to check
     * @return true if it is an additional test directory; false otherwise.
     */
    default boolean isPreferredTestLocation(ConfigLocation location) { return false; }

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
