package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/* Windows OS filesystem locations for configuration storage.

    Over time, Windows has accumulated several standards.

    https://docs.microsoft.com/en-us/windows/deployment/usmt/usmt-recognized-environment-variables
    https://technet.microsoft.com/en-us/library/dd560744(v=ws.10).aspx
    ( also https://msdn.microsoft.com/en-us/library/bb762494(VS.85).aspx
    and https://msdn.microsoft.com/en-us/library/bb762584(VS.85).aspx )

    To consider: if registry is present, does it take precedence over config files or the other way around;
    and when it does, how is that reflected in the mConfig modules? (by config source hierarchy)
 */
class WindowsSearchPaths implements SearchPathInitializer
    {
    protected String getEnv(String name)
        {
        return System.getenv(name);
        }

    @Override
    public void initSearchPaths(ConfigSearchList searchList, String companyName, String applicationName, String subDir, FileConfigStorage storage)
        {
        // ---------------------------------------------------------------------
        // SESSION scope
        //
        // @TODO we may want to have a flag to turn off this scope for windows?
        // SESSION scope. used for the current session instance of the software. this is also where most environment variables go.
        // relative to the current working directory the application was started from.
        storage.addAbsolutePathToSearchList(searchList, System.getProperty("user.dir"), ConfigScope.SESSION, ".config", applicationName, subDir);
        // legacy .ini etc. in the same directory as the application is installed in
        storage.addAbsolutePathToSearchList(searchList, System.getProperty("user.dir"), ConfigScope.SESSION, applicationName, subDir);


        // ---------------------------------------------------------------------
        // USER Scope - specific to current user.
        /*
        priorities:
        1. XDG_CONFIG_HOME (Explicit user preference override; if present, it takes precedence over %AppData%).
        2. %AppData% (Standard roaming user settings).
        3. %LocalAppData% (User settings that shouldn't roam, like high-resolution UI caches).
        4. %UserProfile%/.config (Legacy/Unix-imitation fallback).
        */
        String xdgConfig = getEnv("XDG_CONFIG_HOME");
        if (xdgConfig != null && !xdgConfig.isEmpty()) // XDG_CONFIG_HOME overrides %AppData%.
            {
            // sanitize and validate path: ensure XDG_CONFIG_HOME is absolute and doesn't contain traversal
            try
                {
                java.nio.file.Path xdgPath = java.nio.file.Paths.get(xdgConfig).normalize();
                if (xdgPath.isAbsolute())
                    {
                    storage.addPathToSearchList(searchList, xdgPath.toString(), ConfigScope.USER, applicationName, subDir);
                    }
                else
                    {
                    storage.logInfo("XDG_CONFIG_HOME is not an absolute path, ignoring: " + xdgConfig);
                    }
                }
            catch (java.nio.file.InvalidPathException e)
                {
                storage.logInfo("XDG_CONFIG_HOME contains an invalid path, ignoring: " + xdgConfig);
                }
            }
        // application roaming path
        storage.addAbsolutePathToSearchList(searchList, getEnv("AppData"), ConfigScope.USER, companyName, applicationName, subDir);
        // "%LocalAppData%" -- this is not to be moved with the user (no roaming)
        storage.addAbsolutePathToSearchList(searchList, getEnv("LocalAppData"), ConfigScope.USER, companyName, applicationName, subDir);
        // application userdata path
        // "%UserProfile%" - really user-specific data, skipping the company name which users don't usually like
        // stores user config data not on toplevel in your User profile, but in a subdirectory called ".config"
        storage.addAbsolutePathToSearchList(searchList, getEnv("UserProfile"), ConfigScope.USER, ".config", applicationName, subDir);

        // ---------------------------------------------------------------------
        // APPLICATION scope. installation specific

        // "portable" installations where the config lives with the binary.
        // determine from where the program using the library is actually located
        // relative to that:
        // 1. subdirectory "/.config" (Modern clean portable).
        // 2. the path itself (Legacy .ini side-by-side).
        // Gets the path to the JAR/class file location
        try
            {
            Path appPath = Paths.get(FileConfigStorage.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            // If it's a JAR, the path includes the filename, so get the parent
            Path appDir = appPath.toFile().isDirectory() ? appPath : appPath.getParent();

            storage.addPathToSearchList(searchList, appDir.toString(), ConfigScope.APPLICATION, ".config", applicationName, subDir);
            storage.addPathToSearchList(searchList, appDir.toString(), ConfigScope.APPLICATION, applicationName, subDir);
            }
        catch (URISyntaxException|SecurityException|NullPointerException ignored)
            {
            storage.logInfo("cannot determine application directory, skipping");
            }

        // ---------------------------------------------------------------------
        // HOST scope. hardware/sandbox specific

        // 1st Priority: %ProgramData% (The gold standard for file-based machine config).
        storage.addAbsolutePathToSearchList(searchList, getEnv("ProgramData"), ConfigScope.HOST, companyName, applicationName, subDir);

        // 2nd Priority: Windows Registry (HKLM\Software\...)
        // -- this is not a directory; separate mConfig modules for Windows Registry handle the access. No init here.

        // 3. %AllUsersProfile% - legacy at HOST scope, resolves to C:\ProgramData by default.
        storage.addAbsolutePathToSearchList(searchList, getEnv("AllUsersProfile"), ConfigScope.HOST, companyName, applicationName, subDir);

        // ---------------------------------------------------------------------
        // CLUSTER scope:
        // 1. usually populated via environment variables, e.g. docker/kubernetes doing. See env var module
        // 2. an UNC path (e.g., \\config-server\cluster-settings\) may be used with network-based DirectoryConfigurationStorageAdapter.
        // which goes in a different mConfig module. No files.

        // ---------------------------------------------------------------------
        // ORGANIZATION scope (Company Level)
        // 1. Recommended Variant: Windows Registry (GPO Paths).
        // Logic: This is where Group Policy Objects (GPO) land. Policies pushed by an IT department reside in Software\Policies\CompanyName.
        // in cloud-native "organizations," remote URL or a mounted secret volume in a container: see network modules @TODO
        // compare with POLICY level; both can exist in GPO.

        // ---------------------------------------------------------------------
        // PRODUCT scope (hardcoded defaults):
        // not part of file-based configuration. For java, see JAR module.

        // ---------------------------------------------------------------------
        // POLICY scope (security overrides)
        // not part of file-based configuration.
        // Recommended Variant: Windows Registry (GPO Paths).
        // Logic: This is where Group Policy Objects (GPO) land. Policies pushed by an IT department reside in Software\Policies\CompanyName.
        // in cloud-native "organizations," remote URL or a mounted secret volume in a container: see network modules @TODO
        return;
        }
    }
