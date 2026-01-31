package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

/**
 * Initializes Android search paths for config discovery.
 *
 * Android is special because it has strict sandboxing and specific directory structures.
 * Since we are a library and don't have direct access to the Android 'Context' object,
 * we rely on system properties and environment variables.
 *
 * Scopes on Android:
 * - USER/APPLICATION: Internal app files directory (/data/data/<package>/files/)
 * - USER: External files directory (/storage/emulated/0/Android/data/<package>/files/)
 * - SESSION: Cache directory (/data/data/<package>/cache/)
 *
 * Note: Android-specific paths are constructed using environment variables and system properties,
 * which might not be available in all Android environments.
 * Fallback to standard Linux paths (XDG/FHS) is provided for broader compatibility.
 *
 * @TODO needs testing
 */
class AndroidSearchPaths implements SearchPathInitializer
    {
    private final LinuxSearchPaths linuxSearchPaths = new LinuxSearchPaths();

    @Override
    public void initSearchPaths(ConfigSearchList searchList, String companyName, String applicationName, String subDir, FileConfigStorage storage)
        {
        // 1. Internal Storage - /data/user/0/<package>/files/ (or /data/data/<package>/files/)
        // We attempt to find the package name or use the applicationName as a fallback.
        // Android often sets "user.home" to "/data" or similar, which isn't very helpful on its own.
        // However, many Android JVM environments (like Termux or custom ones) might set it differently.

        // Standard Android doesn't give many useful system properties for paths without Context.
        // If we're running in a context where these are set (like some bridge or specialized runner):
        String androidDataDir = System.getenv("ANDROID_DATA"); // usually /data
        String appInternalBase = (androidDataDir != null ? androidDataDir : "/data") + "/data/" + applicationName + "/files";

        // APPLICATION scope: internal app files
        storage.addPathToSearchList(searchList, appInternalBase, ConfigScope.APPLICATION, ".config", subDir);
        storage.addPathToSearchList(searchList, appInternalBase, ConfigScope.APPLICATION, subDir); //@TODO is duplicating USER scope (next line) useful?

        // USER scope: internal app files (as a fallback/alternative to application scope)
        storage.addPathToSearchList(searchList, appInternalBase, ConfigScope.USER, subDir);

        // 2. External Storage (Scoped) - /storage/emulated/0/Android/data/<package>/files/
        String externalStorage = System.getenv("EXTERNAL_STORAGE"); // usually /storage/emulated/0
        if (externalStorage != null)
            {
            String appExternalBase = externalStorage + "/Android/data/" + applicationName + "/files";
            storage.addPathToSearchList(searchList, appExternalBase, ConfigScope.USER, ".config", subDir);
            storage.addPathToSearchList(searchList, appExternalBase, ConfigScope.USER, subDir);
            }

        // 3. SESSION scope: cache directory
        String appCacheBase = (androidDataDir != null ? androidDataDir : "/data") + "/data/" + applicationName + "/cache";
        storage.addPathToSearchList(searchList, appCacheBase, ConfigScope.SESSION, ".config", subDir);

        // 4. Fallback to Linux-style paths in case we're in a more open environment (like Termux)
        linuxSearchPaths.initSearchPaths(searchList, companyName, applicationName, subDir, storage);
        }
    }
//___EOF___
