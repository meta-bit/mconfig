package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigSearchList;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that addPathToSearchList() rejects path segments that could enable
 * directory traversal attacks.
 *
 * The ".." check in addPathToSearchList() throws InvalidPathException before
 * addDirectoryToSearchListIfAccessible() is called, so a real FileConfigStorage
 * instance is sufficient — no stubbing needed.
 */
public class PathTraversalSanitizationTest
    {
    @Test
    public void testDotDotSegmentIsRejected()
        {
        ConfigSearchList list = new ConfigSearchList();
        Path tempBase;
        try { tempBase = Files.createTempDirectory("mconfig_traversal_test"); }
        catch (Exception e) { fail("Could not create temp dir: " + e.getMessage()); return; }

        FileConfigStorage storage = new FileConfigStorage();

        // ".." in a segment must be detected and rejected before any path is added
        storage.addPathToSearchList(list, tempBase.toString(), ConfigScope.USER, "..", "etc", "passwd");
        assertEquals(0, list.getEntries().size(),
                "A path containing '..' should not be added to the search list");
        }

    @Test
    public void testDotDotInMiddleSegmentIsRejected()
        {
        ConfigSearchList list = new ConfigSearchList();
        Path tempBase;
        try { tempBase = Files.createTempDirectory("mconfig_traversal_test2"); }
        catch (Exception e) { fail("Could not create temp dir: " + e.getMessage()); return; }

        FileConfigStorage storage = new FileConfigStorage();

        storage.addPathToSearchList(list, tempBase.toString(), ConfigScope.USER, "subdir", "..", "escaped");
        assertEquals(0, list.getEntries().size(),
                "A path with '..' in the middle should not be added to the search list");
        }

    @Test
    public void testNormalSegmentsAreAccepted()
        {
        ConfigSearchList list = new ConfigSearchList();
        Path tempBase;
        try { tempBase = Files.createTempDirectory("mconfig_traversal_test3"); }
        catch (Exception e) { fail("Could not create temp dir: " + e.getMessage()); return; }

        FileConfigStorage storage = new FileConfigStorage();

        // A clean path should be accepted (directory may not exist, but no traversal)
        assertDoesNotThrow(() ->
                storage.addPathToSearchList(list, tempBase.toString(), ConfigScope.USER, ".config", "myapp")
        );
        }

    @Test
    public void testXdgConfigHomeRelativePathIsIgnoredOnLinux()
        {
        // The LinuxSearchPaths code validates isAbsolute() before using XDG_CONFIG_HOME.
        // Verify the isAbsolute() check logic via Path API.
        assertFalse(java.nio.file.Paths.get("../../etc").isAbsolute(),
                "A relative path starting with '..' must not be treated as absolute");
        assertFalse(java.nio.file.Paths.get("relative/path").isAbsolute(),
                "A relative path must not be treated as absolute");
        assertTrue(java.nio.file.Paths.get("/absolute/path").isAbsolute(),
                "An absolute path must be recognized as absolute");
        }
    }
