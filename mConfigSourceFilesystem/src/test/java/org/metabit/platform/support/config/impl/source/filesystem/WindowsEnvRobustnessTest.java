package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.impl.ConfigSearchList;
import org.metabit.platform.support.osdetection.OperatingSystem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WindowsEnvRobustnessTest {

    @Test
    public void testInitFileSearchLocationsWindowsNoEnv() {
        FileConfigStorage storage = new FileConfigStorage();
        // We don't need a full init, just enough to not NPE on logger if it's used.
        // Actually FileConfigStorage.init initializes the logger.
        // But initFileSearchLocations is what we want to test.
        
        ConfigSearchList searchList = new ConfigSearchList();
        
        // This is expected to throw NPE or InvalidPathException if not handled, 
        // because "AppData" etc are missing on Linux.
        assertDoesNotThrow(() -> {
            storage.initFileSearchLocations(searchList, OperatingSystem.WINDOWS, "MyCompany", "MyApp", null);
        });
    }
}
