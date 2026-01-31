package org.metabit.platform.support.config;

import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.util.List;
import java.util.Map;

/**
 * entry point to using mConfig.
 * <p>
 * This builder is used to configure and build the actual ConfigFactory.
 * First, instantiate a builder;
 * second, apply all the configuration and settings the config factory is to have;
 * third, build() the ConfigFactory.
 * (fourth: use the ConfigFactory instance. @see ConfigFactory )
 */
public interface ConfigFactoryBuilder
{
    /**
     * static factory method to create a DefaultConfigFactoryBuilder.
     * @param orgOrCompany organization or company name
     * @param applicationName application name
     * @return a new ConfigFactoryBuilder instance.
     */
    static ConfigFactoryBuilder create(String orgOrCompany, String applicationName)
        {
        return new org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder(orgOrCompany, applicationName);
        }

    /**
     * static factory method to create a DefaultConfigFactoryBuilder.
     * @param orgOrCompany organization or company name
     * @param applicationName application name
     * @param subPath sub path to use, level(s) below the the application root.
     * @return a new ConfigFactoryBuilder instance.
     */
    static ConfigFactoryBuilder create(String orgOrCompany, String applicationName, String subPath)
        {
        return new org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder(orgOrCompany, applicationName, subPath);
        }

    /**
     * static factory method to create a DefaultConfigFactoryBuilder.
     * @param configFactoryParameters parameters
     * @return a new ConfigFactoryBuilder instance.
     * @throws ConfigCheckedException if parameters are invalid.
     */
    static ConfigFactoryBuilder create(Map<String, String> configFactoryParameters) throws ConfigCheckedException
        {
        return new org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder(configFactoryParameters);
        }

    /**
     * forbid test mode.
     * This gate prevents test mode from being activated.
     *
     * This method is intended for applications where test mode should
     * be restricted for security purposes, to maintain enforced configuration integrity.
     */
    static void forbidTestMode()
        {
        org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder.forbidTestMode();
        }

    /**
     * permit test mode. There are several ways test mode may be activated (e.g., via configuration parameters, environment variables, or explicit method calls).
     * See test mode in documentation.
     */
    static void permitTestMode()
        {
        org.metabit.platform.support.config.impl.DefaultConfigFactoryBuilder.permitTestMode();
        }


    /**
     * <p>build the ConfigFactory for use.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigFactory} object
     * @throws org.metabit.platform.support.config.ConfigException if any.
     */
    ConfigFactory build() throws ConfigException;

    /**
     * activate (or deactivate) test mode for software testing.
     * a different set of default sources will be used, and modules may behave
     * differently.
     * <p>
     * This has only effect if test mode is permitted. See permitTestMode().
     *
     * @param testMode true to activate test mode, false to deactivate.
     */
    void setTestMode(boolean testMode);

    /**
     * set (replace) file paths for use in test mode
     *
     * @param scopeToSetFor scope to set the test paths for.
     * @param directories   a list of directory paths on local system to be used as roots
     *                      for test config files.
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the paths are inaccessible.
     */
    void setTestConfigPaths(ConfigScope scopeToSetFor, List<String> directories) throws ConfigCheckedException;

    /**
     * provide additional parameters for test mode, if activated.
     *
     * @param testParameters a String map for test mode parameters.
     * @throws java.lang.IllegalArgumentException                  if testParameters is of the wrong type
     * @throws org.metabit.platform.support.config.ConfigException if testParameters contains inconsistent entries
     */
    void setTestParameters(Map<String, String> testParameters) throws ConfigException, IllegalArgumentException;

    /**
     * set a feature.
     *
     * @param feature feature to set
     * @param i       an integer. Must match the type of the feature
     * @return the resulting ConfigFactoryBuilder
     */
    ConfigFactoryBuilder setFeature(ConfigFeature feature, Integer i);

    /**
     * set a feature.
     *
     * @param feature feature to set
     * @param b       a boolean. Must match the type of the feature
     * @return the resulting ConfigFactoryBuilder
     */
    ConfigFactoryBuilder setFeature(ConfigFeature feature, Boolean b);

    /**
     * set a feature.
     *
     * @param feature feature to set
     * @param s       a string. Must match the type of the feature
     * @return the resulting ConfigFactoryBuilder
     */
    ConfigFactoryBuilder setFeature(ConfigFeature feature, String s);

    /**
     * set a feature.
     *
     * @param feature feature to set
     * @param strs    a list of strings, must match the type of the feature
     * @return the resulting ConfigFactoryBuilder
     */
    ConfigFactoryBuilder setFeature(ConfigFeature feature, List<String> strs);

    /**
     * <p>setSpecialFeature.</p>
     *
     * @param feature a {@link org.metabit.platform.support.config.ConfigFeature} object
     * @param param   a {@link java.util.List} object
     * @return a {@link org.metabit.platform.support.config.ConfigFactoryBuilder} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    ConfigFactoryBuilder setSpecialFeature(ConfigFeature feature, List<Object> param) throws ConfigCheckedException;

    /**
     * <p>setSchemes.</p>
     *
     * @param Schemes a {@link java.util.Map} object
     * @return a {@link org.metabit.platform.support.config.ConfigFactoryBuilder} object
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    ConfigFactoryBuilder setSchemes(Map<String, ConfigScheme> Schemes) throws ConfigCheckedException;

    /**
     * set the configuration for the secrets provider.
     *
     * @param config Map with provider-specific configuration
     * @return the resulting ConfigFactoryBuilder
     */
    ConfigFactoryBuilder setSecretsProviderConfig(Map<String, Object> config);

    /**
     * add a secrets provider configuration.
     *
     * @param providerID ID of the secrets provider (e.g. "hashicorp-vault")
     * @param config Map with provider-specific configuration
     * @param scope Scope at which the secrets should be integrated (optional, default APPLICATION)
     * @return the resulting ConfigFactoryBuilder
     */
    ConfigFactoryBuilder addSecretsProvider(String providerID, Map<String, Object> config, ConfigScope scope);

    /**
     * Add configuration scheme(s) using JSON string.
     * Can contain single scheme or array with multiple.
     * Name extracted from JSON "name" field.
     * Parsed during build using ConfigScheme.fromJSON.
     * @param jsonScheme the JSON string defining the scheme(s)
     * @return this builder for chaining
     */
    ConfigFactoryBuilder addSchemeJson(String jsonScheme);
}
//___EOF___
