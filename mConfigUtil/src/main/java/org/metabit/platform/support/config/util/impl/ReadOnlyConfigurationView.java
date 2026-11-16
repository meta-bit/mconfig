package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigCursor;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;

/**
 * Common base of the {@link Configuration} views in this package: everything that would change
 * a configuration is refused, in one way.
 * <p>
 * The views help with transition and adoption. They are not a second facade: whoever needs to
 * write, or to navigate with a cursor, uses the mConfig {@link Configuration} itself.
 */
abstract class ReadOnlyConfigurationView implements Configuration
{
    private final String viewName;

    ReadOnlyConfigurationView(String viewName)
        {
        this.viewName = viewName;
        }

    private ConfigException readOnly()
        {
        return new ConfigException(ConfigException.ConfigExceptionReason.NOT_WRITEABLE, viewName+" is a read-only view");
        }

    @Override public void put(String fullKey, String value) { throw readOnly(); }
    @Override public void put(String fullKey, Boolean value) { throw readOnly(); }
    @Override public void put(String fullKey, Integer value) { throw readOnly(); }
    @Override public void put(String fullKey, Long value) { throw readOnly(); }
    @Override public void put(String fullKey, Double value) { throw readOnly(); }
    @Override public void put(String fullKey, BigInteger value) { throw readOnly(); }
    @Override public void put(String fullKey, BigDecimal value) { throw readOnly(); }
    @Override public void put(String fullKey, byte[] value) { throw readOnly(); }
    @Override public void put(String fullKey, List<String> value) { throw readOnly(); }
    @Override public void put(String fullKey, String value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, String value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, Boolean value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, Boolean value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, Integer value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, Integer value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, Long value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, Long value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, Double value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, Double value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, BigInteger value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, BigInteger value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, BigDecimal value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, BigDecimal value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, byte[] value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, byte[] value, EnumSet<ConfigScope> scopes) { throw readOnly(); }
    @Override public void put(String fullKey, List<String> value, ConfigScope scope) { throw readOnly(); }
    @Override public void put(String fullKey, List<String> value, EnumSet<ConfigScope> scopes) { throw readOnly(); }

    @Override
    public void setConfigSchema(ConfigSchema schema)
        {
        throw new UnsupportedOperationException(viewName+" is an immutable view; set the schema on the configuration it shows");
        }

    @Override
    public ConfigCursor getConfigCursor()
        {
        throw new UnsupportedOperationException(viewName+" does not support cursors");
        }

    @Override
    public boolean isWriteable()
        {
        return false;
        }

    /** nothing is buffered for writing. */
    @Override
    public int flush()
            throws ConfigCheckedException
        {
        return 0;
        }

    /** a view owns nothing; the configuration it shows is closed by its owner. */
    @Override
    public void close()
        {
        // no-op
        }
}
//___EOF___
