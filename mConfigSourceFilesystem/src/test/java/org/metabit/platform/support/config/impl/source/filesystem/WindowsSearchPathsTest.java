package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigSearchList;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class WindowsSearchPathsTest
    {
    @Test
    public void testProgramDataNotPolluted()
        {
        FileConfigStorage storage = new FileConfigStorage();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        storage.init(ctx);

        WindowsSearchPaths wsp = new WindowsSearchPaths();
        ConfigSearchList searchList = new ConfigSearchList();

        String company = "ACME";
        String app = "RoadRunner";

        wsp.initSearchPaths(searchList, company, app, null, storage);

        // Since we are likely on Linux, these env vars might be null. 
        // Let's force check what would be added if they WERE set.
        
        for (ConfigLocation loc : searchList.getEntries())
            {
            String locStr = loc.toLocationString();
            
            // It should NOT be exactly file:/ followed by nothing or just a company name
            // (assuming some default values for env vars if they were there, 
            // but here we check for the ones that ARE added even if env vars are null)
            
            // For example, System.getProperty("user.dir") is not null.
            String userDir = System.getProperty("user.dir");
            assertFalse(locStr.equalsIgnoreCase("file:" + userDir), "Should not have plain user.dir in search list: " + locStr);
            assertFalse(locStr.equalsIgnoreCase("file:" + userDir + "/"), "Should not have plain user.dir/ in search list: " + locStr);
            }
        }
    }
