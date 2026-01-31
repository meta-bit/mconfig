package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestBlobAccess
{
    public static final  String COMPANY_NAME               = "metabit";
    public static final  String APPLICATION_NAME           = "mConfigIT";
    private static final String CONFIG_NAME                = "testBlobAccessConfig";
    public static final  String TEST_TEXT_CONTENT_TEMPLATE  = "testdata=level1\npartiallyobscured=level1\nminimallyobscured=level1\nsecond=2";
    private static final String TEST_TEXT2_CONTENT_TEMPLATE = "this is test content for a generated BLOB";
    private static       Path   currentWorkingDirectory;
    private static       Path   tempDir1; // for the reads
    private static       Path   tempDir2; // for the write
    private static       Path   tempCfgFile1;
    private static       Path   tempCfgFile2;
    private static Path tempCfgFile3;
    private static       byte[] test_binary_content;

    @Test
    void readBLOBs()
            throws ConfigCheckedException
        {
        // file system access is expected to be given; at least some entries must be available, and in the search list.
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                // prepend this temp directory to our config directory search list
                .setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("HOST:"+tempDir1.toAbsolutePath().toString()))
                .setFeature(ConfigFeature.TEST_MODE, true)
                .build())
            {

            List<ConfigLocation> searchList = factory.getSearchList();
            assertNotNull(searchList);
            assertFalse(searchList.isEmpty());

            // cumbersome? well, it's not a standard functionality.
            Configuration cfg = factory.getConfigSpecial(CONFIG_NAME+".bin", EnumSet.allOf(ConfigScope.class), Map.of("type", "blob"));
            assertNotNull(cfg);
            byte[] binContents = cfg.getBytes("");
            assertNotNull(binContents);
            assertArrayEquals(test_binary_content, binContents);
            for (ConfigLocation location : cfg.getSourceLocations())
                {
                System.out.println(location.toLocationString());
                }


            // cumbersome? well, it's not a standard functionality.
            Configuration cfg2 = factory.getConfigSpecial(CONFIG_NAME+".txt", EnumSet.allOf(ConfigScope.class), Map.of("type", "blob"));
            assertNotNull(cfg2);
            String contents = cfg2.getString("");
            assertNotNull(contents);
            assertFalse(contents.isEmpty());
            assertEquals(TEST_TEXT_CONTENT_TEMPLATE, contents);
            for (ConfigLocation location : cfg2.getSourceLocations())
                {
                System.out.println(location.toLocationString());
                }
            }
        return;
        }

    @Test void writeToExitingBLOB()
            throws ConfigCheckedException, IOException
        {
        // file system access is expected to be given; at least some entries must be available, and in the search list.
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                // prepend this temp directory to our config directory search list
                .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, List.of(tempDir2.toAbsolutePath().toString()))
                .build())
            {
            Configuration wcfg = factory.getConfigSpecial(CONFIG_NAME+"-overwrite", EnumSet.allOf(ConfigScope.class), Map.of("type", "blob"));
            assertNotNull(wcfg);
            assertTrue(wcfg.isWriteable());

            wcfg.put("","overwritten",ConfigScope.RUNTIME);
            // read back and compare contents?
            byte[] filecontents = Files.readAllBytes(tempCfgFile3);
            assertArrayEquals("overwritten".getBytes(StandardCharsets.UTF_8), filecontents);
            }
        return;
        }



    @Test
    void writeAndReadBackBLOBs()
            throws ConfigCheckedException, IOException
        {
        if (this.tempDir2.toFile().canWrite() == false)
            {
            System.err.println("test environment temp directories were not writeable for BLOB tests");
            return;
            }

        byte[] CONTENTS_TO_BE_WRITTEN = TEST_TEXT2_CONTENT_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        // file system access is expected to be given; at least some entries must be available, and in the search list.
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                // prepend this temp directory to our config directory search list
                .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, List.of(tempDir2.toAbsolutePath().toString()))
                .build())
            {
            Configuration wcfg = factory.getConfigSpecial(CONFIG_NAME+"-written", EnumSet.allOf(ConfigScope.class), Map.of("type", "blob"));
            assertNotNull(wcfg);
            assertTrue(wcfg.isWriteable());

            wcfg.put("",CONTENTS_TO_BE_WRITTEN,ConfigScope.RUNTIME);

            // now get the file and check its contents
            Path newBlobFile = tempDir2.resolve(CONFIG_NAME+"-written");
            assertTrue(Files.exists(newBlobFile));

            byte[] contents = Files.readAllBytes(newBlobFile);
            assertArrayEquals(CONTENTS_TO_BE_WRITTEN, contents);
            // and delete the File here, lest it blocks the other cleanup
            assertTrue(newBlobFile.toFile().delete());
            }
        return;
        }


    @Test void getBinaryConfigPath()
            throws ConfigCheckedException
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                // prepend this temp directory to our config directory search list
                .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, List.of(tempDir1.toAbsolutePath().toString()))
                .build())
            {
            String theActualConfigName = CONFIG_NAME+".bin";


            Configuration wcfg = factory.getConfigSpecial(theActualConfigName, EnumSet.allOf(ConfigScope.class), Map.of("type", "blob"));

            List<Path> fileLocations = getConfigFilePaths(wcfg, null);
            for (Path fileLocation : fileLocations)
                System.out.println(fileLocation);

            List<URI> uris = getConfigURIs(wcfg, null);
            for (URI uri : uris)
                System.out.println(uri);
            }
        return;
        }

    /// @TODO to go to mConfigSimple or the like, for general use.
    /**
     * get file paths a configuration is currently being read from.
     * @param cfg configuration to look into
     * @param configScopes the scopes for which to check. null for all scopes.
     * @return a List of Path entries referencing files.
     */
    public static List<Path> getConfigFilePaths(final Configuration cfg, EnumSet<ConfigScope> configScopes)
        {
        if (configScopes == null)
            configScopes = EnumSet.allOf(ConfigScope.class);
        //@TODO any way we can do without the configName parameter? and what about the scopes
        List<Path> paths = new ArrayList<>();
        if (cfg.isEmpty())
            throw new ConfigException("empty configuration");
        for (ConfigLocation loc : cfg.getSourceLocations())
            {
            if (! configScopes.contains(loc.getScope()))
                continue; // skip if scope is not wanted
            // convert location to URI
            URI uri = loc.getURI("",null);
            // get URI
            if(! uri.getScheme().equalsIgnoreCase("file"))
                {
                // logger.info("skipping non-file path location");
                continue;
                }
            // this may fail if the URI is not a file, so we check before.
            paths.add(Paths.get(uri));
            }
        return paths;
        }

    //@TODO this is for REAL general use. to be tested more
    public static List<URI> getConfigURIs(final Configuration cfg, EnumSet<ConfigScope> configScopes)
        {
        if (configScopes == null)
            configScopes = EnumSet.allOf(ConfigScope.class);
        //@TODO any way we can do without the configName parameter? and what about the scopes
        List<URI> uris = new ArrayList<>();
        if (cfg.isEmpty())
            throw new ConfigException("empty configuration");
        for (ConfigLocation loc : cfg.getSourceLocations())
            {
            if (! configScopes.contains(loc.getScope()))
                continue; // skip if scope is not wanted
            // convert location to URI
            URI uri = loc.getURI("",null); // for blobs perfectly; does it work for general configs as well?`
            uris.add(uri);
            }
        return uris;
        }


    @BeforeAll
    static void init()
            throws IOException
        {
        // dummy content for testing
        int test_binary_length = getRandomInRange(10, 1000);
        test_binary_content = new byte[test_binary_length];
        Random random = new Random();
        random.nextBytes(test_binary_content);
        //
        byte[] test_text_content = TEST_TEXT_CONTENT_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        //
        currentWorkingDirectory = Files.createTempDirectory("JUnitTests_");
        // create temp subdirectory
        tempDir1 = Files.createTempDirectory(currentWorkingDirectory, "blobReadingTest");

        // add test file 1 to it, with
        tempCfgFile1 = Files.createFile(tempDir1.resolve(CONFIG_NAME+".txt"));
        assertAccess(tempCfgFile1);
        // write some content into the file
        Files.write(tempCfgFile1, test_text_content);

        // add test file 2 to it, with
        tempCfgFile2 = Files.createFile(tempDir1.resolve(CONFIG_NAME+".bin"));
        assertAccess(tempCfgFile2);
        // write some content into the file
        Files.write(tempCfgFile2, test_binary_content);

        // ---------------------------------------------------------------------
        currentWorkingDirectory = Files.createTempDirectory("JUnitTests_");
        // create temp subdirectory
        tempDir2 = Files.createTempDirectory(currentWorkingDirectory, "blobWritingTest");

        tempCfgFile3 = Files.createFile(tempDir2.resolve(CONFIG_NAME+"-overwrite"));
        assertAccess(tempCfgFile3);
        Files.write(tempCfgFile3, "content to be overwritten".getBytes(StandardCharsets.UTF_8));
        // ---------------------------------------------------------------------

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
        assertTrue(tempCfgFile3.toFile().delete());
        assertTrue(tempCfgFile2.toFile().delete());
        assertTrue(tempCfgFile1.toFile().delete());
        assertTrue(tempDir2.toFile().delete());
        assertTrue(tempDir1.toFile().delete());
        assertTrue(currentWorkingDirectory.toFile().delete());
        }

    public static int getRandomInRange(int min, int max)
        { return (int) ((Math.random()*(max-min))+min); }
}
