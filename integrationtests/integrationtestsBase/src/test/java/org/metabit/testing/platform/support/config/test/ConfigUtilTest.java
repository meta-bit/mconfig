package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.util.ConfigUtil;
import org.metabit.platform.support.config.impl.util.ConfigIOUtil;
import org.metabit.platform.support.config.util.TestDetector;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUtilTest
{
    public static final  String COMPANY_NAME     = "metabit";
    public static final  String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME      = "testconfig";

    private static Path currentWorkingDirectory;
    private static Path tempDirUser;
    private static Path tempDirApplication;

    @Test
    void copyPrefixedEntriesToJavaProperties()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            Assertions.assertNotNull(cfg);
            Properties extracted = ConfigUtil.copyPrefixedEntriesToJavaProperties(cfg, "prefixed.");
            Assertions.assertNotNull(extracted);
            assert (!extracted.isEmpty());
            }
        }


    @Test
    void whereToPutMyFiles()
            throws ConfigCheckedException
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
//@TODO check: what is wrong with this?
//        builder.setFeature(ConfigFeature.TEST_MODE, true);
//        builder.setTestConfigPaths(ConfigScope.USER, List.of(String.valueOf(tempDirUser.toAbsolutePath())));
//        builder.setTestConfigPaths(ConfigScope.APPLICATION, List.of(String.valueOf(tempDirApplication.toAbsolutePath())));
// instead
        builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(String.valueOf(tempDirUser.toAbsolutePath())));


        // add test path and test mode builder.
        // add test
        try (final ConfigFactory configFactory = builder.build())
            {
            for (ConfigLocation cse : configFactory.getSearchList())
                { System.out.println(cse.toString()); }

            Path somePath = ConfigUtil.whereToPutMyFiles(configFactory, ConfigScope.USER, false);
            System.out.println("this is where to put USER files:"+somePath);

            assertEquals(0, somePath.compareTo(tempDirUser));
            /*
            Path appFilesPath = ConfigUtil.whereToPutMyFiles(configFactory, ConfigScope.APPLICATION, false);
            System.out.println("this is where to put APPLICATION files:" + somePath.toString());
            */
            }
        return;
        }


    @Test
    void hasTestFrameworkBeenDetected()
            throws Exception
        {
        // test the detector directly
        assertTrue(TestDetector.isRunByTestingLibrary());

        // make sure that, when using quickConfig, the file paths all refer to src/test/resources
        try (Configuration config = ConfigUtil.quickConfig(COMPANY_NAME, APPLICATION_NAME, CONFIG_NAME))
            {
            config.getSourceLocations().forEach(location->
                {
                URI uri = location.getURI("entry", null);
                if (uri.getScheme().equals("file"))
                    assertTrue(uri.getPath().contains("src/test/resources"));
                });
            }
        return; // success
        }



    @BeforeAll
    static void init()
            throws IOException
        {
        currentWorkingDirectory = Files.createTempDirectory("JUnitTests_");
        // create temp subdirectory
        tempDirUser = Files.createTempDirectory(currentWorkingDirectory, "findForWriting_USER");
        // create temp subdirectory
        tempDirApplication = Files.createTempDirectory(currentWorkingDirectory, "findForWriting_APPLICATION");
        return;
        }


    @AfterAll
    static void exit()
            throws IOException
        {
        ConfigIOUtil.deleteDirectoryWithContents(tempDirApplication);
        ConfigIOUtil.deleteDirectoryWithContents(tempDirUser);
        assertTrue(currentWorkingDirectory.toFile().delete());
        }

    @Test public void minimumExample()
            throws Exception
        {
        // in this automated test, it is read from within resources directory,
        // .config/ACME/ourApp/itsConfig.properties
        try (Configuration cfg = ConfigUtil.quickConfig("ACME", "ourApp", "itsConfig"))
            {
            String message = cfg.getString("hello");
            System.out.println(message);
            }
        }
}