package org.metabit.platform.support.config.scheme.impl.ext;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.BlobConfiguration;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>SchemeDefaultConfigStorage class.</p>
 *
 * 
 * @version $Id: $Id
 */
public class SchemeDefaultConfigStorage implements ConfigStorageInterface
{
    /**
     * {@inheritDoc}
     *
     * get the name.
     * this should be static, but Java doesn't allow static members on interfacing.
     */
    @Override
    public String getStorageName()
        {
        return "scheme-defaults";
        }

    /**
     * {@inheritDoc}
     *
     * get the ID.
     * this should be static, but Java doesn't allow static members on interfacing.
     */
    @Override
    public String getStorageID()   { return "defaults"; }


    /**
     * {@inheritDoc}
     *
     * test whether initializing a config source for use would work.
     */
    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        { return true; }

    /**
     * {@inheritDoc}
     *
     * initialize the config source. It may *write* its own additions to the ctx.
     * So we need to explicitly pass a reference.
     */
    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        { return true; }

    /**
     * {@inheritDoc}
     *
     * cleanup after use / close access handles.
     * This should not throw exceptions; if possible, perform all cleanup quietly.
     * exit, clean up and free handles after use.
     */
    @Override
    public void exit()
        { }

    /** {@inheritDoc} */
    @Override
    public ConfigStorageInterface clone()
            throws CloneNotSupportedException
        {
        super.clone();
        return null;
        }

    /**
     * {@inheritDoc}
     *
     * has this storage the possibility to be written to at all?
     * it still depends on the individual Configuration, but some storage locations are not writeable at all.
     */
    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
        }

    /** {@inheritDoc} */
    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        if (configLocation.getStorage() != this)
            throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
        String keyString = (key == null) ? "" : URLEncoder.encode(key, StandardCharsets.UTF_8);
        String locationString = URLEncoder.encode(configLocation.toLocationString(), StandardCharsets.UTF_8);
        String uristring = String.format("mconfig:schemedefaults/%s/%s", locationString, keyString); //@CHECK
        if (optionalFragment != null)
            uristring += "#"+URLEncoder.encode(optionalFragment, StandardCharsets.UTF_8);
        return URI.create(uristring);
        }

    /** {@inheritDoc} */
    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        throw new ConfigException(new UnsupportedOperationException()); // nope, we don't do that.
        }

    /**
     * {@inheritDoc}
     *
     * schemes have no dynamic creation/writing. This should never be called since schemes already state they're not for write access,
     * thus not for create, either.
     */
    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        { throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR); }

    /**
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param location            location to look in
     * @param blobConfig          the blob config we're working on
     */
    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        throw new ConfigException(new UnsupportedOperationException()); // nope, we don't do that.
        }

    /** {@inheritDoc} */
    @Override
    public java.util.Set<org.metabit.platform.support.config.ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        return java.util.Collections.emptySet();
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        return false; // schemes don't change at runtime. @CHECK or do they? if they do, we need to keep a flag.
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        // schemes don't change at runtime
        }

    /*
     * writing is done one at a time
     *
     * @param configEntry
     * @return
    @Override
    public boolean putEntry(ConfigEntry configEntry)
        {
        throw new ConfigException(ConfigException.ConfigExceptionReason.NO_WRITEABLE_LOCATION); // nope, we don't do that. the scheme is read as a whole.
        }
     */

}
//___EOF___

