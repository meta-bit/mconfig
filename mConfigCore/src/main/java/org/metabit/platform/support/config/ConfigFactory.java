package org.metabit.platform.support.config;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.impl.ConfigFactoryInternal;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * <p>ConfigFactory interface.</p>
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigFactory extends AutoCloseable, ConfigFactoryInternal
{
    /**
     * explicit shutdown.
     * Since we have no reliable deterministic destructor in Java,
     * we need to provide a different way to explicitly close the handles opened,
     * and so on.
     * <p>
     * after calling this, the ConfigFactory instance becomes invalid.
     */
    void close();


    /**
     * list all available configurations discovered by this factory, with metadata.
     *
     * @return a set of configuration discovery info.
     */
    java.util.Set<ConfigDiscoveryInfo> listAvailableConfigurations();


    /// get a configuration instance for reading.
    ///
    /// @param configName name to look up by. (special value: null returns an empty Configuration, intentionally.)
    /// @return configuration handle.
    ///
    /// @throws org.metabit.platform.support.config.ConfigException if no such configuration was found (including fallbacks and defaults).
    ///
    ///                                                                                                                         We intentionally use ConfigException (a runtime exception) instead of ConfigCheckedException,
    ///                                                                                                                         because we want to avoid unnecessary overhead of checked exceptions for common use cases.
    ///                                                                                                                         Checks are handled at initialization time, defaults and fallbacks exist;
    ///                                                                                                                         anything severe enough to be left here is indeed a runtime error.
    ///                                                                                                                         Please use the default and scheme mechanisms.
    Configuration getConfig(final String configName)
            throws ConfigException;

    /**
     * info: where will this ConfigFactory search for configurations, and in which order?
     *
     * @return an ordered list of ConfigSearchListEntry
     */
    List<ConfigLocation> getSearchList();


    /**
     * get a list of all ConfigEvents for this ConfigFactory.
     * ConfigEvents specific to a Configuration are found there.
     *
     * @return ConfigEventList
     */
    ConfigEventList getEvents();

    /**
     * get configuration instance, with explicit Schemes
     *
     * @param configName   the key/name for the configuration
     * @param configScheme the Scheme for the configuration - may be "null" for none.
     * @return Configuration instance. (see also Feature EXCEPTION_ON_NULL)
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the Configuration could not be found.
     */
    Configuration getConfig(final String configName, final ConfigScheme configScheme)
            throws ConfigCheckedException;


    /**
     * special config instance access.
     *
     * @param configName the raw name of the configuration desired
     * @param scopes     the scopes the special config is to be used in/searched for
     * @param wanted     attributes wanted with this
     * @return a configuration matching the requested.
     *
     * @throws org.metabit.platform.support.config.ConfigCheckedException if no configuration matching the wanted parameters is found.
     */
    Configuration getConfigSpecial(final String configName, final EnumSet<ConfigScope> scopes, final Map<String,String> wanted)
            throws ConfigCheckedException;


    /**
     * add a configuration scheme to the overall pool of known config schemes.
     *
     * The primary use case is for tests. Production code should place its
     * schemes in the resource folder, if it is distributed as a JAR.
     *
     * @param configName                the configuration to set this scheme for
     * @param jsonFormattedConfigScheme the JSON-formatted config scheme
     * @throws org.metabit.platform.support.config.ConfigCheckedException   if the string is not in a valid ConfigScheme format
     */
    void addConfigScheme(final String configName, final String jsonFormattedConfigScheme)
            throws ConfigCheckedException;

}
//___EOF___

