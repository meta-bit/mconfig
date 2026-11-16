package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEventList;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Read-only view on another {@link Configuration}: every read goes to that configuration, with
 * the key passed through {@link #mapKey(String)}. Subclasses override what they change.
 * <p>
 * The view does not validate: the configuration it shows has done that already. Values are
 * handed on as they come, so a secret stays a {@link SecretValue} and is never read as a String
 * on the way.
 */
abstract class DelegatingConfigurationView extends ReadOnlyConfigurationView
{
    protected final Configuration source;
    private         ConfigEventList ownEvents; // only if the source has none

    DelegatingConfigurationView(String viewName, Configuration source)
        {
        super(viewName);
        this.source = Objects.requireNonNull(source, "the configuration to show cannot be null");
        }

    /**
     * @param key the key as the caller of the view gave it, trimmed
     * @return the key to use on the configuration shown
     */
    protected String mapKey(String key)
        {
        return key;
        }

    private String sourceKey(String fullKey)
        {
        return mapKey(fullKey.trim());
        }

    @Override public String getString(String fullKey) throws ConfigException { return source.getString(sourceKey(fullKey)); }
    @Override public Boolean getBoolean(String fullKey) throws ConfigException { return source.getBoolean(sourceKey(fullKey)); }
    @Override public Integer getInteger(String fullKey) throws ConfigException { return source.getInteger(sourceKey(fullKey)); }
    @Override public Long getLong(String fullKey) throws ConfigException { return source.getLong(sourceKey(fullKey)); }
    @Override public Double getDouble(String fullKey) throws ConfigException { return source.getDouble(sourceKey(fullKey)); }
    @Override public BigInteger getBigInteger(String fullKey) throws ConfigException { return source.getBigInteger(sourceKey(fullKey)); }
    @Override public BigDecimal getBigDecimal(String fullKey) throws ConfigException { return source.getBigDecimal(sourceKey(fullKey)); }
    @Override public byte[] getBytes(String fullKey) throws ConfigException { return source.getBytes(sourceKey(fullKey)); }
    @Override public List<String> getListOfStrings(String fullKey) { return source.getListOfStrings(sourceKey(fullKey)); }
    @Override public SecretValue getSecret(String fullKey) throws ConfigException { return source.getSecret(sourceKey(fullKey)); }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        return source.getConfigEntryFromFullKey(sourceKey(fullKey), scopes);
        }

    @Override
    public synchronized ConfigEventList getEvents()
        {
        ConfigEventList events = source.getEvents();
        if (events != null)
            { return events; }
        if (ownEvents == null)
            { ownEvents = new ConfigEventList(1000); }
        return ownEvents;
        }

    @Override
    public ConfigSchema getConfigSchema()
        {
        return source.getConfigSchema();
        }

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        source.subscribeToUpdates(listener);
        }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        source.subscribeToUpdates(sourceKey(fullKey), listener);
        }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        source.unsubscribeFromUpdates(listener);
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        return source.getSourceLocations();
        }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        return source.getAllConfigurationKeysFlattened(scopes);
        }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        {
        return source.getAllConfigurationKeysWithSchemasFlattened(scopes);
        }

    @Override
    public String getConfigName()
        {
        return source.getConfigName();
        }

    @Override
    public boolean reload()
            throws ConfigCheckedException
        {
        return source.reload();
        }

    @Override
    public boolean isEmpty()
        {
        return source.isEmpty();
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        return source.getEntryKeyTreeIterator();
        }

    @Override
    public boolean isClosed()
        {
        return source.isClosed();
        }
}
//___EOF___
