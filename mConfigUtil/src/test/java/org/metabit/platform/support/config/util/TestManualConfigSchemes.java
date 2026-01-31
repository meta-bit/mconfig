package org.metabit.platform.support.config.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestManualConfigSchemes
{

    public static final String               COMPANY_NAME     = "metabit";
    public static final String               APPLICATION_NAME = "CONFIGTEST";
    private static      ConfigFactoryBuilder configFactoryBuilder;
    private static      Path                 tempDir1;
    private static      Path                 tempDir2;
    private static      Path                 tempCfgFile1;
    private static      Path                 tempCfgFile2;
    @TempDir
    static              Path                 currentWorkingDirectory;

    @BeforeAll
    static void init()
            throws ConfigCheckedException, IOException
        {
        //    Path currentWorkingDirectory = Paths.get(".");
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
        Files.write(tempCfgFile1, "testdata=testvalue\nsecond=2".getBytes(StandardCharsets.UTF_8));
        Files.write(tempCfgFile2, "testdata=othertestvalue\nthird=3".getBytes(StandardCharsets.UTF_8));


        // add temp subdirectory to search path in builder?
        configFactoryBuilder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        // prepend this temp directory to our config directory search list
        configFactoryBuilder.setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, Arrays.asList(tempDir1.toAbsolutePath().toString(), tempDir2.toAbsolutePath().toString()));
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

    @Test
    void testConfigAccessSingleStringEntries()
            throws ConfigCheckedException
        {
        configFactoryBuilder.setFeature(ConfigFeature.LOGLEVEL_NUMBER, 5);

        // configFactoryBuilder.setFeature(ConfigFactory.Feature.CONFIG_Scheme_MAP, SchemeMap);

        ConfigFactory configFactory = configFactoryBuilder.build();

// List<ConfigScheme> good = new ArrayList<>(2);
// good.add(ConfigScheme.statics.fromStrings(new String[]{"", "", ""}));
        // configFactory.addScheme();
        Set<ConfigSchemeEntry> dummies = new HashSet<>();
        dummies.add(new ConfigSchemeEntry("testkey", ConfigEntryType.STRING).setDescription("test entry"));

        dummies.add(new ConfigSchemeEntry("second", ConfigEntryType.NUMBER).setDescription("test number").setDefault("4711"));
        ConfigScheme Scheme = ConfigScheme.fromSchemeEntries(dummies); // was ConfigScheme.statics. ...
        // @TODO no, we need to have this ConfigScheme generation function in the CFB or CF, where the settings are accessible!!
        // the ConfigSchemeImpl functions need to have the ctx.
        // not just for the logger-  but most of all, for the settings! they need to check the s dfs

        // reading the config without a scheme set; the contents are passed as strings, without checking.
        Configuration cfg = configFactory.getConfig("testconfig"); // from test-properties file
        String value = cfg.getString("testdata");
        assertEquals("othertestvalue", value); // empty string, but no exception

        // now with Scheme, and implicitly activated flag - so the "testdata" entry should fail the scheme tests.
        cfg.setConfigScheme(Scheme);
        try
            {
            value = cfg.getString("testdata"); // doesn't exist, NO_MATCHING_ENTRY exception
            }
        catch (ConfigException cex)
            {
            // expected! we haven't defined this entry, so there is no matching entry.
            assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, cex.getReason());
            }
        // we're getting the most specific/last one from the list.
        // this is not a side effect, but the intended and documented behaviour.

        // provoke entry not found exception
        ConfigException thrown = assertThrows(ConfigException.class, ()->
            {
            String dummy = cfg.getString("ThisKeyDoesNotExist");
            System.out.println(dummy);
            });
        assertEquals(ConfigException.ConfigExceptionReason.NO_MATCHING_ENTRY, thrown.getReason());
        }

/* empty schemes throw an exception. isn't that as it should be,
    because they are indicative of an issue?
    @Test
    public void testSchemePreset()
            throws ConfigCheckedException
        {
        // might return a list anyhow
        Map<String,ConfigScheme> schemingLizard = ConfigScheme.statics.fromJSON("", null);
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME,APPLICATION_NAME)
                .setSchemes(schemingLizard)
                .build())
            {
            // test we've got the scheme active
            }
        return;
        }
*/

    @Test
    protected void testBuilderChaining()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.FALLBACKS_ACROSS_SCOPES, true)
                .setFeature(ConfigFeature.COMPANY_NAME, "differentCompany")
                .build())
            {

            }
        return;
        }

}


//___EOF___
