package outside.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestSimulatedAccessFromOutsideOfModule
{

    public static final String               COMPANY_NAME        = "metabit";
    public static final String               APPLICATION_NAME    = "CONFIGTEST";
    public static final String               SAMPLE_TEST_CONTENT = "sample test content";
    private static      ConfigFactoryBuilder configFactoryBuilder;
    private static      Path                 tempDir;
    private static      Path                 tempCfgFile;
    private static      Path                 tempCfgFile2;

    @BeforeAll
    static void init()
            throws ConfigCheckedException, IOException
        {
        // in standard maven environment, target will be the temporary build dir which `clean` will remove automatically.
        Path currentWorkingDirectory = Paths.get(".", "target");
        // create temp subdirectory
        tempDir = Files.createTempDirectory(currentWorkingDirectory, "configtest");
        // add a test file to it
        tempCfgFile = Files.createFile(tempDir.resolve("testconfig.properties"));
        tempCfgFile2 = Files.createFile(tempDir.resolve("testconfig2.txt"));


        assertNotNull(tempCfgFile);
        assertTrue(tempCfgFile.toFile().exists());
        assertTrue(tempCfgFile.toFile().canRead());
        assertTrue(tempCfgFile.toFile().canWrite());

        Files.write(tempCfgFile, "testdata=testvalue\nsecond=2".getBytes(StandardCharsets.UTF_8));
        // write stuff into the file.

        Files.write(tempCfgFile2, SAMPLE_TEST_CONTENT.getBytes(StandardCharsets.UTF_8));

//@TODO

        // add temp subdirectory to search path in builder?
        configFactoryBuilder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        // prepend this temp directory to our config directory search list
        configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, Collections.singletonList(tempDir.toAbsolutePath().toString()));
        }

    @AfterAll
    static void exit()
        {
        // clean up temp directory with temp config files
        tempCfgFile2.toFile().delete();
        tempCfgFile.toFile().delete();
        tempDir.toFile().delete();
        }

    @Test
    void testBuild()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);
        ConfigFactory configFactory = configFactoryBuilder.build();
        List<ConfigLocation> searchList = configFactory.getSearchList();
        searchList.forEach(entry->
            { System.out.println(entry); });
        }

    @Test
    void testConfigAccessSingleStringEntries()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);

        ConfigFactory configFactory = configFactoryBuilder.build();

        Configuration cfg = configFactory.getConfig("testconfig");

        String value = cfg.getString("testdata");
        assertEquals("testvalue", value);

        // provoke entry not found exception
        ConfigException thrown = assertThrows(ConfigException.class, ()->
            {
            String dummy = cfg.getString("ThisKeyDoesNotExist");
            System.out.println(dummy);
            });
        assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, thrown.getReason());
        }

    @Test
    void textFileRead()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);

        ConfigFactory configFactory = configFactoryBuilder.build();

        Configuration cfg = configFactory.getConfig("testconfig2");

        String value = cfg.getString("");

        System.out.println(value);
        assertEquals(value, value);

        // provoke entry not found exception
        ConfigException thrown = assertThrows(ConfigException.class, ()->
            {
            String dummy = cfg.getString("ThisKeyDoesNotExist");
            System.out.println(dummy);
            });
        assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, thrown.getReason());
        return;
        }

// System.err.println(cfg.getStorageLocation());
// cfg.getEntry(); or getValue, or get(), or...
// we want to differentiate between a full entry, and the value associated.
// and also, the most common case, we want a mapping
//  to  (a) an object, like (b) a Java Properties thingy.
// but those mappings belong in a different service-interface + modules.
// some are default-java-class, mappers, others should use e.g. reflection.
// maybe use reflection with put and get?
}
//___EOF___
