package org.metabit.platform.support.config.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class TestMultipleConfigsInDifferentScopes
{

public static final String               COMPANY_NAME     = "metabit";
public static final String               APPLICATION_NAME = "CONFIGTEST";
private static      ConfigFactoryBuilder configFactoryBuilder;
private static      Path                 tempDir1;
private static Path tempDir2;
private static Path tempCfgFile1;
private static Path tempCfgFile2;

@BeforeAll
static void init() throws ConfigCheckedException, IOException
    {
    Path currentWorkingDirectory = Paths.get(".");
    // create temp subdirectory
    tempDir1 = Files.createTempDirectory(currentWorkingDirectory, "configtest1");
    tempDir2 = Files.createTempDirectory(currentWorkingDirectory, "configtest2");

    // add a test file to it
    tempCfgFile1 = Files.createFile(tempDir1.resolve("testconfig.properties"));
    tempCfgFile2 = Files.createFile(tempDir2.resolve("testconfig.properties"));


    assertNotNull(tempCfgFile1);
    assertTrue(tempCfgFile1.toFile().exists());
    assertTrue(tempCfgFile1.toFile().canRead());
    assertTrue(tempCfgFile1.toFile().canWrite());

    // write stuff into the files
    Files.write(tempCfgFile1,"testdata=userspecific\nsecond=2".getBytes(StandardCharsets.UTF_8));
    Files.write(tempCfgFile2,"testdata=runtimespecific\nthird=3".getBytes(StandardCharsets.UTF_8));


    // add temp subdirectory to search path in builder?
    configFactoryBuilder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
    // prepend this temp directory to our config directory search list
    configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, Collections.singletonList(tempDir1.toAbsolutePath().toString())); // Configuration.Scope.USER
    configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, Collections.singletonList(tempDir2.toAbsolutePath().toString())); // Configuration.Scope.RUNTIME
    }

@AfterAll
static void exit()
    {
    // clean up temp directory with temp config files
    tempCfgFile2.toFile().delete();
    tempCfgFile1.toFile().delete();
    tempDir2.toFile().delete();
    tempDir1.toFile().delete();
    }

@Test
void testConfigAccessSingleStringEntries()
        throws ConfigException, ConfigCheckedException
    {
    configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);

    ConfigFactory configFactory = configFactoryBuilder.build();

    Configuration cfg = configFactory.getConfig("testconfig");

    String value = cfg.getString("testdata");
    assertEquals("runtimespecific", value); // we're getting the most specific entrie,

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
