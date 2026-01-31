package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

/**
 * Initializes Linux search paths for config discovery
 *
 *     LINUX OS family
 *     Standards:
 *          - XDG Base Directory Specification: specifications.freedesktop.org
 *          - Filesystem Hierarchy Standard (FHS): refspecs.linuxfoundation.org
 *
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
            storage.addPathToSearchList(searchList, path, ConfigScope.HOST, applicationName);
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
            storage.addPathToSearchList(searchList, xdgUserConfig, ConfigScope.USER, companyName, applicationName, subDir);
            }
        else
            {
            storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, ".config", companyName, applicationName, subDir);
            }

        // User settings directory: ~/.<appname>
        storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, "."+applicationName, subDir);

        // not official Linux FHS but allows for a cleaner structure.
        storage.addPathToSearchList(searchList, "/etc/opt/", ConfigScope.HOST, companyName, applicationName, subDir);

        // this is the best place according to FHS for most cases.
        storage.addPathToSearchList(searchList, "/etc/opt", ConfigScope.HOST, applicationName, subDir);

        // Common for self-compiled or local admin software
        storage.addPathToSearchList(searchList, "/usr/local/etc", ConfigScope.HOST, applicationName, subDir);

        // according to FHS, there may be no binary configurations in /etc. Not adding an exclusion rule there, though.
        storage.addPathToSearchList(searchList, "/etc/", ConfigScope.HOST, applicationName, subDir);

        // relative to the current working directory the application was started from.
        storage.addPathToSearchList(searchList, System.getProperty("user.dir"), ConfigScope.SESSION, ".config", applicationName, subDir);

        return;
        }
    }
//___EOF___
