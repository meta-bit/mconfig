package org.metabit.testing.platform.support.config.test;

import org.metabit.platform.support.config.*;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class InteractiveChangeTestFromJAR
{
    private static Path tempdir;
    private static boolean loop;

    public static void main(String[] args)
            throws IOException
        {

        // set up Ctrl+C handler to flip our flag
        loop = true;

        // create temp dir
        tempdir = Files.createTempDirectory("test_mConfig_");
        try (
                ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                        // .setFeature(ConfigFeature.TEST_MODE,true) // -- testmode skips the search list setting if ./src/test does not exist at all.
                        // .setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:"+tempdir.toAbsolutePath()))
                        .setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(tempdir.toAbsolutePath().toString()))
                        .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY,false) // return null instead, please.
                        .build())
            {
            System.out.println("mConfig search list");
            for (ConfigLocation cse : factory.getSearchList())
                { System.out.println(cse.toString()); }

            Configuration cfg = factory.getConfig(CONFIG_NAME);
            if (cfg == null)
                throw new RuntimeException("cfg instance is null");

//@TODO WTF why does our config not have a file layer, just the default layer?!
            //------------------------------------------------------------------

            System.out.println("tempdir to create and edit files in: " + tempdir);
            System.out.println("in a different terminal, please create and edit a file called " + CONFIG_NAME+ ".properties (or another supported extension)");
            System.out.println("again: " + tempdir + "/" + CONFIG_NAME + ".properties");
            System.out.println("we'll check for an entry called \"test\".");
            System.out.println("use Ctrl+c to end the loop. In IntelliJ, you may need to use Ctrl+F2.");

            Console cons = System.console();
            String previousValue = null;
            do
                {
// System.out.println("...");
                String value = cfg.getString("test");
// System.out.println(value);
                if (!Objects.equals(value, previousValue))
                    {
                    System.out.println("OK\tchange detected! new value : \"" + value + "\".");
                    previousValue = value;
                    }
                TimeUnit.SECONDS.sleep(1);
                }
            while (loop);
            //@TODO more tests. e.g. check the layers
            }
        catch (InterruptedException e)
            {
            System.err.println(e.getMessage());
            }
        Files.deleteIfExists(tempdir);
        return;
        }

    static final String COMPANY_NAME     = "metabit";
    static final String APPLICATION_NAME = "mConfigIT";
    static final String CONFIG_NAME      = "testconfig";
}
