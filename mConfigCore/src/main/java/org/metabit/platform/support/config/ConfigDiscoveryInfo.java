package org.metabit.platform.support.config;

import java.net.URI;
import java.util.Objects;

/**
 * Information about a configuration discovered by the ConfigFactory.
 */
public final class ConfigDiscoveryInfo
{
    private final String      configName;
    private final ConfigScope scope;
    private final URI         uri;
    private final String      formatID;
    private final boolean     writeable;

    public ConfigDiscoveryInfo(String configName, ConfigScope scope, URI uri, String formatID, boolean writeable)
        {
        this.configName = configName;
        this.scope = scope;
        this.uri = uri;
        this.formatID = formatID;
        this.writeable = writeable;
        }

    public String getConfigName()
        {
        return configName;
        }

    public ConfigScope getScope()
        {
        return scope;
        }

    public URI getUri()
        {
        return uri;
        }

    public String getFormatID()
        {
        return formatID;
        }

    public boolean isWriteable()
        {
        return writeable;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigDiscoveryInfo that = (ConfigDiscoveryInfo) o;
        return writeable == that.writeable &&
               Objects.equals(configName, that.configName) &&
               scope == that.scope &&
               Objects.equals(uri, that.uri) &&
               Objects.equals(formatID, that.formatID);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(configName, scope, uri, formatID, writeable);
        }

    @Override
    public String toString()
        {
        return "ConfigDiscoveryInfo{" +
               "name='" + configName + '\'' +
               ", scope=" + scope +
               ", format='" + formatID + '\'' +
               ", uri=" + uri +
               ", writeable=" + writeable +
               '}';
        }
}
