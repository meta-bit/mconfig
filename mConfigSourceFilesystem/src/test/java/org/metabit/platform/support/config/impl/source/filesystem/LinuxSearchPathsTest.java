package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigSearchList;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LinuxSearchPathsTest
    {
    @Test
    public void testEtcNotPoluted() throws Exception
        {
        Path tempDir = Files.createTempDirectory("mconfig_test_etc");
        try
            {
            // Create a dummy /etc structure
            Path etc = tempDir.resolve("etc");
            Files.createDirectories(etc);
            
            // Unrelated directory with .d (which FileConfigStorage picks up)
            Path unrelatedDir = etc.resolve("unrelated.d");
            Files.createDirectories(unrelatedDir);
            
            // Unrelated file with extension that matches our mock format
            Path unrelatedFile = etc.resolve("someconfig.properties");
            Files.createFile(unrelatedFile);

            FileConfigStorage storage = new FileConfigStorage();
            
            // Mock format
            ConfigFileFormatInterface mockFormat = new ConfigFileFormatInterface() {
                @Override public List<String> getFilenameExtensions() { return Collections.singletonList("properties"); }
                @Override public ConfigLayerInterface readFile(File file, ConfigLocation location) { return null; }
                @Override public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location) { return null; }
                @Override public ConfigLayerInterface createFile(Path fileWithFullPath, ConfigLocation location) { return null; }
                @Override public void writeFile(ConfigLayerInterface layer) {}
                @Override public String getFormatID() { return "PROPERTIES"; }
                @Override public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger) { return true; }
            };

            ConfigFactorySettings settings = new ConfigFactorySettings();
            ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
            
            Map<String, org.metabit.platform.support.config.interfaces.ConfigFormatInterface> formats = new HashMap<>();
            formats.put("PROPERTIES", mockFormat);
            ctx.setConfigFormats(formats);
            
            storage.init(ctx);
            
            LinuxSearchPaths lsp = new LinuxSearchPaths();
            ConfigSearchList searchList = new ConfigSearchList();
            
            // This tests that our change works:
            // Instead of manually adding etc.toAbsolutePath().toString() which simulated the bug,
            // we now use LinuxSearchPaths to populate the searchList and verify our temp etc is NOT in there
            // or if it is, it's with the subdirectories.
            
            // We need to pass a "real" company and app name to lsp.initSearchPaths
            String company = "ACME";
            String app = "RoadRunner";
            
            // However, LinuxSearchPaths has hardcoded /etc. We cannot easily redirect it to our temp etc
            // unless we use a mock storage that we can monitor, or we just trust that by removing the 
            // calls to addPathToSearchList with just "/etc/", the problem is solved.
            
            // Let's refine the test to use the real LinuxSearchPaths and check what it ADDS.
            ConfigSearchList realSearchList = new ConfigSearchList();
            lsp.initSearchPaths(realSearchList, company, app, null, storage);
            
            for (ConfigLocation loc : realSearchList.getEntries())
                {
                String locStr = loc.toLocationString();
                // It should NOT be exactly /etc/ or /etc or /etc/opt/ or /etc/opt
                // It should always have the app name or company name at the end (or both)
                assertFalse(locStr.equals("file:/etc/"), "Should not have plain /etc/ in search list: " + locStr);
                assertFalse(locStr.equals("file:/etc"), "Should not have plain /etc in search list: " + locStr);
                assertFalse(locStr.equals("file:/etc/opt/"), "Should not have plain /etc/opt/ in search list: " + locStr);
                assertFalse(locStr.equals("file:/etc/opt"), "Should not have plain /etc/opt in search list: " + locStr);
                }
            }
        finally
            {
            // cleanup
            deleteDirectory(tempDir.toFile());
            }
        }

    private void deleteDirectory(File directory)
        {
        File[] allContents = directory.listFiles();
        if (allContents != null)
            {
            for (File file : allContents)
                {
                deleteDirectory(file);
                }
            }
        directory.delete();
        }
    }
