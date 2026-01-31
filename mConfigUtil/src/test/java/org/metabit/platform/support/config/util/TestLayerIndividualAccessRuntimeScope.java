package org.metabit.platform.support.config.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * these tests are testing the layer hierarchy within manually added RUNTIME scope layers;
 * this way, they can use temporary files for the tests.
 */
class TestLayerIndividualAccessRuntimeScope
{

    public static final String               COMPANY_NAME     = "metabit";
    public static final String               APPLICATION_NAME = "CONFIGTEST";
    private static      ConfigFactoryBuilder configFactoryBuilder;
    static              Path                 tempDir1;
    static              Path                 tempDir2;
    static              Path                 tempDir3;
    private static      Path                 tempCfgFile1;
    private static      Path                 tempCfgFile2;
    private static      Path                 tempCfgFile3;
    @TempDir
    static              Path                 currentWorkingDirectory;


    @BeforeAll
    static void init()
            throws ConfigCheckedException, IOException
        {
        // currentWorkingDirectory = Files.createTempDirectory("JUnitTests_");
        // create temp subdirectory
        tempDir1 = Files.createTempDirectory(currentWorkingDirectory, "configLayerTest1");
        tempDir2 = Files.createTempDirectory(currentWorkingDirectory, "configLayerTest2");
        tempDir3 = Files.createTempDirectory(currentWorkingDirectory, "configLayerTest2");

        // add a test file to it
        tempCfgFile1 = Files.createFile(tempDir1.resolve("testconfig.properties"));
        tempCfgFile2 = Files.createFile(tempDir2.resolve("testconfig.properties"));
        tempCfgFile3 = Files.createFile(tempDir3.resolve("testconfig.properties"));

        assertAccess(tempCfgFile1);
        assertAccess(tempCfgFile2);
        assertAccess(tempCfgFile3);

        // write stuff into the files
        Files.write(tempCfgFile1, "testdata=level1\npartiallyobscured=level1\nminimallyobscured=level1\nsecond=2".getBytes(StandardCharsets.UTF_8));
        Files.write(tempCfgFile2, "testdata=level2\npartiallyobscured=level2\nthird=3".getBytes(StandardCharsets.UTF_8));
        Files.write(tempCfgFile3, "testdata=level3\n\nthird=3".getBytes(StandardCharsets.UTF_8));

        // add temp subdirectory to search path in builder?
        configFactoryBuilder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        // prepend this temp directory to our config directory search list
        configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, Arrays.asList(tempDir1.toAbsolutePath().toString(), tempDir2.toAbsolutePath().toString(), tempDir3.toAbsolutePath().toString()));

        return;
        }

    private static void assertAccess(Path pathToTest)
        {
        assertNotNull(pathToTest);
        assertTrue(pathToTest.toFile().exists());
        assertTrue(pathToTest.toFile().canRead());
        assertTrue(pathToTest.toFile().canWrite());
        }

    // workaround, see https://github.com/junit-team/junit5/issues/2811
    @AfterEach
    @EnabledOnOs(OS.WINDOWS)
    void cleanUp()
        {
        System.gc();
        }
    /* outside of this Windows oddity, the JUnit5 cleanup of the @TempDir
       is supposed to delete the temp dir including all its subdirectories.
     */
/*
    @AfterAll
    static void exit()
        {
        // clean up temp directory with temp config files
        tempCfgFile3.toFile().delete();
        tempCfgFile2.toFile().delete();
        tempCfgFile1.toFile().delete();
        tempDir3.toFile().delete();
        tempDir2.toFile().delete();
        tempDir1.toFile().delete();
        // assertTrue(currentWorkingDirectory.toFile().delete());
        // JUnit5 @tempDir is supposed to clean it all up for us.
        }
    */

    @Test
    @DisplayName("RUNTIME layer test")
    void testConfigAccessSingleStringEntriesHierarchy()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);
        ConfigFactory configFactory = configFactoryBuilder.build();

        Configuration cfg = configFactory.getConfig("testconfig"); // from test-properties file
        String value = cfg.getString("testdata");
        assertEquals("level3", value); // empty string, but no exception

        value = cfg.getString("partiallyobscured");
        assertEquals("level2", value); // empty string, but no exception

        value = cfg.getString("minimallyobscured");
        assertEquals("level1", value); // empty string, but no exception


        // provoke entry not found exception
        ConfigException thrown = assertThrows(ConfigException.class, ()->
            {
            String dummy = cfg.getString("ThisKeyDoesNotExist");
            System.out.println(dummy);
            });
        assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, thrown.getReason());
        }

}
//___EOF___
