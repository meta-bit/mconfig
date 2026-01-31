package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>StringConfigEntryLeaf class.</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class StringConfigEntryLeaf extends AbstractConfigEntry
    {
    private String             value;
    final static List<String> stringListType = new ArrayList<>(0); // for safe class casting

    /**
     * <p>Constructor for StringConfigEntryLeaf.</p>
     *
     * @param key a {@link java.lang.String} object
     * @param value a {@link java.lang.String} object
     * @param meta a {@link ConfigEntryMetadata} object
     */
    public StringConfigEntryLeaf(final String key, final String value, final ConfigEntryMetadata meta)
        {
        super(key, meta);
        this.value = value;
        }

    // would be "final" static if Java allowed interfaces to have static functions
    /** {@inheritDoc} */
    @Override
    public ConfigEntryType getType()
        {
        return ConfigEntryType.STRING;
        }

    /** {@inheritDoc} */
    @Override
    public String getValueAsString()
            throws ConfigCheckedException
        { return value; }

    /** {@inheritDoc} */
    @Override
    public Boolean getValueAsBoolean()
            throws ConfigCheckedException
        { return Boolean.parseBoolean(value); }

    /** {@inheritDoc} */
    @Override
    public Integer getValueAsInteger()
            throws ConfigCheckedException
        {
        try
            { return Integer.valueOf(value); }
        catch (NumberFormatException e)
            { throw new ConfigCheckedException(e); }
        }

    /** {@inheritDoc} */
    @Override
    public Long getValueAsLong()
            throws ConfigCheckedException
        {
        try
            { return Long.valueOf(value); }
        catch (NumberFormatException e)
            { throw new ConfigCheckedException(e); }
        }

    /** {@inheritDoc} */
    @Override
    public Double getValueAsDouble()
            throws ConfigCheckedException
        {
        try
            { return Double.valueOf(value); }
        catch (NumberFormatException e)
            { throw new ConfigCheckedException(e); }
        }

    /** {@inheritDoc} */
    @Override
    public BigInteger getValueAsBigInteger()
            throws ConfigCheckedException
        {
        try
            { return new BigInteger(value); }
        catch (NumberFormatException e)
            { throw new ConfigCheckedException(e); }
        }

    /** {@inheritDoc} */
    @Override
    public BigDecimal getValueAsBigDecimal()
            throws ConfigCheckedException
        {
        try
            { return new BigDecimal(value); }
        catch (NumberFormatException e)
            { throw new ConfigCheckedException(e); }
        }

    /** {@inheritDoc} */
    @Override
    public byte[] getValueAsBytes()
            throws ConfigCheckedException
        { return value.getBytes(StandardCharsets.UTF_8); }

    /** {@inheritDoc} */
    @Override
    public List<String> getValueAsStringList()
            throws ConfigCheckedException
        { return Arrays.asList(value); }

    /** {@inheritDoc} */
    @Override
    public void putString(String value)
            throws ConfigCheckedException
        {
        // write-through in the storage.
        ConfigStorageInterface storage = this.meta.getLocation().getStorage();
        if (!storage.isGenerallyWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        this.value = value; // Escaping handled by ConfigFormat
        this.writeBack();
        }


    /** {@inheritDoc} */
    @Override
    public void putValue(Object value, ConfigEntryType valueType)
            throws ConfigCheckedException
        {
        try
            {
            // then perform conversions, catching invalid object casts.
            switch (valueType)
                {
                case STRING:
                    this.putString((String) value);
                    break;
                case NUMBER:
                case BOOLEAN:
                    this.putString(String.valueOf(value));
                    break;
                case BYTES:
                    this.putString(new String((byte[]) value, StandardCharsets.UTF_8));
                    break;
                case MULTIPLE_STRINGS:
                    @SuppressWarnings("unchecked")
                    List<String> stringList = (List<String>) value;
                    this.putString(String.join(",", stringList)); // Planned: improved list serialization (quoted CSV)
                    break;
                case URI:
                case FILEPATH:
                    this.putString(String.valueOf(value));
                    break;
                default:
                    throw new IllegalStateException("code error - non-existent enum value: " + valueType);
                }
            }
        catch (ClassCastException ex)
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.OBJECT_CLASS_MISMATCHING_WITH_TYPE);
            }
        }


// Source location available via metadata.getLocation()

    @Override
    public String toString()
        {
        return "StringConfigEntryLeaf{" +
                "key='" + key + '\'' +
                ", value='" + (isSecret() ? "[REDACTED]" : value) + '\'' +
                ", scope=" + getScope() +
                '}';
        }

}
