package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.metabit.platform.support.config.ConfigCheckedException.ConfigExceptionReason.INVALID_USE;

public class BlobConfigEntryLeaf extends AbstractConfigEntry
    {
    private byte[] blob;

    public BlobConfigEntryLeaf(String key, byte[] blob)
        {
        super(key, null);
        this.blob = (blob != null) ? blob.clone() : null;
        }

    public BlobConfigEntryLeaf(String key, byte[] blob, ConfigEntryMetadata meta)
        {
        super(key, meta);
        this.blob = (blob != null) ? blob.clone() : null;
        }

    /**
     * if the entry is of type string, return its value.
     * if the entry is of another type, try to convert to string.
     *
     * @return String representation of the entry value.
     *
     * @throws ConfigCheckedException if entry value is not convertible.
     */
    @Override
    public String getValueAsString()
            throws ConfigCheckedException
        { return new String(blob, StandardCharsets.UTF_8); }

    /**
     * <p>getValueAsBoolean.</p>
     *
     * @return a {@link Boolean} object
     *
     * @throws ConfigCheckedException if any.
     */
    @Override
    public Boolean getValueAsBoolean()
            throws ConfigCheckedException
        { throw new ConfigCheckedException(INVALID_USE); }

    /**
     * <p>getValueAsInteger.</p>
     *
     * @return a {@link Integer} object
     *
     * @throws ConfigCheckedException if any.
     */
    @Override
    public Integer getValueAsInteger()
            throws ConfigCheckedException
        { throw new ConfigCheckedException(INVALID_USE); }


    /**
     * <p>getValueAsLong.</p>
     *
     * @return a {@link Long} object
     *
     * @throws ConfigCheckedException if any.
     */
    @Override
    public Long getValueAsLong()
            throws ConfigCheckedException
        { throw new ConfigCheckedException(INVALID_USE); }


    /**
     * <p>getValueAsDouble.</p>
     *
     * @return a {@link Double} object
     *
     * @throws ConfigCheckedException if any.
     */
    @Override
    public Double getValueAsDouble()
            throws ConfigCheckedException
        { throw new ConfigCheckedException(INVALID_USE); }


    /**
     * <p>getValueAsBigInteger.</p>
     *
     * @return a {@link BigInteger} object
     *
     * @throws ConfigCheckedException if any.
     */
    @Override
    public BigInteger getValueAsBigInteger()
            throws ConfigCheckedException
        {
        //@TODO may be possible...
        throw new ConfigCheckedException(INVALID_USE);
        }

    /**
     * <p>getValueAsBigDecimal.</p>
     *
     * @return a {@link BigDecimal} object
     *
     * @throws ConfigCheckedException if any.
     */
    @Override
    public BigDecimal getValueAsBigDecimal()
            throws ConfigCheckedException
        { throw new ConfigCheckedException(INVALID_USE); }


    @Override
    public byte[] getValueAsBytes()
            throws ConfigCheckedException
        { return (blob != null) ? blob.clone() : null; }

    @Override
    public List<String> getValueAsStringList()
            throws ConfigCheckedException
        { throw new ConfigCheckedException(INVALID_USE); }

    @Override
    public ConfigEntryType getType()
        {
        return ConfigEntryType.BYTES;
        }

    @Override
    public void putString(String value)
            throws ConfigCheckedException
        {
        this.blob = value.getBytes(StandardCharsets.UTF_8);
        this.writeBack();
        }

    /**
     * if writeable, write a value of specified type
     *
     * @param value     the value to be written
     * @param valueType the type what is to be written
     * @throws ConfigCheckedException on failure
     */
    @Override
    public void putValue(Object value, ConfigEntryType valueType)
            throws ConfigCheckedException
        {
        if (valueType == ConfigEntryType.BYTES)
            {
            this.blob = (byte[]) value;
            this.writeBack();
            }
        else if (valueType == ConfigEntryType.STRING)
            {
            this.putString((String) value);
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }

    @Override
    public String toString()
        {
        return "BlobConfigEntryLeaf{" +
                "blob=" + (isSecret() ? "[REDACTED]" : "byte[" + blob.length + "]") +
                '}';
        }
}
