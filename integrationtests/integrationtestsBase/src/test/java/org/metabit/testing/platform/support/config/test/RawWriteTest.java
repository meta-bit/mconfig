package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RawWriteTest
{
    private static final String COMPANY_NAME     = "metabit";
    private static final String APPLICATION_NAME = "mConfigIT";

    @Test
    void testWriteRawText() throws IOException
        {
        Path tempDir = Files.createTempDirectory("mconfig-test-text");
        try
            {
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(tempDir.toString()));
            builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, List.of("text_file"));
            
            try (ConfigFactory factory = builder.build())
                {
                Configuration cfg = factory.getConfig("writeTestText");
                String content = "Hello persistent world!";
                cfg.put("", content, ConfigScope.USER);
                
                // Verify file exists on disk
                Path expectedFile = tempDir.resolve("writeTestText.cfg"); // .cfg before .txt
                assertTrue(Files.exists(expectedFile), "File should be created at " + expectedFile);
                assertEquals(content, Files.readString(expectedFile));
                
                // Verify reading it back through the API
                assertEquals(content, cfg.getString(""));
                }
            }
        finally
            {
            // cleanup
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> p.toFile().delete());
            }
        }

    @Test
    void testWriteRawBinary() throws IOException
        {
        Path tempDir = Files.createTempDirectory("mconfig-test-bin");
        try
            {
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(tempDir.toString()));
            builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, List.of("binary_file"));
            
            try (ConfigFactory factory = builder.build())
                {
                Configuration cfg = factory.getConfig("writeTestBin");
                byte[] content = new byte[]{0, 1, 2, 3, 4, 5};
                cfg.put("", content, ConfigScope.USER);
                
                // Verify file exists on disk
                Path expectedFile = tempDir.resolve("writeTestBin.bin");
                assertTrue(Files.exists(expectedFile));
                assertArrayEquals(content, Files.readAllBytes(expectedFile));
                
                // Verify reading it back
                assertArrayEquals(content, cfg.getBytes(""));
                }
            }
        finally
            {
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> p.toFile().delete());
            }
        }

    @Test
    void testSecurityPathTraversal()
        {
        assertThrows(ConfigException.class, () -> ConfigFactoryBuilder.create("../malicious", APPLICATION_NAME));
        assertThrows(ConfigException.class, () -> ConfigFactoryBuilder.create(COMPANY_NAME, "app/traversal"));
        assertThrows(ConfigException.class, () -> ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME, "sub/../path"));
        }

    @Test
    void testSubPathMultiLevel() throws IOException
        {
        Path tempDir = Files.createTempDirectory("mconfig-test-subpath");
        try
            {
            String subPath = "level1/level2/level3";
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME, subPath);
            builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of(tempDir.toString()));
            builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, List.of("text_file"));

            try (ConfigFactory factory = builder.build())
                {
                Configuration cfg = factory.getConfig("subPathTest");
                String content = "Nested content";
                
                // Ensure directories exist so mConfig can find the location if it checks for existence
                // Some versions of mConfig might only add existing directories to the search list
                Path targetDir = tempDir.resolve(subPath);
                Files.createDirectories(targetDir);
                
                cfg.put("", content, ConfigScope.USER);

                // Verify file exists on disk in the correct nested directory
                List<ConfigLocation> locs = cfg.getSourceLocations();
                ConfigLocation loc = locs.stream()
                        .filter(l -> l.getStorageInstanceHandle() instanceof Path)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("No source location with Path handle found"));
                
                Path actualPath = (Path) loc.getStorageInstanceHandle();
                assertNotNull(actualPath, "Storage instance handle should not be null");
                
                Path expectedFile;
                if (Files.isRegularFile(actualPath))
                    expectedFile = actualPath;
                else
                    expectedFile = actualPath.resolve("subPathTest.txt");
                
                assertTrue(Files.exists(expectedFile), "File should be created at " + expectedFile);
                assertEquals(content, Files.readString(expectedFile));
                
                // Double check it's actually in our temp dir under the subpath
                assertTrue(expectedFile.startsWith(tempDir), "File should be within the temporary directory");
                assertTrue(expectedFile.toString().contains(subPath.replace("/", expectedFile.getFileSystem().getSeparator())), 
                        "File path should contain the subPath components");
                }
            }
        finally
            {
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> p.toFile().delete());
            }
        }
}