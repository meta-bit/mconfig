package org.metabit.platform.support.config.impl.secrets;
import org.metabit.platform.support.config.impl.entry.*;
import org.metabit.platform.support.config.impl.entry.SecretConfigEntry;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class SecretConfigEntryLeaf extends AbstractConfigEntry implements SecretConfigEntry
    {
    private final SecretValue secretValue;

    public SecretConfigEntryLeaf(String key, SecretValue secretValue, ConfigLocation location)
        {
        super(key, new ConfigEntryMetadata(null)); // @TODO better metadata handling for secrets
        if (location instanceof ConfigSource)
            {
            this.meta.setSource((ConfigSource) location);
            }
        this.secretValue = secretValue;
        }

    @Override
    public String getValueAsString() throws ConfigCheckedException
        {
        // For security reasons, we might not want to allow secret values as strings in generic contexts,
        // but for PLAIN_TEXT secrets it might be necessary.
        // However, the issue description says "special handling for security reasons".
        return new String(secretValue.getValue());
        }

    @Override
    public Boolean getValueAsBoolean() throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
        }

    @Override
    public Integer getValueAsInteger() throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
        }

    @Override
    public Long getValueAsLong() throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
        }

    @Override
    public Double getValueAsDouble() throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
        }

    @Override
    public BigInteger getValueAsBigInteger() throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
        }

    @Override
    public BigDecimal getValueAsBigDecimal() throws ConfigCheckedException
        {
        throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
        }

    @Override
    public byte[] getValueAsBytes() throws ConfigCheckedException
        { return secretValue.getValue(); }

    @Override
    public List<String> getValueAsStringList() throws ConfigCheckedException
        { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE); }


    @Override
    public ConfigEntryType getType()
        {
        switch (secretValue.getType())
            {
            case PLAIN_TEXT:
            case HASHED_TEXT:
                return ConfigEntryType.STRING;
            case PRIVATE_KEY:
            case SYMMETRIC_KEY:
            case CERTIFICATE:
            default:
                return ConfigEntryType.BYTES;
            }
        }

    @Override
    public boolean isSecret()
        { return true; }

    @Override
    public void putString(String value) throws ConfigCheckedException
        { this.putValue(value, ConfigEntryType.STRING); }

    @Override
    public char[] getValueAsChars() throws ConfigCheckedException
        { return secretValue.getChars(); }

    @Override
    public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException
        {
        // 1. check if location/storage is generally writeable
        if (!getLocation().isWriteable())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }

        // Secrets need special handling because the entry itself is often a wrapper 
        // around a value that might come from a dedicated secrets provider or be 
        // encrypted in a normal file.
        this.writeBack();
        }

    public SecretValue getSecretValue()
        { return secretValue; }

    @Override
    public String toString()
        {
        return "SecretConfigEntryLeaf{key='" + key + "', type=" + secretValue.getType() + ", value=[REDACTED]}";
        }
}
