package org.metabit.platform.support.config.source.hardcoded;
import org.metabit.platform.support.config.ConfigDiscoveryInfo;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.provider.ConfigSchemeProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * mConfig Source for configurations "hardcoded" to the JAR file the Java program is running from.
 * This assumes the program is inside a JAR and checks specific paths in the resource/ directory of the JAR.
 * <br/>
 * Nota Bene:
 * a single JAR may combine contents of several other JARs.
 * Thus, it is desirable to check first for application-specific subdirectories
 * and fall back to general ones only if there was nothing specific.
 * 1. src/main/resources/ .config/COMPANY/APPLICATION/CONFIGNAME.EXTENSION
 * 2. src/main/resources/ .config/APPLICATION/CONFIGNAME.EXTENSION
 * only if both fail
 * 3. src/main/resources/ .config/CONFIGNAME.EXTENSION
 * which is usually good enough.
 */
public class JARConfigSource implements ConfigStorageInterface
{
    public static final String                 CONFIG_RESOURCE_PATH_PREFIX = ".config/";
    private             ConfigLoggingInterface logger;
    private ArrayList<ConfigFormatInterface>   fileFormats;
    private             ClassLoader            classLoader;
    private             String[]               searchPaths;

    /**
     * get the name.
     * this should be static, but Java doesn't allow static members on interfacing.
     *
     * @return the storage name (human use)
     */
    @Override
    public String getStorageName()
        {
        return "JARConfigSource";
        }

    /**
     * get the ID.
     * this should be static, but Java doesn't allow static members on interfacing.
     *
     * @return the storage ID (programmatic use; only regular identifier characters allowed.
     */
    @Override
    public String getStorageID()
        {
        return "JAR";
        }

    /**
     * test whether initializing a config source for use would work.
     *
     * @param settings settings to use
     * @param logger   logger to use
     * @return true if successful and the config source is usable, false if there was a problem and it should not be used.
     */
    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        URL myself = JARConfigSource.class.getResource("JARConfigSource.class");
        if (myself == null)
            {
            logger.warn("Java module access issues: JARConfigSource class cannot access itself");
            return false;
            }
        String protocol = myself.getProtocol();
        if (protocol == null)
            return false;
        if ("jar".equalsIgnoreCase(protocol)) // URL starting with "jar:"
            {
            return true;
            }
        // specialized protocols for some application servers and frameworks
        if ("vfs".equalsIgnoreCase(protocol) || "bundle".equalsIgnoreCase(protocol))
            {
            return true;
            }
        // only in test mode, we allow this
        if (settings.getBoolean(ConfigFeature.TEST_MODE))
            {
            return true;
            }

        // reason: if it weren't, one could override software defaults by simply placing a config file in the right place.
        logger.info("JAR config source refuses running outside of a JAR (protocol: " + protocol + ").");
        return false;
        }

    /**
     * initialize the config source. It may *write* its own additions to the ctx.
     * So we need to explicitly pass a reference.
     *
     * @param ctx reference to the ConfigFactoryInstanceContext object in use
     * @return true if OK, false if there was an issue, and the config source should be removed from future use.
     */
    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.logger = ctx.getLogger();
        this.classLoader = ctx.getClassLoader();
        this.fileFormats = new ArrayList<>();
        // attach fileformats
        ctx.getConfigFormats().values().forEach(format->
            {
            // check whether it is a file format. if not, skip. if it is, store in class member
            if (format instanceof ConfigFileFormatInterface)
                {
                ConfigFileFormatInterface fileformat = (ConfigFileFormatInterface) format;
                logger.debug("file format found: "+fileformat.getFormatID());
                this.fileFormats.add(fileformat);
                }
            });


        String companyName = ctx.getSettings().getString(ConfigFeature.COMPANY_NAME);
        String applicationName = ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME);
        String subDir = ctx.getSettings().getString(ConfigFeature.SUB_PATH);
        if (subDir == null)
            subDir = "";

        String companyAppPath;
        if (companyName == null || companyName.trim().isEmpty())
            {
            companyAppPath = null;
            }
        else
            {
            companyAppPath = CONFIG_RESOURCE_PATH_PREFIX + companyName + "/" + applicationName + "/";
            }
        String appPath = CONFIG_RESOURCE_PATH_PREFIX + applicationName + "/";
        String genericPath = CONFIG_RESOURCE_PATH_PREFIX;
        if (!subDir.isEmpty())
            {
            if (companyAppPath != null)
                companyAppPath += subDir + "/";
            appPath += subDir + "/";
            }
        List<String> sp = new ArrayList<>();
        if (companyAppPath != null)
            sp.add(companyAppPath);
        sp.add(appPath);
        sp.add(genericPath);
        this.searchPaths = sp.toArray(new String[0]);

        for (int i = 0; i < searchPaths.length; i++)
            {
            logger.debug("JAR search path " + (i + 1) + ": " + searchPaths[i]);
            }


        //@TODO format really null?
        ConfigLocation ourLocation = new ConfigLocationImpl(ConfigScope.PRODUCT,this, null, this); // or the JAR handle instead of null
        ctx.getSearchList().insertAtScopeEnd(ourLocation, ConfigScope.PRODUCT);
        
        // Also add locations for the specific search paths so discovery can find them if it iterates locations
        for (String path : searchPaths)
            {
            ctx.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.PRODUCT, this, null, path), ConfigScope.PRODUCT);
            }

        return true;
        }

    @Override
    public ConfigStorageInterface clone() throws CloneNotSupportedException
        {
        super.clone();
        return null;
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return false;
        }

    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        { throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR); }



    private void scanResources(String resourcePath, ResourceProcessor processor)
        {
        if (!resourcePath.endsWith("/"))
            {
            resourcePath += "/";
            }

        try
            {
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            while (resources.hasMoreElements())
                {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("jar".equalsIgnoreCase(protocol))
                    {
                    scanJarResources(url, resourcePath, processor);
                    }
                else if ("file".equalsIgnoreCase(protocol))
                    {
                    scanDirectoryResources(new File(url.toURI()), resourcePath, processor);
                    }
                }
            }
        catch (Exception e)
            {
            logger.warn("Failed to scan resources at " + resourcePath + ": " + e.getMessage());
            }
        }

    private void scanJarResources(URL jarUrl, String resourcePath, ResourceProcessor processor) throws IOException
        {
        String jarPath = jarUrl.getPath();
        if (jarPath.startsWith("file:"))
            {
            jarPath = jarPath.substring(5);
            }
        int bangIndex = jarPath.indexOf('!');
        if (bangIndex != -1)
            {
            jarPath = jarPath.substring(0, bangIndex);
            }
        jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);

        try (JarFile jarFile = new JarFile(jarPath))
            {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
                {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(resourcePath) && !entry.isDirectory())
                    {
                    processor.process(name, () -> jarFile.getInputStream(entry));
                    }
                }
            }
        }

    private void scanDirectoryResources(File dir, String resourcePath, ResourceProcessor processor) throws IOException
        {
        File[] files = dir.listFiles();
        if (files == null)
            {
            return;
            }
        for (File file : files)
            {
            if (file.isDirectory())
                {
                scanDirectoryResources(file, resourcePath + file.getName() + "/", processor);
                }
            else
                {
                String name = resourcePath + file.getName();
                processor.process(name, () -> java.nio.file.Files.newInputStream(file.toPath()));
                }
            }
        }

    @FunctionalInterface
    private interface ResourceProcessor
        {
        void process(String name, InputStreamSupplier inputStreamSupplier) throws IOException;
        }

    @FunctionalInterface
    private interface InputStreamSupplier
        {
        InputStream get() throws IOException;
        }

    @Override
    public java.util.Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        java.util.Set<ConfigDiscoveryInfo> foundConfigs = new java.util.HashSet<>();
        Object handle = location.getStorageInstanceHandle();
        
        List<String> pathsToScan = new ArrayList<>();
        if (handle instanceof String)
            {
            pathsToScan.add((String) handle);
            }
        else if (handle == this)
            {
            for (String path : searchPaths)
                {
                pathsToScan.add(path);
                }
            }
        
        for (String resourcePath : pathsToScan)
            {
            scanResources(resourcePath, (name, inputStreamSupplier) ->
                {
                String fileName = name.substring(name.lastIndexOf('/') + 1);
                if (isGhostFile(fileName))
                    {
                    return;
                    }
                for (ConfigFormatInterface format : fileFormats)
                    {
                    if (format instanceof ConfigFileFormatInterface)
                        {
                        ConfigFileFormatInterface fileFormat = (ConfigFileFormatInterface) format;
                        for (String ext : fileFormat.getFilenameExtensions())
                            {
                            String dotExt = ext.startsWith(".") ? ext : "." + ext;
                            if (fileName.endsWith(dotExt))
                                {
                                String configName = fileName.substring(0, fileName.length() - dotExt.length());
                                URI uri = getURIforConfigLocation(location, configName, null);
                                foundConfigs.add(new ConfigDiscoveryInfo(configName, location.getScope(), uri, fileFormat.getFormatID(), false));
                                break;
                                }
                            }
                        }
                    }
                });
            }
        return foundConfigs;
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        return false; // JAR files don't change while the program is running.
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        // JAR content doesn't change during runtime
        }

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        if (configLocation.getStorage() != this)
            throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
        String keyString = (key == null) ? "" : URLEncoder.encode(key, StandardCharsets.UTF_8);
        String locationString = URLEncoder.encode(configLocation.toLocationString(), StandardCharsets.UTF_8);
        String uristring = String.format("mconfig:staticdefaults/JAR/%s/%s", locationString, keyString); //@CHECK
        if (optionalFragment != null)
            uristring += "#"+URLEncoder.encode(optionalFragment, StandardCharsets.UTF_8);
        return URI.create(uristring);
        }

    @Override
    public void exit()
        {
        }

    private String extractConfigNameFromPath(String name)
        {
        String fileName = name;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash != -1)
            {
            fileName = name.substring(lastSlash + 1);
            }
        if (fileName.endsWith(".scheme.json"))
            {
            return fileName.substring(0, fileName.length() - ".scheme.json".length());
            }
        return null;
        }

    private String readStreamToString(InputStream is) throws IOException
        {
        byte[] buffer = new byte[4096];
        int bytesRead;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        while ((bytesRead = is.read(buffer)) != -1)
            {
            baos.write(buffer, 0, bytesRead);
            }
        return baos.toString(StandardCharsets.UTF_8);
        }

    /**
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param location            location to look in
     * @param blobConfig          the blob config we're working on
     */
    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        // for reading resources from within JARs, using the thread-own class loader by default.
        ClassLoader cl = this.classLoader;
        List<String> extensions = blobConfig.getFilenameExtensions();
        for (final String extension : extensions)
            {
            final String combinedConfigName = sanitizedConfigName+extension;

            for (String path : searchPaths)
                {
                URL url = cl.getResource(path + combinedConfigName);
                if (url == null)
                    {
                    logger.debug("config \"" + combinedConfigName + "\" not found within JAR at " + path);
                    continue;
                    }
                try
                    {
                    URLConnection conn = url.openConnection();
                    if (conn instanceof JarURLConnection)
                        {
                        logger.trace("attempting to read config from JAR: " + url.toURI());
                        }
                    else
                        {
                        logger.trace("config BLOB found via protocol " + url.getProtocol() + ": " + url.toURI());
                        }
                    // we allow reading from any URL protocol that classloader found
                    blobConfig.addURLstream(url, location);
                    }
                catch (IOException | URISyntaxException ex)
                    {
                    logger.error("unexpected issue while reading from inside JAR", ex);
                    throw new ConfigException(ex);
                    }
                }
            }
        return;
        }
    /**
     * @param sanitizedConfigName the config name, sanitized.
     * @param location            the location we're looking at
     * @param cfgCollector        configuration object to store the results in
     */
    @Override
    public void tryToReadConfigurationLayers(final String sanitizedConfigName, final ConfigLocation location, LayeredConfigurationInterface cfgCollector)
        {
        // for reading resources from within JARs, using the thread-own class loader by default.
        ClassLoader cl = this.classLoader;


        // iterate through all locations
        for (ConfigFormatInterface format : fileFormats)
            {
            ConfigFileFormatInterface fileFormat = (ConfigFileFormatInterface) format;
            List<String> extensions = fileFormat.getFilenameExtensions();
            for (final String extension : extensions)
                {
                ConfigLayerInterface cfg = attemptToReadConfigOrReturnNull(cl, location, fileFormat, sanitizedConfigName+extension);
                if (cfg != null)
                    cfgCollector.add(cfg, location);
                }
            }
        return;
        }

    private ConfigLayerInterface attemptToReadConfigOrReturnNull(ClassLoader cl, final ConfigLocation location, ConfigFileFormatInterface fileFormat, final String combinedConfigName) // final String locationString, final String fileName, final ConfigLocationImpl location, ConfigFileFormatInterface fileformat)
        {
        for (String path : searchPaths)
            {
            ConfigLayerInterface cli = getConfigLayerInterfaceInner(cl, location, fileFormat, path + combinedConfigName);
            if (cli != null)
                {
                return cli;
                }
            }
        return null;
        }

    private ConfigLayerInterface getConfigLayerInterfaceInner(ClassLoader cl, ConfigLocation location, ConfigFileFormatInterface fileFormat, String pathname)
        {
        //@TODO change to a list of patterns
        URL url = cl.getResource(pathname);
        // ClassLoader.getResource searches in
        if (logger.isDebugEnabled())
            logger.debug(String.format("trying to read config from JAR resources: /%s at %s", pathname, location.toLocationString()));
        if (url == null)
            {
            logger.debug("config \""+pathname+"\" not found within JAR");
            return null;
            }
        try
            {
            URLConnection conn = url.openConnection();
            if (conn instanceof JarURLConnection)
                {
                logger.trace("attempting to read config from JAR: "+url.toURI());
                }
            else
                {
                logger.trace("config found via protocol "+url.getProtocol()+": "+url.toURI());
                }
            // writeable is false for streams. We don't plan to write JARs from within.
            return fileFormat.readStream(conn.getInputStream(), location);
            }
        catch (IOException|URISyntaxException ex)
            {
            logger.error("unexpected issue while reading from inside JAR", ex);
            throw new ConfigException(ex);
            }
        }

    @Override
    public String toString()
        {
        return "JAR Resources";
        }
    private boolean isGhostFile(String fileName)
        {
        if (fileName.endsWith("~") || fileName.endsWith(".bak") || fileName.endsWith(".swp") || fileName.endsWith(".tmp") || "Thumbs.db".equalsIgnoreCase(fileName))
            {
            return true;
            }
        return false;
        }

}
//___EOF___
