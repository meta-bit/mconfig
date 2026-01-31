package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConfigLayersWithJson
{
    public static final  String COMPANY_NAME     = "metabit";
    public static final  String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME      = "testconfig";
    private static       Path   currentWorkingDirectory;
    private static       Path   tempDir1;
    private static       Path   tempCfgFile1;

    @Test
    void checkFilePathSearchList()
        {
        // file system access is expected to be given; at least some entries must be available, and in the search list.
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                // prepend this temp directory to our config directory search list
                .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, List.of(tempDir1.toAbsolutePath().toString()))
                .build())
            {
            List<ConfigLocation> searchList = factory.getSearchList();
            assertNotNull(searchList);
            assertFalse(searchList.isEmpty());

            Configuration cfg = factory.getConfig(CONFIG_NAME);

            System.out.println("searchList: ");
            for (ConfigLocation location : searchList)
                { System.out.println("\t"+location.getScope()+"\t"+location.getURI("entry", null)); }

            System.out.println("config locations: ");
            for (ConfigLocation location : cfg.getSourceLocations())
                { System.out.println("\t"+location.getScope()+"\t"+location.getURI("entry", null)); }
            }
        }

    // test case 1: access a correctly formatted JSON file
    // test case 2: check overlays of two JSON files
    // test case 3: write invalid JSON first, see it getting ignored; then fix it, and check whether the file watch correctly detected the change and parsed it!

    @Test
    void readSimpleJsonConfig()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
/*
@TODO continue work here
            String testdataToplevel = cfg.getString("testdata");
            assertNotNull(testdataToplevel);

 */
            }
        return;
        }


    @BeforeAll
    static void init()
            throws IOException
        {
        currentWorkingDirectory = Files.createTempDirectory("JUnitTests_");
        // create temp subdirectory
        tempDir1 = Files.createTempDirectory(currentWorkingDirectory, "configLayerTest1");

        // add a test file to it
        tempCfgFile1 = Files.createFile(tempDir1.resolve(CONFIG_NAME+".json"));
        assertAccess(tempCfgFile1);
        // write some content into the file
        Files.write(tempCfgFile1, "{\"testdata\":\"level1\",\n\"array_empty\":[]}".getBytes(StandardCharsets.UTF_8));
        return;
        }

    private static void assertAccess(Path pathToTest)
        {
        assertNotNull(pathToTest);
        assertTrue(pathToTest.toFile().exists());
        assertTrue(pathToTest.toFile().canRead());
        assertTrue(pathToTest.toFile().canWrite());
        }

    @AfterAll
    static void exit()
        {
        // clean up temp directory with temp config files
        assertTrue(tempCfgFile1.toFile().delete());
        assertTrue(tempDir1.toFile().delete());
        assertTrue(currentWorkingDirectory.toFile().delete());
        }


}
