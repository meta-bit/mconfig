package outside.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WriteTest
{
    public static final String               COMPANY_NAME     = "metabit";
    public static final String               APPLICATION_NAME = "CONFIGTEST";
    private static      ConfigFactoryBuilder configFactoryBuilder;
    private static      Path                 tempDir;
    private static      Path                 tmpDirRuntimeScope;
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


        tmpDirRuntimeScope = Files.createTempDirectory(currentWorkingDirectory, "configtest_userscope");
        //@TODO



        // add temp subdirectory to search path in builder?
        configFactoryBuilder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        // prepend this temp directory to our config directory search list
        configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, Collections.singletonList(tempDir.toAbsolutePath().toString()));
        configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, Collections.singletonList(tmpDirRuntimeScope.toAbsolutePath().toString()));
        configFactoryBuilder.setFeature(ConfigFeature.WRITE_SYNC, false);
        }

    @AfterAll
    static void exit()
        {
        // clean up temp directory with temp config files
        tempCfgFile2.toFile().delete();
        tempCfgFile.toFile().delete();
        tempDir.toFile().delete();
        tmpDirRuntimeScope.toFile().delete();
        // ignoring results: there is no use in aborting the deletion.
        }

    @BeforeEach
    void resetFileContents()
            throws IOException
        {
        // write stuff into the files, replacing previous content.
        // this is called before each test, so the order of tests does not matter - they get the same contents
        Files.write(tempCfgFile, "testdata=testvalue\nsecond=2".getBytes(StandardCharsets.UTF_8));
        Files.write(tempCfgFile2, "sample test content".getBytes(StandardCharsets.UTF_8));
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
    void testConfigEntryWriting()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);

        ConfigFactory configFactory = configFactoryBuilder.build();

        Configuration cfg = configFactory.getConfig("testconfig");

        // get existing entry
        ConfigEntry entry = cfg.getConfigEntryFromFullKey("testdata", EnumSet.of(ConfigScope.USER));
        assertEquals(ConfigEntryType.STRING, entry.getType());
        assertEquals("testvalue", entry.getValueAsString());

        // change the specific entry we've found
        entry.putString("changedvalue");

        cfg.flush();
        // and read back in a different way to check it.
        String testValue = cfg.getString("testdata");
        assertEquals("changedvalue", testValue);
        return;
        }


    @Test
    void testConfigWritingStringEntries()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5); //

        configFactoryBuilder.setFeature(ConfigFeature.WRITE_SYNC, true); // sync writes, no write cache
        configFactoryBuilder.setFeature(ConfigFeature.FALLBACKS_ACROSS_SCOPES, true); // sync writes, no write cache

        ConfigFactory configFactory = configFactoryBuilder.build();

        Configuration cfg = configFactory.getConfig("testconfig");

        // read existing entry
        String value = cfg.getString("testdata");
        assertEquals("testvalue", value);

        // provoke entry not found exception
        ConfigException thrown = assertThrows(ConfigException.class, ()->
            {
            String dummy = cfg.getString("ThisKeyDoesNotExist");
            System.out.println(dummy);
            });
        assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, thrown.getReason());

        // overwrite the existing entry, by key (not by ConfigEntry handle)
        cfg.put("testdata", "changed", ConfigScope.USER); // our test directories have been applied to the runtime scope here
        //@TODO perform a similar test with a temporary directory added in the USER scope.

        // check the change to be effective
        String value2 = cfg.getString("testdata");
        assertEquals("changed", value2);

        // write a new entry for a specific Scope, without prior config
        cfg.put("testdata","override", ConfigScope.RUNTIME);

        // read the key to see the write was effective
        // RUNTIME takes priority over USER,
        String value3 = cfg.getString("testdata");
        assertEquals("override", value3);

        // check the specific scope for the key to make sure it landed there (fallback turned off)

        ConfigEntry entry = cfg.getConfigEntryFromFullKey("testdata", EnumSet.of(ConfigScope.RUNTIME));
        // @TODO checks

        return;
        }

}
//___EOF___
