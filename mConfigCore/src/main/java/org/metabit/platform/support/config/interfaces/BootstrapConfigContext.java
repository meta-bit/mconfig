package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.ConfigScope;

import java.util.EnumSet;

/**
 * Restricted context passed to {@link ConfigStorageInterface#postInit(BootstrapConfigContext)} for
 * self-configuration of networked or remote storage implementations.
 * <p>
 * This interface intentionally exposes only the operations needed for bootstrap: reading configuration
 * values from a scope-limited view and accessing the logger. It prevents networked storages from
 * accidentally reading their own (not yet initialized) scope during bootstrap, which would cause
 * circular initialization.
 * </p>
 * <p>
 * Use {@link #getRestrictedConfig(String, EnumSet)} with a scope set limited to local sources
 * (e.g. {@code PRODUCT}, {@code HOST}, {@code USER}, {@code APPLICATION}) to safely read bootstrap
 * settings without creating circular references.
 * </p>
 */
public interface BootstrapConfigContext
    {
    /**
     * Returns a scope-restricted {@link Configuration} view for the given config name.
     * <p>
     * Only {@link org.metabit.platform.support.config.ConfigLocation}s whose scope is contained
     * in {@code scopes} contribute layers to the returned configuration. This prevents remote/cloud
     * storages from being queried during their own bootstrap phase.
     * </p>
     * <p>
     * The returned configuration is not cached; each call constructs a fresh view.
     * It is a snapshot: it does not check for or receive source changes, and does not need to be closed.
     * </p>
     *
     * @param configName the logical configuration name (e.g. {@code "vault"}, {@code "aws"})
     * @param scopes     the set of scopes to include; pass low-precedence local scopes to avoid circular references
     * @return a scope-restricted configuration, never {@code null}
     */
    Configuration getRestrictedConfig(String configName, EnumSet<ConfigScope> scopes);

    /**
     * Returns the logger for this configuration factory instance.
     *
     * @return the logger, never {@code null}
     */
    ConfigLoggingInterface getLogger();
    }
//___EOF___
