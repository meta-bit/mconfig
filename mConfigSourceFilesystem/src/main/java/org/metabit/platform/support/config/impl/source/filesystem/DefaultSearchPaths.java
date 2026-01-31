package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

/**
 * if no supported OS could be detected, this is the default fallback.
 */
class DefaultSearchPaths implements SearchPathInitializer
    {
    @Override
    public void initSearchPaths(ConfigSearchList searchList, String companyName, String applicationName, String subDir, FileConfigStorage storage)
        {
        // application-config directory straight in the user homes
        storage.addPathToSearchList(searchList, System.getProperty("user.home"), ConfigScope.USER, "."+applicationName, subDir);
        // relative to the working directory the application was started from; hence: application scope.
        storage.addPathToSearchList(searchList, System.getProperty("user.dir"), ConfigScope.HOST, ".config", applicationName, subDir);
        }
    }
//___EOF___