package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A wrapper for Configuration that ignores getSecret calls.
 * Used during the initialization phase of secret providers to prevent circular dependencies.
 */
public class ConfigurationPhaseWrapper extends AbstractConfiguration implements Configuration {
    private final Configuration delegate;

    public ConfigurationPhaseWrapper(Configuration delegate) {
        this.delegate = delegate;
    }

    @Override
    public SecretValue getSecret(String fullKey) throws ConfigException {
        // This is the main reason for this wrapper: ignore getSecret calls during init phase
        return null;
    }

    @Override
    public void put(String fullKey, String value, ConfigScope scope) throws ConfigException {
        delegate.put(fullKey, value, scope);
    }

    @Override
    protected void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope) throws ConfigCheckedException {
        if (delegate instanceof AbstractConfiguration) {
            ((AbstractConfiguration) delegate).putGeneric(fullKey, value, type, scope);
        } else {
            delegate.put(fullKey, String.valueOf(value), scope);
        }
    }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope) throws ConfigException {
        delegate.put(fullKey, value, scope);
    }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes) throws ConfigException {
        delegate.put(fullKey, value, scopes);
    }

    @Override
    public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes) throws ConfigException {
        delegate.put(fullKey, value, scopes);
    }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes) throws ConfigException {
        delegate.put(fullKey, value, scopes);
    }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope) throws ConfigException {
        delegate.put(fullKey, value, scope);
    }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope) throws ConfigException {
        delegate.put(fullKey, value, scope);
    }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes) throws ConfigException {
        delegate.put(fullKey, value, scopes);
    }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope) throws ConfigException {
        delegate.put(fullKey, value, scope);
    }

    @Override
    public ConfigScheme getConfigScheme() {
        return delegate.getConfigScheme();
    }

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener) {
        delegate.subscribeToUpdates(listener);
    }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener) {
        delegate.subscribeToUpdates(fullKey, listener);
    }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener) {
        delegate.unsubscribeFromUpdates(listener);
    }

    @Override
    public ConfigCursor getConfigCursor() {
        return delegate.getConfigCursor();
    }

    @Override
    public List<ConfigLocation> getSourceLocations() {
        return delegate.getSourceLocations();
    }

    @Override
    public String getConfigName() {
        return delegate.getConfigName();
    }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes) {
        return delegate.getConfigEntryFromFullKey(fullKey, scopes);
    }

    @Override
    public void setConfigScheme(ConfigScheme scheme) {
        delegate.setConfigScheme(scheme);
    }

    @Override
    public boolean isWriteable() {
        return delegate.isWriteable();
    }

    @Override
    public int flush() throws ConfigCheckedException {
        return delegate.flush();
    }

    @Override
    public boolean reload() throws ConfigCheckedException {
        return delegate.reload();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Iterator<String> getEntryKeyTreeIterator() {
        return delegate.getEntryKeyTreeIterator();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } catch (Exception e) {
            // Nothing we can do here besides logging, but we don't have a logger in this wrapper
        }
    }
}
