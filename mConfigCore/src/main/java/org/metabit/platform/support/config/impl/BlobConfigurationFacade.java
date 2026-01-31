package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>BlobConfigurationFacade class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class BlobConfigurationFacade extends AbstractConfiguration implements Configuration
{
    private final BlobConfiguration inner;
    private final ConfigLoggingInterface logger;

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
     *
     * get a String entry from a config.
     */
    @Override
    public String getString(String fullKey)
            throws ConfigException
        {
        if ((fullKey == null)||("".equalsIgnoreCase(fullKey)))
            {
            try
                {
                return new String(inner.getBlob(EnumSet.allOf(ConfigScope.class)), StandardCharsets.UTF_8);
                }
            catch (ConfigCheckedException e)
                { throw new ConfigException(e); }
            }
        // else
        logger.warn("BLOB read access attempted with invalid key: " + fullKey);
        return "";
        }



    /**
     * {@inheritDoc}
     *
     * get an array of bytes entry from a config.
     * Formats for which no matching type is defined will be subject to interpretation and conversion,
     * matching typical string values to the range of the output type.
     * <br/>
     * strings will be converted to byte arrays, using base64 if applicable, hex as a fallback, UTF-8 as last resort.
     */
    @Override
    public byte[] getBytes(String fullKey)
            throws ConfigException
        {
        final EnumSet<ConfigScope> scopes = EnumSet.allOf(ConfigScope.class);
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey)))
            {
            try
                { return inner.getBlob(scopes); }
            catch (ConfigCheckedException e)
                { throw new ConfigException(e); }
            }
        // else
        logger.warn("BLOB read access attempted with invalid key: " + fullKey);
        return new byte[0];
        }

    @Override
    public void put(String fullKey, String value, ConfigScope scope) throws ConfigException {
        try {
            putGeneric(fullKey, value, ConfigEntryType.STRING, scope);
        } catch (ConfigCheckedException e) {
            throw new ConfigException(e);
        }
    }

    @Override
    protected void putGeneric(String fullKey, Object value, ConfigEntryType type, ConfigScope scope) throws ConfigCheckedException {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey))) {
            byte[] bytes;
            if (value instanceof byte[]) {
                bytes = (byte[]) value;
            } else {
                bytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
            }
            inner.putBlob(bytes, scope);
            return;
        }
        // else
        logger.warn("BLOB write access attempted with invalid key: " + fullKey);
    }

    /**
     * {@inheritDoc}
     *
     * put = write a value to a key, multiple scopes with fall-forward.
     * <p>
     * automatic creation of new configurations is controlled by settings in the CFB and ConfigFactory.
     */
    @Override
    public void put(String fullKey, String value, EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null)||("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(value.getBytes(StandardCharsets.UTF_8), scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: " + fullKey);
        }


    /** {@inheritDoc} */
    @Override
    public void put(String fullKey, byte[] value, ConfigScope scope)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null)||("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(value, scope);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: " + fullKey);
        }

    /** {@inheritDoc} */
    @Override
    public void put(final String fullKey, final byte[] value, final EnumSet<ConfigScope> scopes)
            throws ConfigException
        {
        if (value == null)
            return; // ignore
        if ((fullKey == null)||("".equalsIgnoreCase(fullKey)))
            {
            inner.putBlob(value, scopes);
            return;
            }
        // else
        logger.warn("BLOB write access attempted with invalid key: " + fullKey);
        }

    @Override
    public void put(String fullKey, Long value, ConfigScope scope) throws ConfigException {
        try {
            putGeneric(fullKey, value, ConfigEntryType.NUMBER, scope);
        } catch (ConfigCheckedException e) {
            throw new ConfigException(e);
        }
    }

    @Override
    public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes) throws ConfigException {
        if (value == null)
            return; // ignore
        if ((fullKey == null) || ("".equalsIgnoreCase(fullKey))) {
            inner.putBlob(String.valueOf(value).getBytes(StandardCharsets.UTF_8), scopes);
            return;
        }
        // else
        logger.warn("BLOB write access attempted with invalid key: " + fullKey);
    }

    // -------------------------------------------------------------------------
    /** {@inheritDoc} */
    @Override
    public ConfigScheme getConfigScheme()
        { return null; } // no config schemes for BLOBs.

    /**
     * {@inheritDoc}
     *
     * get notified when the configuration changes.
     * Note: The scopes for which notifications are sent exclude RUNTIME by default.
     * You can set the scope filter globally with the parameter UPDATE_CHECK_SCOPES
     */
    @Override
    public void subscribeToUpdates(Consumer<ConfigLocation> listener)
        {
        // Subscriptions unsupported for BLOB configurations (content read fresh on access)
        throw new UnsupportedOperationException("Subscriptions unsupported for BLOB configurations");
        }

    /**
     * {@inheritDoc}
     *
     * Unsupported for BLOB configurations (single blob entry)
     */
    @Override
    public void subscribeToUpdates(String fullKey, Consumer<ConfigLocation> listener)
        {
        // Per-key subscriptions unsupported for BLOB configurations
        throw new UnsupportedOperationException("Per-key subscriptions unsupported for BLOB configurations");
        }

    /**
     * {@inheritDoc}
     *
     * Remove subscription for all updates where this listener might be called
     */
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

    /**
     * get the list of all source locations the layers refer to.
     *
     * @return a list of ConfigLocation entries
     */
    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        // Blob filenames derived from config name
        return inner.getSourceLocations();
        }

    @Override
    public String getConfigName()
        { return inner.getConfigName(); }

    /**
     * {@inheritDoc}
     *
     * get an config entry.
     */
    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        return inner.getConfigEntryFromFullKey(fullKey, scopes);
        }

    /**
     * {@inheritDoc}
     *
     * set the config scheme to use and validate against.
     */
    @Override
    public void setConfigScheme(ConfigScheme scheme)
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    /**
     * {@inheritDoc}
     *
     * check whether this configuration is writable
     */
    @Override
    public boolean isWriteable()
        { return inner.isWriteable(); }

    /**
     * {@inheritDoc}
     *
     * flush all write caches, if write-buffering is activated
     */
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
    public SecretValue getSecret(String fullKey) throws ConfigException
        {
        throw new ConfigException(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE);
        }

    @Override
    public void put(String fullKey, List<String> value, ConfigScope scope) throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }

    @Override
    public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes) throws ConfigException
        {
        throw new UnsupportedOperationException("invalid with BLOB configurations");
        }
}
//___EOF___
