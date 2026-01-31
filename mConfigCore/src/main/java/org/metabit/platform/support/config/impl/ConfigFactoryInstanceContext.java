package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.impl.core.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.scheme.repository.ConfigSchemeRepository;
import org.metabit.platform.support.config.scheme.repository.DefaultConfigSchemeRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * internal implementation class. Encapsulation of the context  data for a
 * ConfigFactory instance.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigFactoryInstanceContext
{
    private final ConfigFactorySettings               settings;
    private       ConfigLoggingInterface              logger;
    private       Map<String, ConfigStorageInterface> configStorages;
    private       Map<String, ConfigFormatInterface>  configFormats;
    private       Map<String, ConfigSecretsProviderInterface> configSecretsProviders;
    private       ConfigSearchList                    searchList;
    private       ClassLoader                         classLoader;
    private       SourceChangeNotifier                sourceChangeNotifier;
    private final ConfigSchemeRepository              schemeRepository = new DefaultConfigSchemeRepository();
    private       ConfigFactory                       factory;

    /**
     * constructor.
     *
     * @param configFactorySettings the settings to use.
     */
    public ConfigFactoryInstanceContext(final ConfigFactorySettings configFactorySettings)
        {
        this.settings = configFactorySettings; // or copy
        this.searchList = new ConfigSearchList();
        // safety inits
        this.logger = NullLogging.getSingletonInstance();
        this.configStorages = new HashMap<>();
        this.configFormats = new HashMap<>();
        this.configSecretsProviders = new HashMap<>();
        this.classLoader = this.getClass().getClassLoader();
        }

    public ConfigFactory getFactory()
    {
        return factory;
    }

    public void setFactory(ConfigFactory factory)
    {
        this.factory = factory;
    }

    /**
     * <p>Getter for the field <code>settings</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.impl.ConfigFactorySettings} object
     */
    public ConfigFactorySettings getSettings()
        {
        return settings;
        }

    /**
     * <p>Getter for the field <code>logger</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigLoggingInterface} object
     */
    public ConfigLoggingInterface getLogger()
        {
        return logger;
        }

    /**
     * <p>Setter for the field <code>logger</code>.</p>
     *
     * @param logger a {@link org.metabit.platform.support.config.interfaces.ConfigLoggingInterface} object
     */
    public void setLogger(ConfigLoggingInterface logger)
        {
        this.logger = logger;
        }

    /**
     * <p>Getter for the field <code>configStorages</code>.</p>
     *
     * @return a {@link java.util.Map} object
     */
    public Map<String, ConfigStorageInterface> getConfigStorages()
        {
        return configStorages;
        }

    /**
     * <p>Setter for the field <code>configStorages</code>.</p>
     *
     * @param configStorages a {@link java.util.Map} object
     */
    public void setConfigStorages(Map<String, ConfigStorageInterface> configStorages)
        {
        this.configStorages = configStorages;
        }

    /**
     * <p>Getter for the field <code>searchList</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.impl.ConfigSearchList} object
     */
    public ConfigSearchList getSearchList() { return searchList; } // needs a reference which can be changed.

    /**
     * <p>Getter for the field <code>configFormats</code>.</p>
     *
     * @return a {@link java.util.Map} object
     */
    public Map<String, ConfigFormatInterface> getConfigFormats()
        {
        return configFormats;
        }

    /**
     * <p>Setter for the field <code>configFormats</code>.</p>
     *
     * @param configFormats a {@link java.util.Map} object
     */
    public void setConfigFormats(Map<String, ConfigFormatInterface> configFormats)
        {
        this.configFormats = configFormats;
        }

    /**
     * <p>Getter for the field <code>configSecretsProviders</code>.</p>
     *
     * @return a {@link java.util.Map} object
     */
    public Map<String, ConfigSecretsProviderInterface> getConfigSecretsProviders()
        {
        return configSecretsProviders;
        }

    /**
     * <p>Setter for the field <code>configSecretsProviders</code>.</p>
     *
     * @param configSecretsProviders a {@link java.util.Map} object
     */
    public void setConfigSecretsProviders(Map<String, ConfigSecretsProviderInterface> configSecretsProviders)
        {
        this.configSecretsProviders = configSecretsProviders;
        }

    /**
     * <p>Getter for the field <code>classLoader</code>.</p>
     *
     * @return a {@link java.lang.ClassLoader} object
     */
    public ClassLoader getClassLoader()
        {
        return classLoader;
        }

    /**
     * <p>Setter for the field <code>classLoader</code>.</p>
     *
     * @param classLoader a {@link java.lang.ClassLoader} object
     */
    public void setClassLoader(ClassLoader classLoader)
        {
        this.classLoader = classLoader;
        }

    /**
     * <p>Getter for the field <code>sourceChangeNotifier</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.impl.SourceChangeNotifier} object
     */
    public SourceChangeNotifier getSourceChangeNotifier()
        {
        return sourceChangeNotifier;
        }

    /**
     * <p>Setter for the field <code>sourceChangeNotifier</code>.</p>
     *
     * @param sourceChangeNotifier a {@link org.metabit.platform.support.config.impl.SourceChangeNotifier} object
     */
    public void setSourceChangeNotifier(SourceChangeNotifier sourceChangeNotifier)
        {
        this.sourceChangeNotifier = sourceChangeNotifier;
        }

    /**
     * get the scheme repository.
     * @return the scheme repository.
     */
    public ConfigSchemeRepository getSchemeRepository()
        {
        return schemeRepository;
        }

// formats
}
