package org.metabit.platform.support.config;

import org.metabit.platform.support.osdetection.OperatingSystem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * features which can be set for mConfig
 * <p>
 * implementation note: any changes here must be reflected with default settings in ConfigFactoryBuilder.initDefaultSettings().
 *
 * @version $Id: $Id
 */
public enum ConfigFeature
{
    /**
     * company, organization, or similar name for the entity creating/using the software for which we want configurations.
     * mandatory value, required by OS conventions.
     */
    COMPANY_NAME,           // string
    /**
     * name of the software for which we want configurations.
     * mandatory value, required by OS conventions.
     */
    APPLICATION_NAME,       // string
    /**
     * optional sub-paths (subdirectory or subdirectories, branches etc.)
     * to be applied below the APPLICATION layer for more detailed hierarchy.
     * Will be converted and applied by the ConfigSources depending on their
     * respective address scheme. Also, limitations depending on ConfigSource.
     * <p>
     * The common content pattern for this string is "subpath" for a single,
     * "subpath/secondsubpath" for two, "subpath/second/third" for three path
     * elements, and so on. use "" or null for none.
     * (regular) spaces are allowed in the names.
     *
     */
    SUB_PATH,
    /**
     * the user ID the program runs under. not necessarily the ID of the person using the software!
     */
    CURRENT_USER_ID,
    /**
     * Test mode can be disabled for specific builders, to increase security.
     * By default, your tests can simply set TEST_MODE to `true` and use test mode.
     * If you chose to do so for security reasons, you can disable test mode in your code explicitly.
     * Even if the test code tries to enable it, that will have no effect on the builder
     * configured to ignore it.
     */
    PERMIT_TEST_MODE, // Boolean, default: true
    /**
     * Activate the test mode. In test mode, instead of the normal hierarchy
     * of config sources, a replacement set is used which is searched for in
     * the standard software-project test resource folders.
     * <br/>
     * This flag can be activated via environment variables, too,
     * to better support Integration Testing.
     * For it to have any effect, PERMIT_TEST_MODE must be set.
     */
    TEST_MODE, // Boolean, default: false

    /**
     * test mode directories, with scope.
     * format, for ease of use: SCOPENAME ":" PATH
     * e.g. "USER:/~developer/mylocaltest" or "APPLICATION:/tmp/generatedautomatedtestingdir"
     * multiple entries per scope are possible, and used in order.
     */
    TESTMODE_DIRECTORIES,

    /**
     * a string-to-string map for additional test parameters.
     */
    TESTMODE_PARAMETERS,


    /**
     * ZooKeeper connection string.
     */
    ZOOKEEPER_CONNECT_STRING,
    /**
     * ZooKeeper root path for mConfig.
     */
    ZOOKEEPER_ROOT_PATH,
    /**
     * ZooKeeper session timeout in milliseconds.
     */
    ZOOKEEPER_SESSION_TIMEOUT_MS,
    /**
     * ZooKeeper retry policy base sleep time in milliseconds.
     */
    ZOOKEEPER_RETRY_BASE_SLEEP_MS,
    /**
     * ZooKeeper retry policy maximum number of retries.
     */
    ZOOKEEPER_RETRY_MAX_RETRIES,
    /**
     * The configuration name used to bootstrap ZooKeeper settings (indirectly).
     */
    ZOOKEEPER_BOOTSTRAP_CONFIG_NAME,


    /**
     * reduce logging output the library generates. works separately from the logging config.
     */
    QUIET, // Boolean

    /**
     * service module name of the logging module you want to use;
     * overriding automatic discovery.
     */
    LOGGING_TO_USE_IN_CONFIGLIB,
    /**
     * numerical loglevel, provided to the logging module.
     * Most logging modules have their own filter logic, and will ignore this;
     * but others, like StdErr-logging, use this.
     * Suggested values: 0 is none, 1 logs severe errors, 2 warnings, and so on.
     * The actual meaning is mapped/determined by the respective logging module.
     * default: 2
     */
    LOGLEVEL_NUMBER, // integer


    /**
     * a list of additional directories for the filesystem storage to look in
     * for the RUNTIME scope.
     * <p>
     * Format: <code>[SCOPENAME ":"] PATH</code>
     * e.g. "APPLICATION:C:/myconfig" or simply "/etc/myapp" (defaults to RUNTIME scope)
     * <p>
     * paths starting with "./" as seen as relative to the current working directory.
     * These get priority over the default directories of their respective scope (prepended to search list).
     * Invalid or locally unavailable paths are ignored.
     */
    ADDITIONAL_RUNTIME_DIRECTORIES,
    /**
     * a list of additional directories for the filesystem storage to look in
     * for the USER scope.
     * <p>
     * Format: <code>[SCOPENAME ":"] PATH</code>
     * e.g. "APPLICATION:C:/myconfig" or simply "~/.myapp" (defaults to USER scope)
     * <p>
     * paths starting with "./" as seen as relative to the current working directory.
     * These get priority over the default directories of their respective scope (prepended to search list).
     * Invalid or locally unavailable paths are ignored.
     */
    ADDITIONAL_USER_DIRECTORIES,

    /**
     * when using Windows Registry as config source, this is the root path to use.
     * if not set, it defaults to Software\&lt;companyName&gt;\&lt;applicationName&gt;\.
     * the root path is appended to the Hive (e.g. HKEY_CURRENT_USER).
     */
    REGISTRY_BASE_PATH,


    /**
     * when reading config *files*, this list is the order in which formats are tested for availability.
     * The entries are expected to match the value returned from ConfigFileFormatInterface.getFormatID().
     * <p>
     * Planned: instead of names, it's also possible to give the fully qualified class names.
     */
    FILE_FORMAT_READING_PRIORITIES,
    /**
     * when writing or creating config *files*, this is the order in which formats are tested for their availability.
     * the first in the list is tested first, etc. - earlier entries take precedence over later entries.
     * The entries are expected to match the value returned from ConfigFileFormatInterface.getFormatID().
     * Missing entries are skipped.
     */
    FILE_FORMAT_WRITING_PRIORITIES,  // Strings
    /**
     * when the list of reading prioritized formats is exhausted,
     * continue with those formats not explicitly named on the list?
     * default: true
     */
    FILE_FORMAT_READING_ALLOW_ALL_FORMATS,

    /**
     * when the list of writing prioritized formats is exhausted,
     * continue with those formats not explicitly named on the list?
     * default: true
     */
    FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS,

    /**
     * Defines the order in which config storages (FILE, JAR, registry, etc.) are initialized.
     * This re-orders the ConfigLocation entries in the ConfigSearchList within each scope.
     * Value type: List of Strings.
     */
    STORAGE_TYPE_PRIORITIES,
    /**
     * when the list of prioritized storage types is exhausted,
     * continue with those storages not explicitly named on the list?
     * default: true
     */
    STORAGE_TYPE_ALLOW_ALL_STORAGES,


    /**
     * for text file formats, whether to automatically trim trailing and leading whitespace
     * Usually, they are "typo" errors, and not intended, but can cause problems.
     */
    TRIM_TEXTVALUE_SPACES,

    /**
     * provide all the config Schemes before creating a config factory.
     * parameter type must be Map&lt;String,ConfigScheme&gt;
     */
    CONFIG_SCHEME_LIST,

    /**
     * AKA "pass undefined entries"
     * if a config entry has no defined entry in the Scheme, should we just pass it?
     * set to false for strict mode, if you don't want to allow unchecked data to sneak through.
     * default: true.
     */
    SCHEME_STRICT_MODE,

    /**
     * whether setting a config scheme replaces existing defaults entirely (true),
     * or whether it adds to existing defaults (false).
     * default: false.
     */
    SCHEME_RESETS_DEFAULTS,


    /**
     * use the caller thread's context class loader (on true),
     * default: false
     * context class loader flag takes precedence over system class loader flag,
     */
    USE_CONTEXT_CLASS_LOADER,

    /**
     * use the caller thread's context class loader (on true),
     */
    USE_SYSTEM_CLASS_LOADER,

    /**
     * write cache: should write access not be buffered, but happen immediately (insofar possible?)
     * default: true - so each write is performed immediately.
     * <p>
     * If set to false, writes may be cached until a flush() or close() is called.
     * Whether the write cache is actually employed is implementation detail to the storages and formats;
     * it may make sense for some, less so for others.
     * <br>
     * NB: write access needs a specific Scope to work on; the layered default hierarchy can work without,
     * but writes cannot.
     */
    WRITE_SYNC,
    /**
     * if for the given scope no matching config is found, should mConfig keep searching in the more general
     * scope levels? default: true.
     * <p>
     * If false, only the PRODUCT scope is considered as a fallback (this usually contains defaults).
     */
    FALLBACKS_ACROSS_SCOPES,
    /**
     * when writing, do we stick strictly to the scope specified - or do we allow a fallback?
     * default: false.
     * <p>
     * if true, if no write within the specified scope is possible,
     * we try *more specific* scopes. By default, this goes up to but excluding RUNTIME.
     * Principle of Least Surprise, here: we'd expect written values to be persistent.
     */
    WRITE_FALLBACK_ACROSS_SCOPES,

    /**
     * "automated cache flush frequency", AKA update frequency.
     * How often to check for changes on cached ConfigSources (e.g. files).
     * in milliseconds. Default: 2000
     * set to 0 to turn off automated checks, keep cached contents indefinitely.
     */
    UPDATE_CHECK_FREQUENCY_MS,

    /**
     * By default, text formats are to use a variant suited for human use
     * in regard to formatting, comments, and so on - insofar standards allow.
     * In case a file is never read, debugged, changed, configured by humans
     * (or always a suitable tool at hand), setting this flag allows to use a
     * condensed version of the format, if possible.
     */
    WRITE_CONDENSED_FORMAT,


    /**
     * if accessing a configuration entry does not yield any value at all, then this flag set to true
     * will cause a ConfigException to be thrown. If it is set to false, the null handle or the hard-coded
     * default will be returned.
     * default: true
     */
    EXCEPTION_ON_MISSING_ENTRY,  // Boolean, default: false. instead of returning NULL, thrown an exception.
    /**
     * if a missing entry is encountered and no exception is thrown (see EXCEPTION_ON_MISSING_ENTRY), this
     * flag tells us whether to return a null handle, or the hard-coded default fallback values.
     * true: use hard-coded defaults, false: return null
     * default: false
     */
    DEFAULT_ON_MISSING_ENTRY,
    /**
     * When for a given configuration name/path no configuration can be found at all,
     * the default behaviour is to return an empty Configuration.
     * This flag allows you to trigger a ConfigException automatically instead.
     * default: false
     */
    EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND,
    /**
     * a list of Strings, where you specify additional filename extensions to look for when
     * config filenames are read. String format is:
     */
    FILENAME_EXTENSION_MAPPINGS,

    /**
     * list of module IDs to disable.
     * comma separated list.
     */
    DISABLED_MODULE_IDS,
    /**
     * redirect internal mConfig logging output.
     * values: "stderr", "stdout", "quiet", "file:"+filename
     */
    LOGGING_REDIRECT_TARGET,
    /**
     * enable runtime settings from environment variables. default: false=off.
     * It may be useful for some test and CI/CD environments to enable this
     * but also consider the security implications.
     */
    ALLOW_MCONFIG_RUNTIME_SETTINGS,
    /**
     * enable self-configuration of mConfig via classpath resources. default: true=on.
     * It may be a security issue if the classpath is not "closed".
     */
    ENABLE_SELF_CONFIGURATION,
    /**
     * ID of the secrets provider to be used for secret configuration entries.
     */
    SECRETS_PROVIDER_ID,
    /**
     * Configuration map for the secrets provider.
     */
    SECRETS_PROVIDER_CONFIG,
    /**
     * List of additional secrets providers to be activated.
     * Each entry is a map containing "id", "provider", "config" (optional), and "scope" (optional).
     */
    ADDITIONAL_SECRETS_PROVIDERS,
    /**
     * whether to read comments from configuration files.
     * default: false.
     * Note: mConfig does not guarantee that all comments will be read.
     */
    COMMENTS_READING,
    /**
     * whether to write comments to configuration files.
     * default: false.
     */
    COMMENTS_WRITING,
    /**
     * whether to write the description from the config scheme as a comment
     * when an entry is created.
     * default: false.
     */
    DESCRIPTION_ON_CREATE,
    //------------------------------------------------------------------------------------------------------------------
    // features which are under review; may be removed or changed in future versions before 1.0
    /**
     * global setting to limit the scopes for update checks.
     * default: everything except RUNTIME, because that scope is changed by your own code.
     */
    UPDATE_CHECK_SCOPES,

    /**
     * flag: should configurations be (automatically) created on write access, if not existing?
     * if configuration not found, create it (in the most specific location found writable) - also, create directories.
     */
    AUTOMATIC_CONFIG_CREATION,
    /**
     * flag: should configurations automatically be loaded, before first read access? default: true!
     */
    AUTOMATIC_CONFIG_LOADING,

    /**
     * flag: should configurations be buffered im memory, if possible? default: true.
     * if set to false, each access will cause several config sources to be checked. Consider leaving it enabled,
     * and using the reload/flush call instead.
     */
    CACHE_CONFIGS,

    /**
     * a "free configuration" is one that does not follow a Scheme.
     * NB: this mechanism may change to a setting of Scheme sources/prefixes/... in the future.
     * Boolean, default: true = no Scheme attached.
     */
    FREE_CONFIGURATION, // Scheduled for removal in future.

    /**
     * not implemented yet:
     * flag: do not set default config search paths/directories in local filesystem.
     */
    NO_DEFAULT_DIRECTORIES,

    /**
     * java Charset name for character set to use in text file interpretation.
     * UTF-8 and ISO 8859-1 are the usual choices. Default UTF-8 here, fallback ISO 8859-1
     */
    DEFAULT_TEXTFILE_CHARSET,

    /**
     * Operating System we're running on. Detected automatically.
     */
    CURRENT_PLATFORM_OS,
    /**
     * the hostname of the context the program runs in (hardware, container, ...)
     */
    HOSTNAME,
    /**
     * list of module IDs or file paths to add to the module loading.
     */
    ADDITIONAL_MODULE_PATHS;


    // -------------------------------------------------------------------------

    static
        {
        COMPANY_NAME.valueType = ValueType.STRING;
        APPLICATION_NAME.valueType = ValueType.STRING;
        SUB_PATH.valueType = ValueType.STRING;

        PERMIT_TEST_MODE.valueType = ValueType.BOOLEAN;
        PERMIT_TEST_MODE.defaultValue = Boolean.TRUE;

        TEST_MODE.valueType = ValueType.BOOLEAN;
        TEST_MODE.defaultValue = Boolean.FALSE;

        TESTMODE_PARAMETERS.valueType = ValueType.SPECIAL_CLASS; // Map <String, String>
        TESTMODE_PARAMETERS.classType = Map.class;
        TESTMODE_PARAMETERS.defaultValue = null;

        TESTMODE_DIRECTORIES.valueType = ValueType.STRINGLIST; // EnumMap<Scope,List<String>>
        TESTMODE_DIRECTORIES.defaultValue = List.of(); // List.of(); in JDK9+

        ZOOKEEPER_CONNECT_STRING.valueType = ValueType.STRING;
        ZOOKEEPER_ROOT_PATH.valueType = ValueType.STRING;
        ZOOKEEPER_ROOT_PATH.defaultValue = "/mconfig";
        ZOOKEEPER_SESSION_TIMEOUT_MS.valueType = ValueType.NUMBER;
        ZOOKEEPER_SESSION_TIMEOUT_MS.defaultValue = 60000;
        ZOOKEEPER_RETRY_BASE_SLEEP_MS.valueType = ValueType.NUMBER;
        ZOOKEEPER_RETRY_BASE_SLEEP_MS.defaultValue = 1000;
        ZOOKEEPER_RETRY_MAX_RETRIES.valueType = ValueType.NUMBER;
        ZOOKEEPER_RETRY_MAX_RETRIES.defaultValue = 3;
        ZOOKEEPER_BOOTSTRAP_CONFIG_NAME.valueType = ValueType.STRING;
        ZOOKEEPER_BOOTSTRAP_CONFIG_NAME.defaultValue = "zookeeper";

        CURRENT_PLATFORM_OS.valueType = ValueType.SPECIAL_CLASS;    // this may need an additional entry - which specifically?
        CURRENT_PLATFORM_OS.classType = OperatingSystem.class;

        CURRENT_USER_ID.valueType = ValueType.STRING;
        HOSTNAME.valueType = ValueType.STRING;

        // this applies to all loggers.
        // Tests covered in ConfigFeatureTypeTest
        QUIET.valueType = ValueType.BOOLEAN;
        QUIET.defaultValue = Boolean.FALSE;

        // this is relevant for StdErrLogger only. all other loggers are unaffected, they filter themselves.
        // Tests covered in ConfigFeatureTypeTest
        LOGLEVEL_NUMBER.valueType = ValueType.NUMBER;
        LOGLEVEL_NUMBER.defaultValue = Integer.valueOf(4);
        // if unset, priority of logging modules will tell which one to use (highest numerical priority value)
        LOGGING_TO_USE_IN_CONFIGLIB.valueType = ValueType.STRING;
        LOGGING_TO_USE_IN_CONFIGLIB.defaultValue = "";

        // both default to null.
        ADDITIONAL_RUNTIME_DIRECTORIES.valueType = ValueType.STRINGLIST;
        ADDITIONAL_USER_DIRECTORIES.valueType = ValueType.STRINGLIST;
        REGISTRY_BASE_PATH.valueType = ValueType.STRING;

        TRIM_TEXTVALUE_SPACES.valueType = ValueType.BOOLEAN;
        TRIM_TEXTVALUE_SPACES.defaultValue = Boolean.TRUE;

        USE_CONTEXT_CLASS_LOADER.valueType = ValueType.BOOLEAN;
        USE_CONTEXT_CLASS_LOADER.defaultValue = Boolean.FALSE;

        USE_SYSTEM_CLASS_LOADER.valueType = ValueType.BOOLEAN;
        USE_SYSTEM_CLASS_LOADER.defaultValue = Boolean.FALSE;

        EXCEPTION_ON_MISSING_ENTRY.valueType = ValueType.BOOLEAN;
        EXCEPTION_ON_MISSING_ENTRY.defaultValue = Boolean.TRUE;

        EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND.valueType = ValueType.BOOLEAN;
        EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND.defaultValue = Boolean.FALSE;

        DEFAULT_TEXTFILE_CHARSET.valueType = ValueType.STRING;
        DEFAULT_TEXTFILE_CHARSET.defaultValue = StandardCharsets.UTF_8.name();

        CONFIG_SCHEME_LIST.valueType = ValueType.SPECIAL_CLASS;
        CONFIG_SCHEME_LIST.classType = Map.class; // needs special check on assignment and use.
        CONFIG_SCHEME_LIST.defaultValue = null;
        FALLBACKS_ACROSS_SCOPES.valueType = ValueType.BOOLEAN;
        FALLBACKS_ACROSS_SCOPES.defaultValue = Boolean.TRUE;

        WRITE_FALLBACK_ACROSS_SCOPES.valueType = ValueType.BOOLEAN;
        WRITE_FALLBACK_ACROSS_SCOPES.defaultValue = Boolean.FALSE;

        SCHEME_STRICT_MODE.valueType = ValueType.BOOLEAN;
        SCHEME_STRICT_MODE.defaultValue = Boolean.FALSE;
        SCHEME_RESETS_DEFAULTS.valueType = ValueType.BOOLEAN;
        SCHEME_RESETS_DEFAULTS.defaultValue = Boolean.FALSE;

        DEFAULT_ON_MISSING_ENTRY.valueType = ValueType.BOOLEAN;
        DEFAULT_ON_MISSING_ENTRY.defaultValue = Boolean.FALSE;

        UPDATE_CHECK_FREQUENCY_MS.valueType = ValueType.NUMBER;
        UPDATE_CHECK_FREQUENCY_MS.defaultValue = 2000;

        UPDATE_CHECK_SCOPES.valueType = ValueType.STRINGLIST;
        UPDATE_CHECK_SCOPES.defaultValue = Arrays.stream(ConfigScope.values()).map(Enum::name).collect(Collectors.toList()); // convert all the enum names to a string list

        WRITE_CONDENSED_FORMAT.valueType = ValueType.BOOLEAN;
        WRITE_CONDENSED_FORMAT.defaultValue = Boolean.FALSE;

        //----------------------------------------------------------------------
        FILE_FORMAT_READING_PRIORITIES.valueType = ValueType.STRINGLIST; // for writing
        FILE_FORMAT_READING_PRIORITIES.defaultValue = new ArrayList<String>(Arrays.asList("DHALL", "TOML", "TOMLwithJackson", "YAML", "YAMLwithJackson", "JSON5", "JSON5withJackson", "JSON", "JSONwithJackson", "ASN1", "properties", "INI"));

        // we should not write DHALL; it is too complex, and we're likely to mess up the "source of truth". So we don't.
        // and we don't want to write INI unless explicitly asked for
        FILE_FORMAT_WRITING_PRIORITIES.valueType = ValueType.STRINGLIST; // for reading
        FILE_FORMAT_WRITING_PRIORITIES.defaultValue = new ArrayList<String>(Arrays.asList("TOML", "TOMLwithJackson", "YAML", "YAMLwithJackson", "JSON5", "JSON5withJackson", "JSON", "JSONwithJackson", "properties"));

        FILE_FORMAT_READING_ALLOW_ALL_FORMATS.valueType = ValueType.BOOLEAN;
        FILE_FORMAT_READING_ALLOW_ALL_FORMATS.defaultValue = Boolean.TRUE;
        FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS.valueType = ValueType.BOOLEAN;
        FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS.defaultValue = Boolean.TRUE;

        STORAGE_TYPE_PRIORITIES.valueType = ValueType.STRINGLIST;
        STORAGE_TYPE_PRIORITIES.defaultValue = new ArrayList<String>(Arrays.asList("RAM", "secrets", "files", "registry", "registryjni", "zookeeper", "JAR"));
        STORAGE_TYPE_ALLOW_ALL_STORAGES.valueType = ValueType.BOOLEAN;
        STORAGE_TYPE_ALLOW_ALL_STORAGES.defaultValue = Boolean.TRUE;

        FILENAME_EXTENSION_MAPPINGS.valueType = ValueType.STRINGLIST;
        AUTOMATIC_CONFIG_LOADING.valueType = ValueType.BOOLEAN;
        AUTOMATIC_CONFIG_CREATION.valueType = ValueType.BOOLEAN;

        CACHE_CONFIGS.valueType = ValueType.BOOLEAN;
        WRITE_SYNC.valueType = ValueType.BOOLEAN;
        FREE_CONFIGURATION.valueType = ValueType.BOOLEAN; // scheduled for removal
        NO_DEFAULT_DIRECTORIES.valueType = ValueType.BOOLEAN;

        ALLOW_MCONFIG_RUNTIME_SETTINGS.valueType = ValueType.BOOLEAN;
        ALLOW_MCONFIG_RUNTIME_SETTINGS.defaultValue = Boolean.FALSE;

        ENABLE_SELF_CONFIGURATION.valueType = ValueType.BOOLEAN;
        ENABLE_SELF_CONFIGURATION.defaultValue = Boolean.TRUE;

        LOGGING_REDIRECT_TARGET.valueType = ValueType.STRING;
        DISABLED_MODULE_IDS.valueType = ValueType.STRINGLIST;
        ADDITIONAL_MODULE_PATHS.valueType = ValueType.STRINGLIST;

        SECRETS_PROVIDER_ID.valueType = ValueType.STRING;
        SECRETS_PROVIDER_CONFIG.valueType = ValueType.SPECIAL_CLASS;
        SECRETS_PROVIDER_CONFIG.classType = Map.class;
        ADDITIONAL_SECRETS_PROVIDERS.valueType = ValueType.SPECIAL_CLASS;
        ADDITIONAL_SECRETS_PROVIDERS.classType = List.class;

        COMMENTS_READING.valueType = ValueType.BOOLEAN;
        COMMENTS_READING.defaultValue = Boolean.FALSE;

        COMMENTS_WRITING.valueType = ValueType.BOOLEAN;
        COMMENTS_WRITING.defaultValue = Boolean.FALSE;

        DESCRIPTION_ON_CREATE.valueType = ValueType.BOOLEAN;
        DESCRIPTION_ON_CREATE.defaultValue = Boolean.FALSE;
        }

    private ValueType valueType;
    private Class     classType; // outside valueType; specific to SPECIAL_CLASS
    private Object    defaultValue; // if set, must be of correct type.

    /**
     * check if the Feature is of Boolean type
     *
     * @return true if boolean, false otherwise
     */
    public boolean isBooleanType()
        {
        return (valueType == ValueType.BOOLEAN);
        }

    /**
     * check if the Feature is of String type
     *
     * @return true if String, false otherwise
     */
    public boolean isStringType()
        {
        return (valueType == ValueType.STRING);
        }

    /**
     * check if the Feature is of numeric (Integer) type
     *
     * @return true if Integer, false otherwise
     */
    public boolean isNumberType()
        {
        return (valueType == ValueType.NUMBER);
        }

    /**
     * check if the Feature is of a String List type
     *
     * @return true if a List of Strings, false otherwise.
     */
    public boolean isStringListType()
        {
        return (valueType == ValueType.STRINGLIST);
        }

    // internal. intentionally not exported
    public ValueType getType()
        {
        return valueType;
        }

    /**
     * <p>getDefault.</p>
     *
     * @return a {@link java.lang.Object} object
     */
    public Object getDefault() { return defaultValue; }

    /**
     * check if the Feature is of a special class type, e.g. Map or String List.
     *
     * @param <T>           the general set of these types
     * @param expectedClass the class specifically to be tested against
     * @return true if a List of Strings, false otherwise.
     */
    public <T extends Object> boolean isSpecialClassType(Class<T> expectedClass)
        {
        if (valueType != ValueType.SPECIAL_CLASS) return false;
        return (classType != null) && classType.isAssignableFrom(expectedClass);
        }

    /**
     * Convenience: does the SPECIAL_CLASS declaration accept the runtime type of the given object?
     */
    public boolean isSpecialClassInstance(Object value)
        {
        if (valueType != ValueType.SPECIAL_CLASS || value == null) return false;
        return (classType != null) && classType.isInstance(value);
        }


    public enum ValueType
    {BOOLEAN, STRING, STRINGLIST, NUMBER, SPECIAL_CLASS}

}
//___EOF___
