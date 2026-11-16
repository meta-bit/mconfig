package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * search locations whose path contains control characters are rejected (A8, CWE-117).
 * All search locations of the storage pass through addDirectoryToSearchListIfAccessible().
 */
class UnsafeSearchPathTest
{
    private static Path pathOrSkip(String first, String... more)
        {
        try
            {
            return Paths.get(first, more);
            }
        catch (InvalidPathException e)
            {
            assumeTrue(false, "this filesystem does not allow control characters in paths");
            return null;
            }
        }

    @Test
    void pathWithLineBreakIsRejected() throws Exception
        {
        Path base = Files.createTempDirectory("mconfig_unsafe_path");
        Path forged = pathOrSkip(base.toString(), "conf\nINFO forged log line");
        ConfigSearchList list = new ConfigSearchList();
        assertFalse(new FileConfigStorage().addDirectoryToSearchListIfAccessible(list, forged, ConfigScope.USER, true));
        assertEquals(0, list.getEntries().size());
        }

    @Test
    void pathWithTerminalEscapeIsRejectedViaAddPathToSearchList() throws Exception
        {
        Path base = Files.createTempDirectory("mconfig_unsafe_path2");
        pathOrSkip(base.toString(), "x\u001b[2J");
        ConfigSearchList list = new ConfigSearchList();
        new FileConfigStorage().addPathToSearchList(list, base.toString(), ConfigScope.USER, "x\u001b[2J");
        assertEquals(0, list.getEntries().size());
        }

    @Test
    void ordinaryPathIsAccepted() throws Exception
        {
        Path base = Files.createTempDirectory("mconfig_safe path");
        ConfigSearchList list = new ConfigSearchList();
        assertTrue(new FileConfigStorage().addDirectoryToSearchListIfAccessible(list, base, ConfigScope.USER, true));
        assertEquals(1, list.getEntries().size());
        }
}
//___EOF___
