package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.util.ConfigUtil;
import org.metabit.platform.support.config.impl.util.ConfigIOUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * the SUB_PATH feature is not limited to filesystems, but that's its primary use case.
 */
class SubPathTest
{
    public static final  String COMPANY_NAME     = "metabit";
    public static final  String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME      = "testconfig";
    public static final  String SUBPATH_VALUE    = "subpath";

    private static Path currentWorkingDirectory;
    private static Path tempDirUser;
    private static Path tempDirUserSubdir;

    //@TODO this is missing a test for Filesystem subpath usage

    // then, also missing a test for registry subpath usage... if registry is present. So, the test goes in separate windows test?

    // then, more tests for different config sources


    @Test
    void whereToPutMyFiles()
            throws ConfigCheckedException
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
// normally, this should be activated in a test. However, we're checking for directory paths here, so special case.
//        builder.setFeature(ConfigFeature.TEST_MODE, true);
        // add the extra user dir we constructed ourselves; it gets priority over the defaults.
        builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(String.valueOf(tempDirUser.toAbsolutePath())));
        builder.setFeature(ConfigFeature.SUB_PATH, SUBPATH_VALUE);

        try (final ConfigFactory configFactory = builder.build())
            {
            // so where do the files go for this specific ConfigFactory instance?
            Path somePath = ConfigUtil.whereToPutMyFiles(configFactory, ConfigScope.USER, false);
            assertEquals(tempDirUserSubdir, somePath);
            }
        return;
        }


    @BeforeAll
    static void init()
            throws IOException
        {
        currentWorkingDirectory = Files.createTempDirectory("JUnitTests_");
        // create temp subdirectory
        tempDirUser = Files.createTempDirectory(currentWorkingDirectory, "findForWriting_USER");
        Path tempDirUserSubdirPath = tempDirUser.resolve(SUBPATH_VALUE);
        tempDirUserSubdir = Files.createDirectory(tempDirUserSubdirPath); // if it doesn't exist, it is not found later.
        // create temp subdirectory
        // tempDirApplication = Files.createTempDirectory(currentWorkingDirectory, "findForWriting_APPLICATION");
        return;
        }


    @AfterAll
    static void exit()
            throws IOException
        {
//        ConfigIOUtil.deleteDirectoryWithContents(tempDirApplication);
        ConfigIOUtil.deleteDirectoryWithContents(tempDirUser);
        assertTrue(currentWorkingDirectory.toFile().delete());
        }
}