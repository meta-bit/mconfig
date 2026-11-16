package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * .d fragments whose file name contains control characters are skipped (A8, CWE-117).
 */
public class UnsafeFragmentNameTest
{
    private static final String COMPANY_NAME     = "metabit";
    private static final String APPLICATION_NAME = "UnsafeFragmentNameTest";
    private static final String CONFIG_NAME      = "network";

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException
        {
        tempDir = Files.createTempDirectory("mConfigUnsafeFragmentNameTest");
        }

    @AfterEach
    void tearDown() throws IOException
        {
        try (Stream<Path> pathStream = Files.walk(tempDir))
            {
            pathStream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }

    @Test
    void fragmentWithLineBreakInNameIsSkipped() throws IOException, ConfigCheckedException
        {
        Files.write(tempDir.resolve(CONFIG_NAME + ".properties"), List.of("key=main"));
        Path dotDDir = Files.createDirectory(tempDir.resolve(CONFIG_NAME + ".d"));
        Files.write(dotDDir.resolve("01-valid.properties"), List.of("key=valid"));
        try
            {
            // sorts after 01-valid, so it would override it if it were read
            Files.write(dotDDir.resolve("02-evil\nINFO [mConfig] forged.properties"), List.of("key=evil"));
            }
        catch (InvalidPathException | IOException e)
            {
            assumeTrue(false, "this filesystem does not allow line breaks in file names");
            }

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + tempDir.toAbsolutePath()));
        try (ConfigFactory factory = builder.build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            assertEquals("valid", cfg.getString("key"), "a fragment with a line break in its name must not be read");
            }
        }
}
//___EOF___
