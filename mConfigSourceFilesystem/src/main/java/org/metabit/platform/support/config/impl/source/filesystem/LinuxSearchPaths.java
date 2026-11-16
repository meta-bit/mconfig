package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

/**
 * Initializes Linux search paths for config discovery
 * <br/>
 *     LINUX OS family
 *     Standards:
 *          - XDG Base Directory Specification: specifications.freedesktop.org
 *          - Filesystem Hierarchy Standard (FHS): refspecs.linuxfoundation.org
 * <br/>
 * note: Linux uses HOST scope primarily. APPLICATION scope is atypical for linux.
 */
class LinuxSearchPaths implements SearchPathInitializer
    {
    @Override
    public void initSearchPaths(ConfigSearchList searchList, String companyName, String applicationName, String subDir, FileConfigStorage storage)
        {
        // HOST Scope on Linux
        // with check for XDG override
        String xdgDirs = System.getenv("XDG_CONFIG_DIRS");
        if (xdgDirs == null || xdgDirs.isEmpty())
            {
            xdgDirs = "/etc/xdg";
            }
        for (String path : xdgDirs.split(":"))
            {
            if (path == null || path.trim().isEmpty()) continue;
            storage.addAbsolutePathToSearchList(searchList, path.trim(), ConfigScope.HOST, applicationName);
            }

        /// In the field, one may also encounter `/etc/xdg/<config>.<extension>`.
        /// This should be deprecated, because it provokes collisions. ("/etc/xdg/database.yaml")
        /// = not supported by mConfig.

        //@TODO support for XDG feature of specific write paths in contrast to read paths to be considered.

        // User settings directory: ~/.config/<companyName>/<appname>
        // with check for XDG override
        String xdgUserConfig = System.getenv("XDG_CONFIG_HOME");
        if (xdgUserConfig != null && !xdgUserConfig.isEmpty())
            {
            // sanitize and validate path: ensure XDG_CONFIG_HOME is absolute and doesn't contain traversal
            try
                {
                java.nio.file.Path xdgPath = java.nio.file.Paths.get(xdgUserConfig).normalize();
                if (!xdgPath.isAbsolute())
                    {
                    storage.logInfo("XDG_CONFIG_HOME is not an absolute path, ignoring: " + xdgUserConfig);
                    storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, ".config", companyName, applicationName, subDir);
                    }
                else
                    {
                    storage.addPathToSearchList(searchList, xdgPath.toString(), ConfigScope.USER, companyName, applicationName, subDir);
                    }
                }
            catch (java.nio.file.InvalidPathException e)
                {
                storage.logInfo("XDG_CONFIG_HOME contains an invalid path, ignoring: " + xdgUserConfig);
                storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, ".config", companyName, applicationName, subDir);
                }
            }
        else
            {
            storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, ".config", companyName, applicationName, subDir);
            }

        // User settings directory: ~/.<appname>
        storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, "."+applicationName, subDir);

        // not official Linux FHS but allows for a cleaner structure.
        storage.addPathToSearchList(searchList, "/etc/opt", ConfigScope.HOST, companyName, applicationName, subDir);

        // this is the best place according to FHS for most cases.
        storage.addPathToSearchList(searchList, "/etc/opt", ConfigScope.HOST, applicationName, subDir);

        // Common for self-compiled or local admin software
        storage.addPathToSearchList(searchList, "/usr/local/etc", ConfigScope.HOST, applicationName, subDir);

        // relative to the current working directory the application was started from.
        storage.addAbsolutePathToSearchList(searchList, System.getProperty("user.dir"), ConfigScope.SESSION, ".config", applicationName, subDir);

        return;
        }
    }
//___EOF___
