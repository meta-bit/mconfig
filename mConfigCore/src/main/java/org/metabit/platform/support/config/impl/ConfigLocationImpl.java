package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/**
 * this implements both ConfigLocation and ConfigSource;
 * with ConfigSource being the "private" side of the interface.
 * Since we cannot declare parts of an interface public and another part protected,
 * we use this more complicated approach instead.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigLocationImpl implements ConfigLocation, ConfigSource
{
    private ConfigScope            scope;
    private ConfigStorageInterface storage;
    private Object                 storageInstanceHandle;
    private StorageInstanceType    storageInstanceType;
    private ConfigFormatInterface  storageFormat; //@CHECK usefulness
    private ConfigLayerInterface   layer;
    private final boolean          isWriteable;

    enum StorageInstanceType { FILE, URI, OTHER };

    /**
     * constructor setting all elements
     *
     * @param scope               scope this location is located in
     * @param storageThisIsFrom   storage this location is in/from.
     * @param formatUsedInStorage the format used in the storage. optional - may be null.
     * @param handleUsedByStorage handle used by the storage internally,  optional - may be null.
     */
    public ConfigLocationImpl(ConfigScope scope, ConfigStorageInterface storageThisIsFrom, ConfigFormatInterface formatUsedInStorage, Object handleUsedByStorage)
        {
        if ((scope == null)||(storageThisIsFrom == null)) throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
        this.scope = scope;
        this.storage = storageThisIsFrom;
        setStorageInstance(handleUsedByStorage);
        this.isWriteable = storage.isGenerallyWriteable();
        this.storageFormat = formatUsedInStorage;
        this.layer = null;
        }

    /**
     * <p>Constructor for ConfigLocationImpl.</p>
     *
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @param layer a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     * @param format a {@link org.metabit.platform.support.config.interfaces.ConfigFormatInterface} object
     * @param storageInstanceHandle a {@link java.lang.Object} object
     */
    public ConfigLocationImpl(final ConfigLocation location, ConfigLayerInterface layer, ConfigFormatInterface format, Object storageInstanceHandle)
        {
        this.scope = location.getScope();
        this.storage = location.getStorage();
        setStorageInstance(storageInstanceHandle);
        this.storageFormat = format;
        this.layer = layer;
        this.isWriteable = location.isWriteable();
        }


    private void setStorageInstance(Object handleUsedByStorage)
        {
        this.storageInstanceHandle = handleUsedByStorage;
        if (storageInstanceHandle == null)
            { storageInstanceType = StorageInstanceType.OTHER;}
        else
            {
            if      (storageInstanceHandle instanceof URI)   storageInstanceType = StorageInstanceType.URI;
//            else if (storageInstanceHandle instanceof File)  storageInstanceType = StorageInstanceType.FILE;
            else if (storageInstanceHandle instanceof Path)  storageInstanceType = StorageInstanceType.FILE;
            else                                             storageInstanceType = StorageInstanceType.OTHER;
            }
        return;
        }

    // or should we call it "resolve", as java.io.Path does?
    public ConfigLocation derive(final Path file)
        {
        return new ConfigLocationImpl(this.scope, this.storage, this.storageFormat, file);
        }

    public ConfigLocation derive(final URI uri)
        {
        return new ConfigLocationImpl(this.scope, this.storage, this.storageFormat, uri);
        }

    /** {@inheritDoc} */
    @Override
    public String toLocationString()
        {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("config:[").append(scope.toString()).append("]:").append(storage.getStorageName()).append(":");
        if (storageInstanceHandle != null)
            {
            String handleStr = storageInstanceHandle.toString();
            // If it's the storage itself, and toString() is the default Object.toString(), we might want to skip it
            // but we added toString() to our storages.
            
            boolean isStorageItself = (storageInstanceHandle == storage);
            
            if (!handleStr.startsWith("/") && !handleStr.startsWith("\\") && !handleStr.contains(":") && !isStorageItself)
                {
                stringBuilder.append("/");
                }
            
            switch (storageInstanceType)
                {
                case FILE:
                case URI:
                    stringBuilder.append(handleStr);
                    break;
                case OTHER:
                    if (isStorageItself)
                        {
                        stringBuilder.append("/").append(handleStr);
                        }
                    else
                        {
                        stringBuilder.append("/").append(storageInstanceHandle.getClass().getSimpleName());
                        }
                    break;
                default:
                    throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
                }
            }
        else
            {
            stringBuilder.append("/");
            }
        return stringBuilder.toString();
        }

    /** {@inheritDoc} */
    @Override
    public String toString()
        {
        return toLocationString();
        }


    /**
     * {@inheritDoc}
     * <p>
     * get the URI for a given key in this location.
     *
     * @param key              the key to get the URI for
     * @param optionalFragment an optional fragment to append to the URI. null for empty.
     *
     */
    @Override
    public URI getURI(String key, String optionalFragment)
        {
        if (storageInstanceHandle == null)
            { return storage.getURIforConfigLocation(this, key, optionalFragment); }
        switch (storageInstanceType)
            {
            case FILE:
                return ((Path) storageInstanceHandle).toUri();
            case URI:
                return ((URI) storageInstanceHandle);
            case OTHER:
                return storage.getURIforConfigLocation(this, key, optionalFragment);
            default:
                throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
            }
        }


    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof ConfigLocation)) return false;
        ConfigLocation that = (ConfigLocation) o;
        // Compare storage and handle. Scope might be different if derived but it should be the same for equality in notifier.
        return Objects.equals(getStorage(), that.getStorage()) &&
                Objects.equals(getStorageInstanceHandle(), that.getStorageInstanceHandle());
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(getStorage(), getStorageInstanceHandle());
        }

    /** {@inheritDoc} */
    @Override
    public ConfigScope getScope() { return scope; }

    /**
     * <p>Setter for the field <code>scope</code>.</p>
     *
     * @param scope a {@link org.metabit.platform.support.config.ConfigScope} object
     */
    public void setScope(ConfigScope scope)
        {
        this.scope = scope;
        }

    /**
     * <p>Getter for the field <code>storage</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigStorageInterface} object
     */
    public ConfigStorageInterface getStorage() { return storage; }

    /** {@inheritDoc} */
    @Override
    public Object getStorageInstanceHandle() { return storageInstanceHandle; }

    /** {@inheritDoc} */
    @Override
    public ConfigFormatInterface getStorageFormat() { return this.storageFormat; }

    /**
     * {@inheritDoc}
     *
     * get the precise layer instance this entry is stored in.
     */
    @Override
    public ConfigLayerInterface getLayer() { return layer; }

    /** {@inheritDoc} */
    @Override
    public boolean hasChangedSincePreviousCheck()
        { return storage.hasChangedSincePreviousCheck(storageInstanceHandle); }

    /**
     * <p>isWriteable.</p>
     *
     * @return a boolean
     */
    public boolean isWriteable()         { return isWriteable; }

    // -----

    /**
     * <p>Setter for the field <code>layer</code>.</p>
     *
     * @param layer a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     */
    public void setLayer(final ConfigLayerInterface layer) { this.layer = layer; }

    /**
     * <p>Setter for the field <code>storage</code>.</p>
     *
     * @param storage a {@link org.metabit.platform.support.config.interfaces.ConfigStorageInterface} object
     */
    public void setStorage(ConfigStorageInterface storage) { this.storage = storage; }

    /**
     * <p>Setter for the field <code>storageFormat</code>.</p>
     *
     * @param storageFormat a {@link org.metabit.platform.support.config.interfaces.ConfigFormatInterface} object
     */
    public void setStorageFormat(ConfigFormatInterface storageFormat)
        { this.storageFormat = storageFormat; }

}
//___EOF___

