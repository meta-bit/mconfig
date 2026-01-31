package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.impl.ConfigSearchList;

interface SearchPathInitializer
    {
    void initSearchPaths(ConfigSearchList searchList, String companyName, String applicationName, String subDir, FileConfigStorage storage);
    }
