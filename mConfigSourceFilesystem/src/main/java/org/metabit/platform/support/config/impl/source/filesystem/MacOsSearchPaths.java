package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * preliminary MacOS support
 *
 * **EXPERIMENTAL**
 *
 * *NOT TESTED YET*
 *
 * needs official documentation sources and references.
 *
 */
class MacOsSearchPaths implements SearchPathInitializer
    {
    @Override
    public void initSearchPaths(ConfigSearchList searchList, String companyName, String applicationName, String subDir, FileConfigStorage storage)
        {
        // USER scope: XDG override (if present)
        String xdgUserConfig = System.getenv("XDG_CONFIG_HOME");
        if (xdgUserConfig != null && !xdgUserConfig.isEmpty())
            {
            storage.addPathToSearchList(searchList, xdgUserConfig, ConfigScope.USER, companyName, applicationName, subDir);
            }

        // macOS standard locations (per gist references)
        storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER,
                "Library", "Application Support", companyName, applicationName, subDir);
        storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER,
                "Library", "Preferences", companyName, applicationName, subDir);

        // Unix-y fallback for CLI tooling
        storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, "."+applicationName, subDir);

        // HOST scope: system-wide config/prefs
        storage.addPathToSearchList(searchList, "/Library/Application Support", ConfigScope.HOST,
                companyName, applicationName, subDir);
        storage.addPathToSearchList(searchList, "/Library/Preferences", ConfigScope.HOST,
                companyName, applicationName, subDir);

        // APPLICATION scope: portable installs next to the binary/jar
        try
            {
            Path appPath = Paths.get(FileConfigStorage.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path appDir = appPath.toFile().isDirectory() ? appPath : appPath.getParent();

            storage.addPathToSearchList(searchList, appDir.toString(), ConfigScope.APPLICATION, ".config");
            storage.addPathToSearchList(searchList, appDir.toString(), ConfigScope.APPLICATION);
            }
        catch (URISyntaxException|SecurityException|NullPointerException ignored)
            {
            storage.logInfo("cannot determine application directory, skipping");
            }

        // SESSION scope: working dir subdirectory .config
        storage.addPathToSearchList(searchList, System.getProperty("user.dir"), ConfigScope.SESSION, ".config", applicationName, subDir);

        return;
        }
    }
//___EOF___