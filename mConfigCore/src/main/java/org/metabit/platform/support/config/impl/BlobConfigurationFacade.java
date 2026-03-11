package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>BlobConfigurationFacade class.</p>
 */
public class BlobConfigurationFacade extends AbstractConfiguration implements Configuration
{
    private final BlobConfiguration      inner;
    private final ConfigLoggingInterface logger;
    private final ConfigEventList        events = new ConfigEventList(1000);

    /**
     * <p>Constructor for BlobConfigurationFacade.</p>
     *
     * @param blobConfiguration a {@link org.metabit.platform.support.config.impl.BlobConfiguration} object
     */
    public BlobConfigurationFacade(BlobConfiguration blobConfiguration)
        {
        inner = blobConfiguration;
        logger = blobConfiguration.getLogger();
        }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(String fullKey)
            throws ConfigException
        {
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            try
                {
                return new String(inner.getBlob(EnumSet.allOf(ConfigScope.class)), StandardCharsets.UTF_8);
                }
            catch (ConfigCheckedException e)
                {
                throw new ConfigException(e);
                }
            }
        // else
        logger.warn("BLOB read access attempted with invalid key: "+fullKey);
        return "";
        }


    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBytes(String fullKey)
            throws ConfigException
        {
        final EnumSet<ConfigScope> scopes = EnumSet.allOf(ConfigScope.class);
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            try
                {
                return inner.getBlob(scopes);
                }
            catch (ConfigCheckedException e)
                {
                throw new ConfigException(e);
                }
            }
        // else
        logger.warn("BLOB read access attempted with invalid key: "+fullKey);
        return new byte[0];
        }

    @Override
    public ConfigEventList getEvents()
        {
        return events;
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.MULTIPLE_STRINGS, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, Integer value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, Double value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, BigInteger value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, BigDecimal value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, Boolean value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.BOOLEAN, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, String value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.STRING, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.BYTES, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope)
            throws ConfigException
        {
        try { putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope); }
        catch (ConfigCheckedException e) { throw new ConfigException(e); }
        }

    @Override
    protected void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope)
            throws ConfigCheckedException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || (fullKey.isEmpty()))
            {
            byte[] bytes;
            if (value instanceof byte[])
                {
                bytes = (byte[]) value;
                }
            else
                {
                bytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
                }
            inner.putBlob(bytes, scope);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        throw new UnsupportedOperationException("invalid with BLOB configurations: nested keys not supported");
        }

    /** {@inheritDoc} */
    @Override
    public void put(final String fullKey, final byte[] value, final EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(value, scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.join("|", value).getBytes(StandardCharsets.UTF_8), scopes); // Heuristic
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(value.getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: "+fullKey);
        }

    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public ConfigSchema getConfigSchema()
        { return null; } // no config schemes for BLOBs.

    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        // Subscriptions unsupported for BLOB configurations (content read fresh on access)
        throw new UnsupportedOperationException("Subscriptions unsupported for BLOB configurations");
        }

    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        // Per-key subscriptions unsupported for BLOB configurations
        throw new UnsupportedOperationException("Per-key subscriptions unsupported for BLOB configurations");
        }

    @Override
    public void unsubscribeFromUpdates(Consumer<ConfigLocation> listener)
        {
        // Unsubscriptions unsupported for BLOB configurations (no subscriptions active)
        throw new UnsupportedOperationException("Unsubscriptions unsupported for BLOB configurations");
        }

    /** {@inheritDoc} */
    @Override
    public ConfigCursor getConfigCursor()
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        // Blob filenames derived from config name
        return inner.getSourceLocations();
        }

    @Override
    public String getConfigName()
        { return inner.getConfigName(); }

    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        return inner.getConfigEntryFromFullKey(fullKey, scopes);
        }

    @Override
    public void setConfigSchema(ConfigSchema schema)
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public boolean isWriteable()
        { return inner.isWriteable(); }

    @Override
    public int flush()
            throws ConfigCheckedException
        { return inner.flush(); }

    @Override
    public boolean reload()
            throws ConfigCheckedException
        { return inner.reload(); }

    @Override
    public boolean isEmpty()
        { return inner.isEmpty(); }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public boolean isClosed()
        {
        return inner.isClosed();
        }

    @Override
    public void close()
        {
        try
            {
            inner.close();
            }
        catch (Exception e)
            {
            logger.error("Failed to close BlobConfigurationFacade", e);
            }
        }


    // -------------------------------------------------------------------------
    // for all of these: no, not with a BLOB.

    @Override
    public Boolean getBoolean(String fullKey)
            throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public Integer getInteger(String fullKey)
            throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public Long getLong(String fullKey)
            throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public Double getDouble(String fullKey)
            throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public BigInteger getBigInteger(String fullKey)
            throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public BigDecimal getBigDecimal(String fullKey)
            throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public List<String> getListOfStrings(String fullKey)
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public SecretValue getSecret(String fullKey)
            throws ConfigException
        {
        throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE);
        }
}
//___EOF___
