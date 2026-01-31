package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.metabit.platform.support.config.impl.entry.BlobConfigEntryLeaf;

/**
 * Configuration type for accessing BLOBs.
 * <br/>
 * Special handling, different from regular Configuration instances:
 * 1. It has just a single entry, named "" (empty string). This entry can be accessed as byte[] or String.
 * String is using UTF-8 conversion.
 * 2. by default, the configuration name is used as full file name when accessing files, so it should include any extensions.
 * 3. It buffers the contents in RAM. Keep in mind where the "L" in BLOB matters.
 * <p>
 * NB: This is not a big great feature of mConfig. This is a sideline, to allow
 * for some useful functionality without breaking the overall thing. Use only
 * where necessary.
 *
 * @version $Id: $Id
 */
public class BlobConfiguration implements LayeredConfigurationInterface
{
    private static final List<ConfigScope>         fromSpecificToGeneric;
    private final        String                    name;
    private final        ConfigLoggingInterface    logger;
    private final        EnumSet<ConfigScope>      scopes;
    private final        List<BlobConfigInternal>  configs;
    private final        Map<ConfigLocation, Path> paths;
    private final        ConfigFactoryInstanceContext ctx;
    private              boolean                   closed = false;

    static
        {
        // prepare scope
        fromSpecificToGeneric = Arrays.asList(ConfigScope.values());
        Collections.reverse(fromSpecificToGeneric);
        }


    /**
     * <p>Constructor for BlobConfiguration.</p>
     *
     * @param sanitizedConfigName a {@link String} object
     * @param ctx                 a {@link ConfigFactoryInstanceContext} object
     * @param configFactory       a {@link ConfigFactory} object
     */
    public BlobConfiguration(String sanitizedConfigName, ConfigFactoryInstanceContext ctx, ConfigFactory configFactory, EnumSet<ConfigScope> scopes)
        {
        this.name = sanitizedConfigName;
        // prepared
        this.logger = ctx.getLogger();
        this.ctx = ctx;
        this.scopes = scopes;
        this.configs = new ArrayList<>();
        this.paths = new HashMap<>();
        // Future auto-discovery planned
/*
        take the configName
        iterate/add some extensions, if the settings contain any
        go and check the config sources for a file of matching name
        collect matching files, keep them, sortable by scopes
        find the most specific scope
*/
        // if we don't have any existing match that's OK to! So we can create it
        // when there is write access. (dont forget dummy byte[0] contents)
        // there is always ONE entry we pick and prefer; writing to a different
        // scope than the one it has may cause that to switch, though.

        // maybe use a WeakReference or something for storing the blob?
        }

    private void checkClosed()
        {
        if (closed)
            {
            throw new IllegalStateException("Configuration " + name + " is closed");
            }
        }

    @Override
    public boolean isClosed()
        {
        return closed;
        }

    @Override
    public String getConfigName()
        {
        return this.name;
        }

    /**
     * {@inheritDoc}
     * <p>
     * get a config entry.
     */
    @Override
    public ConfigEntry getConfigEntryFromFullKey(String fullKey, EnumSet<ConfigScope> scopes)
        {
        checkClosed();
        if (fullKey == null) throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID);
        // "" empty string is the only one we know/accept here.
        if (!fullKey.isEmpty()) throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_REFUSED);
        // preconditions checked.

        try {
            byte[] blobData = getBlob(scopes);
            if (blobData == null) {
                throw new ConfigException(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY);
            }
            return new BlobConfigEntryLeaf(fullKey, blobData);
        } catch (ConfigCheckedException e) {
            logger.warn("Failed to read blob entry: " + e.getMessage(), e);
            throw new ConfigException(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY);
        }
        }

    /** {@inheritDoc} */
    @Override
    public void setConfigScheme(ConfigScheme scheme)
        {
        logger.warn("no config schemes for BLOBs");
        // ignore the attempt.
        }

    /**
     * {@inheritDoc}
     * <p>
     * check whether this configuration is writable
     */
    @Override
    public boolean isWriteable()
        {
        // is at least one of the configs we've found writeable?
        for (BlobConfigInternal cfg : configs)
            {
            if (cfg.isWriteable()) return true; // at least one writeable
            }
        // next: is at least one of the *directories* (paths) writeable?
        for (Map.Entry<ConfigLocation, Path> entry : paths.entrySet())
            {
            if (entry.getValue().toFile().canWrite())
                return true;
            }
        // no existing file, none of the directories can be written to?
        return false;
        }

    /**
     * {@inheritDoc}
     * <p>
     * flush all write caches, if write-buffering is activated
     */
    @Override
    public int flush()
            throws ConfigCheckedException
        {
        checkClosed();
        return 0;
        }

    /**
     * {@inheritDoc}
     * <p>
     * flush all read caches, so subsequent reads get the most recent state of
     * configuration source contents
     */
    @Override
    public boolean reload()
            throws ConfigCheckedException
        {
        // No persistent cache; reads fresh from sources on access.
        return false;
        }

    /**
     * {@inheritDoc}
     * <p>
     * check for empty configurations
     */
    @Override
    public boolean isEmpty()
        { return configs.isEmpty(); // across all scopes
        }

/*
with normal LayeredConfiguration,
this is called from the Storage instances, function "tryToReadConfigurationLayers"
which, in turn, is called from line ~258 in the DefaultConfigFactory:
            ConfigStorageInterface storage = location.getStorage();
            storage.tryToReadConfigurationLayers(sanitizedConfigName, location, layeredCfg);
within the     private Configuration getConfig(String configName, ConfigScheme configScheme, EnumSet<ConfigScope> scopes)
function.

so, for blobs, we need a corresponding getConfig() thing which calls corresponding
    //            storage.tryToReadBlobConfigurations(sanitizedConfigName,location, blobCfg);

 */

    /** {@inheritDoc} */
    @Override
    public void add(ConfigLayerInterface singleConfig, ConfigLocation location)
        {
        logger.warn("ConfigLayerInterface add() not to be used with BLOBs?");
        return;
        }

    /**
     * get the list of all source locations the layers refer to.
     *
     * @return a list of ConfigLocation entries
     */
    @Override
    public List<ConfigLocation> getSourceLocations()
        {
        List<ConfigLocation> locations = new ArrayList<>();
        for (BlobConfigInternal config : configs)
            { locations.add(config.getLocation()); }
        return locations;
        }

    @Override
    public ConfigFactoryInstanceContext getContext()
    {
        return ctx;
    }

    @Override
    public void close()
            throws Exception
        {
        if (closed)
            {
            return;
            }
        flush();
        closed = true;
        }

    /**
     * <p>Getter for the field <code>logger</code>.</p>
     *
     * @return a {@link ConfigLoggingInterface} object
     */
    public ConfigLoggingInterface getLogger() { return logger; }

    public byte[] getBlob()
            throws ConfigCheckedException
        {
        return this.getBlob(this.scopes);
        }

    /**
     * get a BLOB, filtered by scope.
     *
     * @return the byte[] contents, or null if no match is found.
     */
    public byte[] getBlob(final EnumSet<ConfigScope> acceptableScopes)
            throws ConfigCheckedException
        {
        checkClosed();
        // ConfigScope.values() is ordered from most generic to most specific.
        // for our first-match approach here, we want the opposide direction.
        for (ConfigScope scope : fromSpecificToGeneric)
            {
            if (!acceptableScopes.contains(scope)) continue; // skip if the scope is not wanted.
            // "scope" is the most specific, acceptable scope now. try to find a matching entry
            for (BlobConfigInternal config : configs)
                {
                if (config.getScope() == scope)
                    {
                    // now we try to read it, according to type.
                    if (config.isFile())
                        {
                        try // try to read BLOB from file.
                            {
                            Path file = config.getFilePath();
                            // isWriteable...
                            return Files.readAllBytes(file);
                            }
                        catch (IOException ex)
                            {
                            logger.error(ex.getMessage(), ex);
                            // and continue with the next possible entry!
                            }
                        }
                    else // is URL stream
                        {
                        try
                            {
                            URL url = config.getURL();
                            InputStream inStream = url.openStream();
                            //JDK9: byte[] result = inStream.readAllBytes();
                            byte[] result = inputStreamReadAllBytes(inStream);
                            inStream.close();
                            return result;
                            }
                        catch (IOException ex)
                            {
                            logger.error(ex.getMessage(), ex);
                            // and continue with the next possible entry!
                            }
                        }
                    }
                }
            }
        return null;
        }

    // for JDK8 compatibility.
    public static byte[] inputStreamReadAllBytes(InputStream in)
            throws IOException
        {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream())
            {
            byte[] buffer = new byte[1024]; // Buffer size, usual default
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
                { byteArrayOutputStream.write(buffer, 0, bytesRead); }
            return byteArrayOutputStream.toByteArray();
            }
        // exception return here.
        }

    /**
     * <p>putBlob.</p>
     *
     * @param bytes an array of {@link byte} objects
     * @param scope a {@link ConfigScope} object
     */
    public void putBlob(final byte[] bytes, ConfigScope scope)
        {
        this.putBlob(bytes, EnumSet.of(scope));
        }

    /**
     * <p>putBlob.</p>
     *
     * @param bytes  an array of {@link byte} objects
     * @param scopes a {@link EnumSet} object
     * @return true if successful, false on failure
     */
    public boolean putBlob(final byte[] bytes, EnumSet<ConfigScope> scopes)
        {
        checkClosed();
        for (ConfigScope scope : fromSpecificToGeneric)
            {
            if (!scopes.contains(scope)) continue; // skip if the scope is not wanted.

            for (BlobConfigInternal config : configs)
                {
                if (!(config.getScope() == scope))
                    continue; // skipping because scope does not match
                if (!config.writeable)
                    {
                    logger.debug("BLOB in matching scope found, but not writeable");
                    continue;  // skip
                    }
                if (config.isFile())
                    {
                    try
                            {
                            Files.write(config.getFilePath(), bytes);
                        logger.debug("successfully written BLOB");
                        }
                    catch (IOException e)
                        {
                        throw new ConfigException(e);
                        }
                    }
                else
                    {
                    logger.warn("writing to URLs is not supported");
                    }

                }
            // no existing entry found. let's try to create a new entry at this scope
            for (Map.Entry<ConfigLocation, Path> entry : paths.entrySet())
                {
                if (entry.getKey().getScope() != scope)
                    continue;

                File directory = entry.getValue().toFile();
                if (directory.canWrite())
                    {
                    logger.debug("BLOB creating for scope "+scope+" at "+directory.getAbsolutePath()+"seems possible.");
                    Path newFile = entry.getValue().resolve(name);
                    try
                        {
                        Files.write(newFile, bytes);
                        }
                    catch (IOException e)
                        {
                        throw new ConfigException(e);
                        }
                    return true;
                    }
                }
            }
        logger.debug("failed to write BLOB, no matching scope found");
        return false;
        }


    /**
     * {@inheritDoc}
     * <p>
     * iterator to get all valid keys. Will do a tree walk over all
     * config sources which permit iteration; all content from sources
     * which do not will be invisible for this, but still effective.
     */
    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        return null;
        }

    public List<String> getFilenameExtensions()
        {
        return Arrays.asList("", ".bin"); // JDK9+: List.of("");
        }

    public void addFile(final ConfigLocation location, Path file, boolean writeable)
        {
        // source contains Scope
        logger.info("BLOB\t"+location.getScope()+"\tin file "+file);
        this.configs.add(new BlobConfigInternal(location, file, writeable));
        }

    public void addURLstream(final URL url, ConfigLocation location)
        {
        logger.info("BLOB\t"+location.getScope()+"\t, URL at "+url);
        this.configs.add(new BlobConfigInternal(location, url, false));
        }

    public void addPath(ConfigLocation location, Path locationPath)
        {
        this.paths.put(location, locationPath);
        }


    private static class BlobConfigInternal
    {
        private final ConfigScope    scope;
        private final ConfigLocation location;
        private final Path           filepath;
        private final URL            url;
        private final boolean        writeable;

        public BlobConfigInternal(ConfigLocation location, Path file, boolean writeable)
            {
            this.scope = location.getScope();
            this.location = location;
            this.filepath = file;
            this.url = null;
            this.writeable = writeable;
            }

        public BlobConfigInternal(ConfigLocation location, URL url, boolean writeable)
            {
            this.scope = location.getScope();
            this.location = location;
            this.filepath = null;
            this.url = url;
            this.writeable = writeable;
            }

        ConfigScope getScope() { return scope; }

        ConfigLocation getLocation() { return location; }

        public boolean isFile() { return (filepath != null); } // else URL

        public Path getFilePath() { return filepath; }

        public URL getURL() { return url; }

        public boolean isWriteable() { return writeable; }
    }
}
//___EOF___
