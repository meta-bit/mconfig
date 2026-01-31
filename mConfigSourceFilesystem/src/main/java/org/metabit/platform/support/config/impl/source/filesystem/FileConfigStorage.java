package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.osdetection.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * configuration source: read configuration files from filesystem.
 * The thing people tend to think as the main config source still.
 *
 * .d directory support:
 *  - discovers <configName>.d/ directories.
 *  - Fragments are sorted lexicographically.
 *  - Fragments take higher precedence than the main configuration file.
 *  - Ghost files (e.g., *.bak, Thumbs.db) are automatically filtered out.
 *  - The implementation is non-recursive to match standard Linux conventions.
 *  - Multiple formats within the same .d folder are supported, respecting the priority list mentioned above.
 *
 */
public class FileConfigStorage implements ConfigStorageInterface
{
    private static final EnumMap<OperatingSystem, SearchPathInitializer> SEARCH_PATH_INITIALIZERS = new EnumMap<>(OperatingSystem.class);

    static
        {
        SEARCH_PATH_INITIALIZERS.put(OperatingSystem.LINUX, new LinuxSearchPaths());
        SEARCH_PATH_INITIALIZERS.put(OperatingSystem.ANDROID, new AndroidSearchPaths());
        SEARCH_PATH_INITIALIZERS.put(OperatingSystem.WINDOWS, new WindowsSearchPaths());
        SEARCH_PATH_INITIALIZERS.put(OperatingSystem.MACOS, new MacOsSearchPaths());
        }

    private ConfigLoggingInterface               logger;
    private List<ConfigFileFormatInterface>      fileFormats; // an ordered map would be nicer.
    private ArrayList<ConfigFileFormatInterface> readFormatList;
    private ArrayList<ConfigFileFormatInterface> writeFormatList;
    private FileChangeWatcher                    fileChangeWatcher;

    @Override
    public String getStorageName()
        {
        return "FileConfigStorage";
        }

    @Override
    public String getStorageID()
        {
        return "files";
        }

    public boolean test(final ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        //@TODO test whether the filesystem is accessible for us.
        //@IMPROVEMENT support multiple filesystems, when/where this makes sense.
        // return false if it isn't.
        // FileSystemProvider fsp;
        // FileSystem         fs;
        // check settings we're going to use later.

        return true;
        }

    /**
     * initialize; specifically: fill the search list with standardized and
     * usual paths to search for configurations in, specific to the
     * Operating System we're running on.
     * <br/>
     * Previous versions checked for path existence and omitted non-existent ones.
     * Corrected: relevant content may turn up later, while the program is running.
     * Paths stay a valid ConfigLocation in the search list, even though there is no instance in the Layers.
     * This is a situation where the difference between ConfigLocation and ConfigSource is important.
     *
     * @param ctx internal context
     * @return whether the search list remained empty, or not.
     */
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        // this.ctx = ctx;
        this.logger = ctx.getLogger();
        this.fileFormats = new ArrayList<>();
        try
            {
            this.fileChangeWatcher = new FileChangeWatcher(ctx);
            }
        catch (IOException e)
            {
            logger.error("could not create file watcher", e);
            return false;
            }
        ConfigFactorySettings settings = ctx.getSettings();

        Map<String, ConfigFileFormatInterface> formatMap = new HashMap<>(); //@CHECK permanent field?
        // attach fileformats
        ctx.getConfigFormats().values().forEach(format->
            {
            // check whether it is a file format. if not, skip. if it is, store in class member
            if (format instanceof ConfigFileFormatInterface)
                {
                ConfigFileFormatInterface fileformat = (ConfigFileFormatInterface) format;
                logger.debug("file format found: "+fileformat.getFormatID());
                this.fileFormats.add(fileformat);
                formatMap.put(fileformat.getFormatID().toUpperCase(), fileformat); // ignore case for matching, using all caps internally.
                }
            });

        // init search list
        ConfigSearchList searchList = ctx.getSearchList();
        // fill search list for test mode, or for normal operation
        if (ctx.getSettings().getBoolean(ConfigFeature.TEST_MODE))
            {
            String companyName = ctx.getSettings().getString(ConfigFeature.COMPANY_NAME);
            String applicationName = ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME);
            String subDir = ctx.getSettings().getString(ConfigFeature.SUB_PATH);
            final EnumMap<ConfigScope, List<String>> additionalDirs = convertStringListToConfigScopeEnumMap(settings.getStrings(ConfigFeature.TESTMODE_DIRECTORIES));
            if (!initTestModeFileSearchLocations(ctx.getSearchList(), companyName, applicationName, subDir, additionalDirs))
                {
                logger.error("none of the configuration file directories is accessible");
                // we continue nevertheless; other config sources may take over.
                }
            // SECURITY: in TEST_MODE, we intentionally DO NOT use ADDITIONAL_USER_DIRECTORIES
            // or ADDITIONAL_RUNTIME_DIRECTORIES to avoid accidental modification of real
            // production configuration data. Only directories specifically designated for tests
            // (e.g., via TESTMODE_DIRECTORIES or derived from src/test) are used.
            }
        else // normal operation.
            {
            OperatingSystem os = ctx.getSettings().getObject(ConfigFeature.CURRENT_PLATFORM_OS, OperatingSystem.class);
            String companyName = ctx.getSettings().getString(ConfigFeature.COMPANY_NAME);
            String applicationName = ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME);
            String subDir = ctx.getSettings().getString(ConfigFeature.SUB_PATH);
            try
                {
                if (!initFileSearchLocations(ctx.getSearchList(), os, companyName, applicationName, subDir))
                    {
                    logger.error("none of the configuration file directories is accessible");
                    // we continue nevertheless; other config sources may take over.
                    }
                }
            catch (InvalidPathException|NullPointerException e)
                {
                logger.error("critical error during search location initialization - some default paths might be missing", e);
                }
            // use settings.
            prependToSearchlistScope(settings, ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, searchList, ConfigScope.RUNTIME);
            // now the same for the USER
            prependToSearchlistScope(settings, ConfigFeature.ADDITIONAL_USER_DIRECTORIES, searchList, ConfigScope.USER);
            }

        // map format preferences for reading and writing from settings to ordered lists
        List<String> fileFormatReadPreferenceList = settings.getStrings(ConfigFeature.FILE_FORMAT_READING_PRIORITIES);
        boolean fallbackFlag = settings.getBoolean(ConfigFeature.FILE_FORMAT_READING_ALLOW_ALL_FORMATS);
        readFormatList = new ArrayList<ConfigFileFormatInterface>();
        initFormatPreferenceList(fileFormatReadPreferenceList, readFormatList, formatMap, fallbackFlag);

        logger.debug("Read formats: "+readFormatList.stream().map(f->f.getFormatID()).collect(java.util.stream.Collectors.joining(",")));

        List<String> fileFormatWritePreferenceList = settings.getStrings(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES);
        fallbackFlag = settings.getBoolean(ConfigFeature.FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS);
        writeFormatList = new ArrayList<ConfigFileFormatInterface>();
        initFormatPreferenceList(fileFormatWritePreferenceList, writeFormatList, formatMap, fallbackFlag);

        // done.
        return (!searchList.isEmpty());
        }


    private void initFormatPreferenceList(List<String> formatPreferenceList, List<ConfigFileFormatInterface> targetList, Map<String, ConfigFileFormatInterface> formatMap, boolean fallbacksActive)
        {
        // look up the entries in the preferred order, and store them
        formatPreferenceList.forEach(formatName->
            {
            ConfigFileFormatInterface entry = formatMap.get(formatName.toUpperCase());
            if (entry != null)
                { targetList.add(entry); }
            });
        // if we use fallbacks, look up all the others, and append them at the end, in no particular order.
        if (fallbacksActive)
            {
            this.fileFormats.forEach(format->
                {
                if (!targetList.contains(format))
                    targetList.add(format);
                });
            }
        return;
        }

    /**
     * clone the instance.
     *
     * @return a clone
     */
    @Override
    public ConfigStorageInterface clone()
            throws CloneNotSupportedException
        {
        ConfigStorageInterface configStorageInterface = (ConfigStorageInterface) super.clone();
        return configStorageInterface;
        }

    /**
     * has this storage the possibility to be written to at all?
     * it still depends on the individual Configuration, but some storage locations are not writeable at all.
     *
     * @return true if the chance exists that configs may be written; false if no attempt need to be made.
     */
    @Override
    public boolean isGenerallyWriteable()
        { return true; } // files can be writeable; it makes sense to try in the specific cases.

    /**
     * get URI for ConfigLocation within this ConfigStorage.
     *
     * @param configLocation   the location to convert
     * @param key              entry key within the location
     * @param optionalFragment optional fragment, may be null
     * @return URI formatted from the parameters
     */
    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        if (configLocation.getStorage() != this)
            throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
        Path path = (Path) configLocation.getStorageInstanceHandle();
        if (key != null && !key.isEmpty())
            {
            path = path.resolve(key);
            }
        String uristring = path.toAbsolutePath().toUri().toString();
        if (optionalFragment != null && !optionalFragment.isEmpty())
            {
            uristring += "#"+URLEncoder.encode(optionalFragment, StandardCharsets.UTF_8);
            }
        return URI.create(uristring);
        }

    /**
     * try to read config.
     * also, take notes whether it complies with the Scheme, and whether it is writeable
     *
     * @param sanitizedConfigName
     * @param location
     * @param layeredCfg          layered config collection to collect this into. -- abstract to ConfigCollection?
     */
    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation location, LayeredConfigurationInterface layeredCfg)
        {
        // 1. test entry to match with this source.
        if (location.getStorage() != this)
            {
            logger.debug("non-matching source called for config reading"); // caller should check this before!
            return; // not for us
            }
        // 2. go looking.
        // we may cast, because "this" = us is who set the storage instance handle in the first place, so we know what type it is.
        Path locationPath = (Path) location.getStorageInstanceHandle();
        // das ist eine URI! String locationPath2 = source.toLocationString();
        // shouldn't this be in the location, and be returned by location.get
        // iterate through all locations
        // use the prioritized read-list

        // First: the Main File (lowest priority)
        for (ConfigFileFormatInterface fileFormat : readFormatList)
            {
            List<String> extensions = fileFormat.getFilenameExtensions();
            for (final String extension : extensions)
                {
                String filename = sanitizedConfigName+extension;
                ConfigLayerInterface cfg = attemptToReadConfigOrReturnNull(locationPath, filename, location, fileFormat);
                if (cfg != null)
                    {
                    // layeredCfg does not store the location, it checks the scope only; so we don't need to derive the actual ConfigLocation here.
                    layeredCfg.add(cfg, location);
                    }
                else // else add watch to get it when it turns up later?
                    {
                    try
                        {
                        // ... or invalid format in the file!
                        Path nonExistingFile = locationPath.resolve(sanitizedConfigName+extension);
                        fileChangeWatcher.addFile(nonExistingFile, location);
                        }
                    catch (InvalidPathException e) // for added safety
                        {
                        logger.warn("InvalidPathException when adding a Path for later access", e);
                        }
                    }
                }
            }

        // Second: the .d Fragments (higher priority)
        Path dotDPath = locationPath.resolve(sanitizedConfigName + ".d");
        File dotDDir = dotDPath.toFile();
        if (dotDDir.exists() && dotDDir.isDirectory())
            {
            File[] files = dotDDir.listFiles(File::isFile);
            if (files != null && files.length > 0)
                {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File fragment : files)
                    {
                    String fragmentName = fragment.getName();
                    if (isGhostFile(fragmentName))
                        {
                        continue;
                        }
                    boolean foundFormat = false;
                    // For fragments, we also respect the format priorities if multiple extensions match, 
                    // but we need to check which format this specific file matches.
                    for (ConfigFileFormatInterface fileFormat : readFormatList)
                        {
                        for (String extension : fileFormat.getFilenameExtensions())
                            {
                            if (fragmentName.endsWith(extension))
                                {
                                ConfigLayerInterface cfg = attemptToReadConfigOrReturnNull(dotDPath, fragmentName, location, fileFormat);
                                if (cfg != null)
                                    {
                                    layeredCfg.add(cfg, location);
                                    }
                                foundFormat = true;
                                break;
                                }
                            }
                        if (foundFormat)
                            {
                            break;
                            }
                        }
                    }
                }
            // Watch the .d directory for new fragments
            fileChangeWatcher.addDirectory(dotDPath);
            }
        else
            {
            // Watch for the .d directory to be created
            // fileChangeWatcher.addFile(dotDPath, location); // cannot watch directory via addFile
            }

        return;
        }

    /**
     * @param configName   name of the configuration, also used as filename if applicable
     * @param location     location to create it in
     * @param configScheme the ConfigScheme to use, defining the file format and possible entries
     * @param layeredCfg   the layeredCfg to apply it to
     * @return newly instantiated layer interface, or null
     */
    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredCfg)
        {
        // 1. test entry to match with this source.
        if (location.getStorage() != this)
            {
            logger.debug("non-matching source called for config creation"); // caller should check this before!
            return null;
            }
        // 2. go looking.
        Path locationPath = (Path) location.getStorageInstanceHandle();

        // Use all available file formats for creation, giving preference to writeFormatList
        List<ConfigFileFormatInterface> formatsToTry = new ArrayList<>(writeFormatList);
        for (ConfigFileFormatInterface f : fileFormats)
            {
            if (!formatsToTry.contains(f))
                formatsToTry.add(f);
            }

        for (ConfigFileFormatInterface fileformat : formatsToTry)
            {
            List<String> extensions = fileformat.getFilenameExtensions();
            for (final String extension : extensions)
                {
                ConfigLayerInterface cfg = attemptToCreateConfigOrReturnNull(locationPath, configName+extension, location, fileformat);
                if (cfg != null)
                    {
                    // Re-set the source for the created layer to have a more precise location
                    // For single-file configurations, the derived location handle should point to the directory, 
                    // so that further relative resolutions (like in RawWriteTest) work correctly.
                    ConfigLocation derivedLocation = location.derive(locationPath);
                    layeredCfg.add(cfg, derivedLocation);
                    return cfg;
                    }
                }
            }

        logger.debug("File storage could not create requested configuration \""+configName+"\"");
        return null;
        }

    /**
     * @param sanitizedConfigName the name of the configuration; may be used as filename
     * @param searchLocation      location to look in / where to get it from
     * @param blobConfig          the blob config we're working on
     */
    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation searchLocation, BlobConfiguration blobConfig)
        {
        // 1. test entry to match with this source.
        if (searchLocation.getStorage() != this)
            {
            logger.debug("non-matching source called for config reading"); // caller should check this before!
            return; // not for us
            }
        // 2. go looking.
        // we may cast, because "this" = us is who set the storage instance handle in the first place, so we know what type it is.
        Path locationPath = (Path) searchLocation.getStorageInstanceHandle();
        // shouldn't this be in the location, and be returned by location.get
        blobConfig.addPath(searchLocation, locationPath);

        // iterate through all locations
        // use the prioritized read-list

        // BLOBs, by definition, don't do File Formats.
        List<String> extensions = blobConfig.getFilenameExtensions();
        for (final String extension : extensions)
            {
            try
                {
                // attempt to read file (not layer)
                Path file = locationPath.resolve(sanitizedConfigName+extension);
                if (file.toFile().exists())
                    {
                    if (!file.toFile().isFile())
                        {
                        logger.warn("BLOB at "+file+" is not a file");
                        continue;
                        }
                    if (!file.toFile().canRead())
                        {
                        logger.warn("BLOB at "+file+" cannot be read");
                        continue;
                        }
                    boolean writeable = file.toFile().canWrite();
                    // the location of the file itself, not where we were searching
                    ConfigLocation actualLocation = searchLocation.derive(file);
                    blobConfig.addFile(actualLocation, file, writeable);
                    }
                }
            catch (InvalidPathException e) // for added safety
                {
                logger.warn("InvalidPathException when trying to access a file BLOB", e);
                }
            }
        return;
        }

    @Override
    public java.util.Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        Set<ConfigDiscoveryInfo> foundConfigs = new HashSet<>();
        Object handle = location.getStorageInstanceHandle();
        if (handle instanceof Path)
            {
            Path directory = (Path) handle;
            File dirFile = directory.toFile();
            if (dirFile.isDirectory() && dirFile.canRead())
                {
                File[] files = dirFile.listFiles();
                if (files != null)
                    {
                    for (File file : files)
                        {
                        String fileName = file.getName();
                        if (isGhostFile(fileName))
                            {
                            continue;
                            }
                        java.util.List<ConfigFileFormatInterface> formatListToUse = this.readFormatList;
                        if (formatListToUse.isEmpty())
                            {
                            formatListToUse = this.fileFormats;
                            }

                        if (file.isFile())
                            {
                            // we need to strip the extension to get the config name.
                            // but which extensions? Ideally those we have formats for.
                            for (ConfigFileFormatInterface format : formatListToUse)
                                {
                                for (String ext : format.getFilenameExtensions())
                                    {
                                    String dotExt = ext.startsWith(".") ? ext : "."+ext;
                                    if (fileName.endsWith(dotExt))
                                        {
                                        String configName = fileName.substring(0, fileName.length()-dotExt.length());
                                        URI uri = getURIforConfigLocation(location, configName, null);
                                        foundConfigs.add(new ConfigDiscoveryInfo(configName, location.getScope(), uri, format.getFormatID(), file.canWrite()));
                                        break;
                                        }
                                    }
                                }
                            }
                        else if (file.isDirectory() && fileName.endsWith(".d"))
                            {
                            String configName = fileName.substring(0, fileName.length() - 2);
                            URI uri = getURIforConfigLocation(location, configName, null);
                            // For .d directories, we don't have a single format.
                            // We can use a special format ID or just pick one if we want to indicate it's a config.
                            // However, .d by itself is not a format.
                            foundConfigs.add(new ConfigDiscoveryInfo(configName, location.getScope(), uri, "DIR", file.canWrite()));
                            }
                        }
                    }
                }
            }
        return foundConfigs;
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        if (storageInstanceHandle instanceof Path)
            return fileChangeWatcher.hasChanged((Path) storageInstanceHandle);
        return false;
        }

    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        if (storageInstanceHandle instanceof Path)
            fileChangeWatcher.processFileChangeEvent((Path) storageInstanceHandle);
        }

    private ConfigLayerInterface attemptToCreateConfigOrReturnNull(Path fileLocation, String fileName, ConfigLocation location, ConfigFileFormatInterface fileformat)
        {
        try
            {
            //  das ist eine URI locationString2 = location.toLocationString();
            Path fileWithPath = fileLocation.resolve(fileName);
            File file = fileWithPath.toFile();
            if (file.exists())
                {
                logger.warn("strange behaviour: trying to re-create existing file instead of reading it");
                return null;
                }
            if (!fileWithPath.getParent().toFile().canWrite())
                {
                logger.debug("config file directory \""+fileWithPath.toAbsolutePath()+"\" not writeable");
                return null; // not here, try another location.
                }
            // OK, all ready - now let's try to create a file, by file format.
            logger.trace("attempting to create file "+file.getAbsolutePath().toString());
            ConfigLayerInterface layerInstance = fileformat.createFile(fileWithPath, location);
            fileChangeWatcher.addFile(fileWithPath, location); // + layerInstance?
            return layerInstance;
            }
        catch (InvalidPathException ex)
            {
            logger.debug("config file does not exist: \""+fileLocation+"\"::\""+fileName+"\""); //@TODO security check? both parts had been sanitized before.
            return null;
            }
        catch (SecurityException ex)
            {
            logger.debug("we're forbidden from accessing config file: \""+fileLocation+"\"::\""+fileName+"\""); //@TODO security check? both parts had been sanitized before.
            return null;
            }
        }

    private void prependToSearchlistScope(ConfigFactorySettings settings, ConfigFeature additionalUserDirectories, ConfigSearchList searchList, ConfigScope defaultScope)
        {
        String subpath = settings.getString(ConfigFeature.SUB_PATH);
        if (subpath == null) subpath = ""; // avoid nullPtr errors
        if (settings.isSet(additionalUserDirectories))
            {
            // iterate through our parameters; resolve, and if valid, insert. for the correct order in the result, we do this backwards.
            List<String> dirnames = settings.getStrings(additionalUserDirectories);
            ListIterator<String> liter = dirnames.listIterator(dirnames.size());
            final Pattern regex = Pattern.compile("(\\w+):(.+)", Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);

            while (liter.hasPrevious())
                {
                String current = liter.previous();
                try // add at front of scope to give higher priority for finding/writing
                    {
                    ConfigScope scope = defaultScope;
                    String pathname = current;

                    Matcher m = regex.matcher(current);
                    if (m.matches())
                        {
                        try
                            {
                            scope = ConfigScope.valueOf(m.group(1).toUpperCase());
                            pathname = m.group(2);
                            }
                        catch (IllegalArgumentException e)
                            {
                            // ignore, treat as full path
                            }
                        }

                    Path path = Paths.get(pathname);
                    if (!subpath.isEmpty())
                        { path = path.resolve(subpath); }
                    addDirectoryToSearchListIfAccessible(searchList, path, scope, false);
                    }
                catch (InvalidPathException ex)
                    {
                    logger.warn("user-supplied search path invalid: \""+current+"\"");
                    }
                }
            }
        return;
        }

    public void exit()
        {
        // clean up directory handles and file handles, insofar in use.
        if (fileChangeWatcher == null)
            {
            return;
            }
        try
            {
            fileChangeWatcher.cleanup(); // also shutdown included
            }
        catch (IOException e)
            {
            logger.error("file change watcher closed with exception", e);
            }
        }


    private ConfigLayerInterface attemptToReadConfigOrReturnNull(final Path fileLocation, String fileName, final ConfigLocation location, ConfigFileFormatInterface fileformat)
        {
        try
            {
            Path filePath = fileLocation.resolve(fileName);
            File file = filePath.toFile();
            fileChangeWatcher.addFile(filePath, location); // if successful, add to watches . + contentLayer as parameter?
            if (file.exists() == false)
                {
                // this is quite normal.
                logger.trace("failed attempt to access file :\""+filePath.toAbsolutePath()+"\"");
                return null;
                }
            if (file.isFile() == false)
                {
                logger.warn("strange behaviour: directory specified as config file:\""+filePath.toAbsolutePath()+"\"");
                return null;
                }
            if (file.canRead() == false)
                {
                logger.warn("config file exists, but cannot be read :\""+filePath.toAbsolutePath()+"\"");
                return null;
                }
            // OK, all ready - now let's try to read the file. Which is another thing, depending on the file format.
            logger.trace("attempting to read config file "+file.getAbsolutePath());
            ConfigLayerInterface contentLayer = fileformat.readFile(file, location);
            return contentLayer;
            }
        catch (InvalidPathException ex)
            {
            logger.debug("config file does not exist: \""+fileLocation+"\"::\""+fileName+"\""); //@TODO security check? both parts had been sanitized before.
            return null;
            }
        catch (SecurityException ex)
            {
            logger.debug("we're forbidden from accessing config file: \""+fileLocation+"\"::\""+fileName+"\""); //@TODO security check? both parts had been sanitized before.
            return null;
            }
        }

    public boolean initTestModeFileSearchLocations(ConfigSearchList searchList, final String companyName, final String applicationName, String subDir, EnumMap<ConfigScope, List<String>> additionalTestDirs)
        {
        int successes = 0;
        // third: scoped testmode directories, as explicitly added/specified in settings.
        if (additionalTestDirs != null)
            {
            try // type check implicit here
                {
                for (Map.Entry<ConfigScope, List<String>> entry : additionalTestDirs.entrySet())
                    {
                    for (String dir : entry.getValue())
                        {
                        Path searchPath = Paths.get(dir);
                        if ((subDir != null) && (!subDir.isEmpty()))
                            searchPath = searchPath.resolve(subDir);
                        final ConfigScope scope = entry.getKey();
                        searchList.insertAtScopeStart(new ConfigLocationImpl(scope, this, null, searchPath), scope);
                        successes++;
                        }
                    }
                }
            catch (ClassCastException ex)
                {
                logger.error("illegal parameter for directories has been smuggled in", ex);
                return false; // cannot throw exception here, but stop the init.
                }
            }

        // check whether we can access ./src/test at all.
        Path mavenTestBaseDirPath = Paths.get(".", "src", "test");
        File mavenTestBaseDir = mavenTestBaseDirPath.toFile();
        if (!mavenTestBaseDir.exists())
            {
            logger.debug("no test resources found at \""+mavenTestBaseDirPath+"\"");
            }
        else if (!mavenTestBaseDir.canRead())
            {
            logger.warn("test resource directory at \""+mavenTestBaseDirPath+"\" cannot be read");
            }
        else
            {
            // first: the application defaults at ./src/test/config/<COMPANYNAME>/<APPLICATIONNAME>/ for the application-wide, pseudo-generic configs.
            // these are entered at APPLICATION scope. The "config" is to mirror the paths on *ix systems, but without the leading dot for test mode ease.
            Path companyNameRelative;
            if (companyName == null || companyName.trim().isEmpty())
                {
                companyNameRelative = Paths.get("config", applicationName);
                }
            else
                {
                companyNameRelative = Paths.get("config", companyName, applicationName);
                }
            if ((subDir != null) && (!subDir.isEmpty()))
                companyNameRelative = companyNameRelative.resolve(subDir);
            Path applicationTestConfigs = mavenTestBaseDirPath.resolve(companyNameRelative);
            logger.debug("checking TEST_MODE application directory \""+applicationTestConfigs+"\"");
            searchList.insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.APPLICATION, this, null, applicationTestConfigs), ConfigScope.APPLICATION);
            successes++;

            // second: test-specific ./src/test/resources/config with subdirectories, subdirectories using scope name in caps
            Path mavenTestResourceDirectory2 = Paths.get(".", "src", "test", "resources", "config");
            for (ConfigScope scope : ConfigScope.values()) // relying on guaranteed order of Java enums
                {
                Path subdirRelativePath = mavenTestResourceDirectory2.resolve(scope.name());
                if ((subDir != null) && (!subDir.isEmpty()))
                    subdirRelativePath = subdirRelativePath.resolve(subDir);
                logger.debug("checking TEST_MODE scope "+scope.name()+" directory \""+subdirRelativePath+"\"");
                if (scope == ConfigScope.SESSION || scope == ConfigScope.RUNTIME)
                    searchList.insertAtScopeEnd(new ConfigLocationImpl(scope, this, null, subdirRelativePath), scope);
                else
                    searchList.insertAtScopeStart(new ConfigLocationImpl(scope, this, null, subdirRelativePath), scope);
                successes++;
                }
            }


        // evaluation
        if (successes == 0)
            {
            logger.warn("none of the test configuration directories was accessible!");
            return false;
            }
        return true;
        }


    /**
     * Initializes the file search locations for application configuration files based on the operating system
     * conventions.
     * It populates the provided {@code ConfigSearchList} with default paths *locations*.
     * These are not checked for existence at this time; if they come into existence later, fine.
     *
     * @param searchList      the list of configuration locations to populate with default system paths.
     * @param os              the operating system on which the application is running. This is used to determine
     *                        OS-specific paths for configuration files.
     * @param companyName     the name of the company to include in constructing configuration file paths where relevant.
     * @param applicationName the application name, used to construct configuration directory paths.
     * @param subDir          an optional subdirectory to append to the constructed configuration file paths;
     *                        if {@code null}, an empty string will be used as the default.
     * @return {@code true} if at least one configuration directory was successfully added to the search list;
     *         {@code false} if no configuration directories could be added.
     */
    public boolean initFileSearchLocations(ConfigSearchList searchList, final OperatingSystem os, final String companyName, final String applicationName, String subDir)
        {
        if (subDir == null)
            subDir = "";

        SearchPathInitializer initializer = SEARCH_PATH_INITIALIZERS.get(os);
        if (initializer == null)
            {
            initializer = new DefaultSearchPaths();
            }
        initializer.initSearchPaths(searchList, companyName, applicationName, subDir, this);

        if (searchList.isEmpty())
            {
            logger.warn("default configuration directory initialization failed");
            return false;
            }
        return true;
        }



    private EnumMap<ConfigScope, List<String>> convertStringListToConfigScopeEnumMap(List<String> additionalDirsUnparsed)
        {
        final Pattern regex = Pattern.compile("(\\w+):(.+)", Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
        // init empty result map
        EnumMap<ConfigScope, List<String>> additionalDirs = new EnumMap<>(ConfigScope.class);
        for (ConfigScope scope : ConfigScope.values())
            { additionalDirs.put(scope, new ArrayList<>()); }
        // parse input strings
        if (additionalDirsUnparsed == null)
            {
            logger.warn("TESTMODE_DIRECTORIES set but empty");
            return additionalDirs;
            }
        for (String entry : additionalDirsUnparsed)
            {
            Matcher m = regex.matcher(entry);
            if (m.matches())
                {
                try
                    {
                    String scopename = m.group(1);
                    ConfigScope scope = ConfigScope.valueOf(scopename.toUpperCase());
                    String pathname = m.group(2);
                    additionalDirs.get(scope).add(pathname);
                    }
                catch (IllegalArgumentException e)
                    {
                    // if it's not a valid scope, we treat it as a path without scope
                    additionalDirs.get(ConfigScope.RUNTIME).add(entry);
                    }
                }
            else
                {
                // if it doesn't match the regex at all, we treat it as a path without scope
                additionalDirs.get(ConfigScope.RUNTIME).add(entry);
                }
            }
        return additionalDirs;
        }


    /**
     * Safely adds a directory to the search list.
     * This implements the internal logic where the Storage itself is responsible
     * for validating its locations and creating the ConfigLocation instances.
     *
     * @param searchList the list to add to
     * @param path       the filesystem path
     * @param scope      the scope this path represents
     * @param atEnd      true to append at the end of the scope, false to prepend
     * @return true if the directory was accessible and added
     */
    public boolean addDirectoryToSearchListIfAccessible(ConfigSearchList searchList, final Path path, final ConfigScope scope, boolean atEnd)
        {
        try
            {
            File tmp = path.toFile();
            // We add the location even if it doesn't exist (it might turn up later),
            // but we perform basic sanity checks if it DOES exist.
            if (tmp.exists())
                {
                if (!tmp.isDirectory())
                    {
                    logger.info("config path "+path+" is not a directory");
                    return false;
                    }
                if (!tmp.canRead())
                    {
                    logger.info("config directory "+path+" is not readable");
                    return false;
                    }
                }

            // Create the location with a direct reference to 'this' instance
            ConfigLocation location = new ConfigLocationImpl(scope, this, null, path);

            if (atEnd)
                { searchList.insertAtScopeEnd(location, scope); }
            else
                { searchList.insertAtScopeStart(location, scope); }

            return true;
            }
        catch (UnsupportedOperationException|InvalidPathException ex)
            {
            logger.warn("Exception during directory access check for "+path, ex);
            return false;
            }
        }

    private boolean isGhostFile(String fileName)
        {
        if (fileName.endsWith("~") || fileName.endsWith(".bak") || fileName.endsWith(".swp") || fileName.endsWith(".tmp") || "Thumbs.db".equalsIgnoreCase(fileName))
            {
            return true;
            }
        return false;
        }

    /**
     * Safely adds a path to the search list if the base path is not null.
     *
     * @param searchList the search list to add to
     * @param basePath   the base path (usually from an environment variable)
     * @param scope      the scope for the new location
     * @param more       additional path components
     */
    void addPathToSearchList(ConfigSearchList searchList, String basePath, ConfigScope scope, String... more)
        {
        // guard clauses / parameter validation: skip null/blank segments
        List<String> segments = new ArrayList<>();
        if (basePath != null)
            {
            String baseTrimmed = basePath.trim();
            if (!baseTrimmed.isEmpty())
                segments.add(baseTrimmed);
            }
        for (String m : more)
            {
            if (m != null)
                {
                String mTrimmed = m.trim();
                if (!mTrimmed.isEmpty())
                    segments.add(mTrimmed);
                }
            }
        if (segments.isEmpty())
            { return; }
        // application of parameters
        try
            {
            Path path = Paths.get(segments.get(0));
            for (int i = 1; i < segments.size(); i++)
                path = path.resolve(segments.get(i));
            searchList.insertAtScopeEnd(new ConfigLocationImpl(scope, this, null, path), scope);
            }
        catch (InvalidPathException|NullPointerException e)
            {
            if (logger != null)
                {
                logger.warn("could not add default search location for scope " + scope + " due to invalid path: " + basePath, e);
                }
            }
        }

    void logInfo(String message)
        {
        if (logger != null)
            { logger.info(message); }
        }

}
//___EOF___
