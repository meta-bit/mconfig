package org.metabit.platform.support.config.impl.entry;

import org.metabit.platform.support.config.*;

/**
 * A ConfigEntry implementation representing a map node in the configuration tree.
 */
public class MapConfigEntry extends AbstractConfigEntry
{
    private final String simpleKey;

    public MapConfigEntry(String simpleKey)
        {
        super(simpleKey, null);
        this.simpleKey = simpleKey;
        }

    @Override public String getKey() { return simpleKey; }

    @Override public ConfigEntryType getType() { return null; }
    @Override public String getValueAsString() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public Boolean getValueAsBoolean() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public Integer getValueAsInteger() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public Long getValueAsLong() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public Double getValueAsDouble() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public java.math.BigInteger getValueAsBigInteger() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public java.math.BigDecimal getValueAsBigDecimal() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public byte[] getValueAsBytes() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public java.util.List<String> getValueAsStringList() throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public void putString(String value) throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
    @Override public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }
}
