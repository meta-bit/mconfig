package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;


@TestInstance(TestInstance.Lifecycle.PER_CLASS) // use the same temp dir, do not instantiate this test in parallel
class FileChangeWatcherTest
{
    @TempDir(cleanup = CleanupMode.ALWAYS)
    static  Path                         testInstanceTempDir;
    private ConfigFactoryInstanceContext ctx;
    private ConfigStorageInterface       mockStorage;

    @Test
    void testTempDir()
            throws IOException
        {
        assertTrue(testInstanceTempDir.toFile().isDirectory(), "Should be a directory ");

        System.out.println(testInstanceTempDir);

        File letters = new File(testInstanceTempDir.toFile(), "letters.txt");
        List<String> lines = Arrays.asList("x", "y", "z");

        Files.write(letters.toPath(), lines);

        assertAll(
                ()->assertTrue(Files.exists(letters.toPath()), "File should exist"),
                ()->assertLinesMatch(lines, Files.readAllLines(letters.toPath())));
        return; // success.
        }


    @BeforeAll
    void init()
        {
        ctx = Mockito.mock(ConfigFactoryInstanceContext.class);
        mockStorage = Mockito.mock(ConfigStorageInterface.class);
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigLoggingInterface logger = new ConsoleLogging("FileChangeWatcherTest");
        Mockito.when(ctx.getSettings()).thenReturn(settings);
        Mockito.when(ctx.getLogger()).thenReturn(logger);
        }

    @Test
    void testFileChangeWatcherMinimalEmpty()
            throws IOException
        {
        FileChangeWatcher fcw = new FileChangeWatcher(ctx);
        // first, test empty state.
        fcw.cleanup();
        assertFalse(fcw.hasChanged(testInstanceTempDir));
        }


    @Test
    void testWithOneFileChanging()
            throws IOException, InterruptedException
        {
        FileChangeWatcher fcw = new FileChangeWatcher(ctx);

        Path testFile1 = testInstanceTempDir.resolve("testFile1.txt");
        ConfigLocation loc1 = new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile1);

        ConfigLayerInterface dummy = null;
        fcw.addFile(testFile1, loc1);
        TimeUnit.MILLISECONDS.sleep(200);
        assertFalse(fcw.hasChanged(testFile1));

        fcw.removeFile(testFile1);
        fcw.cleanup();
        }


    @Test
    void testWithTwoFiles()
            throws IOException, InterruptedException
        {
        ctx.getSettings().setInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100);
        FileChangeWatcher fcw = new FileChangeWatcher(ctx);

        Path testFile1 = testInstanceTempDir.resolve("testFile1.txt");
        Path testFile2 = testInstanceTempDir.resolve("testFile2.txt");
        ConfigLocation loc1 = new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile1);
        ConfigLocation loc2 = new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile2);

        ConfigLayerInterface dummy = null;
        fcw.addFile(testFile1,loc1);
        TimeUnit.MILLISECONDS.sleep(200);
        // nothing has changed since start of the test
        assertFalse(fcw.hasChanged(testFile1));

        // now we perform a change
        Path dummy1File = Files.createFile(testFile1);
        TimeUnit.MILLISECONDS.sleep(500);
        // that change must be noticed
        assertTrue(fcw.hasChanged(testFile1));
        // but not cause any unexpected side effect
        assertFalse(fcw.hasChanged(testFile2));

        // and once the change has been processed, things are quiet again
        TimeUnit.MILLISECONDS.sleep(200);
        assertFalse(fcw.hasChanged(testFile1));
        Files.deleteIfExists(testFile1);

        TimeUnit.MILLISECONDS.sleep(500);
        // that change must be noticed
        assertTrue(fcw.hasChanged(testFile1));

        // delete the file again, that's another chance.

        // done, cleanup.
        // fcw.removeFile(testFile1);  intentionally skip explicit removal to give cleanup something to do.
        fcw.cleanup();
        }


    /**
     * register two directories, which do not exist yet;
     * then create them, to see whether this is detected and handled correctly.
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    void testTwoSubdirectoriesDelayedCreation()
            throws IOException, InterruptedException
        {
        // slightly different names for each test, so they don't interfere
        Path testSubDir1 = testInstanceTempDir.resolve("testSubDirA1");
        Path testSubDir2 = testInstanceTempDir.resolve("testSubDirA2");
        Path testFile1Dir1 = testSubDir1.resolve("testFile1.txt");
        Path testFile2Dir1 = testSubDir1.resolve("testFile2.txt");
        Path testFile1Dir2 = testSubDir2.resolve("testFile1.txt");
        assertFalse(testFile1Dir1.toFile().exists());
        assertFalse(testFile2Dir1.toFile().exists());
        assertFalse(testFile1Dir2.toFile().exists());

        ctx.getSettings().setInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100);
        FileChangeWatcher fcw = new FileChangeWatcher(ctx);

        fcw.addFile(testFile1Dir1, new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile1Dir1));
        fcw.addFile(testFile2Dir1, new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile2Dir1));
        fcw.addFile(testFile1Dir2, new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile1Dir2));

        // the FCW should be checking for two directories to come into existence now.
        assertEquals(0, fcw.howManyDirectoriesAreWatched());
        assertEquals(2, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        Files.createDirectory(testSubDir2);
        TimeUnit.MILLISECONDS.sleep(500);
        // FCW should have one existing directory watched now, another still checking.
        assertEquals(1, fcw.howManyDirectoriesAreWatched());
        assertEquals(1, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        Files.createDirectory(testSubDir1);
        TimeUnit.MILLISECONDS.sleep(500);
        // FCW should have two existing directory watched now
        assertEquals(2, fcw.howManyDirectoriesAreWatched());
        assertEquals(0, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        // files left for next test; let's see what happens when a directory itself gets deleted.
        Files.delete(testSubDir2);
        TimeUnit.MILLISECONDS.sleep(500);
        assertEquals(1, fcw.howManyDirectoriesAreWatched());
        assertEquals(1, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        Files.createDirectory(testSubDir2);
        TimeUnit.MILLISECONDS.sleep(500);
        // FCW should have one existing directory watched now, another still checking.
        assertEquals(2, fcw.howManyDirectoriesAreWatched());
        }


    /*
scenarios for directory detection

1. exists, to be watched for file changes
3. exists, is deleted
4. didn't exist, was created, then deleted again, then re-created.
 */

    /**
     * delayed creation, this time with files.
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    void testTwoSubdirectoriesWithFilesDelayedCreation()
            throws IOException, InterruptedException
        {
        // slightly different names for each test, so they don't interfere
        Path testSubDir1 = testInstanceTempDir.resolve("testSubDirB1");
        Path testSubDir2 = testInstanceTempDir.resolve("testSubDirB2");
        Path testFile1Dir1 = testSubDir1.resolve("testFile1.txt");
        Path testFile2Dir1 = testSubDir1.resolve("testFile2.txt");
        Path testFile1Dir2 = testSubDir2.resolve("testFile1.txt");
        assertFalse(testFile1Dir1.toFile().exists());
        assertFalse(testFile2Dir1.toFile().exists());
        assertFalse(testFile1Dir2.toFile().exists());

        ctx.getSettings().setInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100);
        FileChangeWatcher fcw = new FileChangeWatcher(ctx);

        fcw.addFile(testFile1Dir1, new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile1Dir1));
        fcw.addFile(testFile2Dir1, new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile2Dir1));
        fcw.addFile(testFile1Dir2, new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, testFile1Dir2));

        // the FCW should be checking for two directories to come into existence now.
        assertEquals(0, fcw.howManyDirectoriesAreWatched());
        assertEquals(2, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        Files.createDirectory(testSubDir2);
        TimeUnit.MILLISECONDS.sleep(200);
        // FCW should have one existing directory watched now, another still checking.
        assertEquals(1, fcw.howManyDirectoriesAreWatched());
        assertEquals(1, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        Files.createDirectory(testSubDir1);
        TimeUnit.MILLISECONDS.sleep(200);
        // FCW should have two existing directory watched now
        assertEquals(2, fcw.howManyDirectoriesAreWatched());
        assertEquals(0, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());



        // -- now, file creation.
        assertFalse(fcw.hasChanged(testFile1Dir1));
        assertFalse(fcw.hasChanged(testFile2Dir1));
        assertFalse(fcw.hasChanged(testFile1Dir2));

        Files.createFile(testFile1Dir2);
        assertTrue(testFile1Dir2.toFile().exists());
        TimeUnit.MILLISECONDS.sleep(200);

        assertFalse(fcw.hasChanged(testFile1Dir1));
        assertFalse(fcw.hasChanged(testFile2Dir1));
        assertTrue(fcw.hasChanged(testFile1Dir2)); /// -- detect the expected change
        // two more files
        Files.createFile(testFile1Dir1);
        Files.createFile(testFile2Dir1);
        TimeUnit.MILLISECONDS.sleep(200);
        // and check
        assertTrue(fcw.hasChanged(testFile1Dir1)); // changed
        assertTrue(fcw.hasChanged(testFile2Dir1)); // changed
        assertFalse(fcw.hasChanged(testFile1Dir2)); // not changed, that previous change is old news.

        // now change contents a bit
        Files.write(testFile1Dir1, "test=value".getBytes(StandardCharsets.UTF_8));
        TimeUnit.MILLISECONDS.sleep(200);
        assertTrue(fcw.hasChanged(testFile1Dir1)); // detect the content change

        // deletion of the file.
        Files.delete(testFile1Dir1);
        TimeUnit.MILLISECONDS.sleep(200);
        assertTrue(fcw.hasChanged(testFile1Dir1)); // detect the content change
        // delete the other file
        Files.delete(testFile2Dir1);
        TimeUnit.MILLISECONDS.sleep(200);
        assertTrue(fcw.hasChanged(testFile2Dir1)); // detect the content change

        // now we delete the directory.
        Files.delete(testSubDir1);
        // but the watch is still there, because we don't get a notice from the Java WatchService.
        assertEquals(2, fcw.howManyDirectoriesAreWatched()); // the non-existing one still here
        assertEquals(0, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        // so let's try what happens if we re-created the directory and the file. will it be detected - again?
        Files.createDirectory(testSubDir1);
        TimeUnit.MILLISECONDS.sleep(200);
        // check: no changes
        assertEquals(2, fcw.howManyDirectoriesAreWatched());
        assertEquals(0, fcw.howManyDirectoriesAreWaitedForToComeIntoExistence());

        // this is where things get tricky: the watch should tell us things, but it doesn't.
        // MLIB-
        /*
        Files.createFile(testFile1Dir1);
        Files.write(testFile1Dir1, "test=valueNumberTwo".getBytes(StandardCharsets.UTF_8));
        TimeUnit.MILLISECONDS.sleep(200);
        assertTrue(fcw.hasChanged(testFile1Dir1)); // changed
        */
        Files.delete(testFile1Dir2);
        Files.delete(testSubDir2);
        return;
        }

    @Test
    void testWatchServiceClosedAfterCleanup()
            throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException
        {
        ctx.getSettings().setInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 100);

        final FileChangeWatcher fcw = new FileChangeWatcher(ctx);

        final Path dummyFile = testInstanceTempDir.resolve("dummy.txt");

        final ConfigLocation dummyLoc = new ConfigLocationImpl(ConfigScope.APPLICATION, mockStorage, null, dummyFile);

        fcw.addFile(dummyFile, dummyLoc);

        TimeUnit.MILLISECONDS.sleep(500); // allow timer to tick and register the directory

        fcw.cleanup();

        final Field watchServiceField = FileChangeWatcher.class.getDeclaredField("watchService");

        watchServiceField.setAccessible(true);

        @SuppressWarnings("unchecked")
        final WatchService ws = (WatchService) watchServiceField.get(fcw);

        assertNotNull(ws);

        final Path regDir = Files.createTempDirectory(testInstanceTempDir, "regtest");

        assertThrows(ClosedWatchServiceException.class, () -> regDir.register(ws, StandardWatchEventKinds.ENTRY_CREATE));

        Files.delete(regDir);
        }


}