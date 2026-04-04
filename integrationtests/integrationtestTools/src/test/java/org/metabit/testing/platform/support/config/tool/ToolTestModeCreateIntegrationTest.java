package org.metabit.testing.platform.support.config.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.tool.Main;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ToolTestModeCreateIntegrationTest
{
    private static final String COMPANY = "testCo";
    private static final String APP = "testApp";
    private static final String CONFIG = "testConf";

    @TempDir
    Path tempDir;

    @AfterEach
    void clearProperties()
        {
        System.clearProperty("TESTMODE_DIRECTORIES");
        }

    @Test
    @DisplayName("mconfig tool: set with --create in TEST_MODE succeeds and creates directory")
    void testSetSucceedsWithCreateInTestMode() throws IOException
        {
        Path dir = tempDir.resolve("non_existent_test_dir");
        // We simulate TEST_MODE by setting TESTMODE_DIRECTORIES system property
        // which the Main tool uses to enable test mode.
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + dir.toString());
        
        String vector = COMPANY + ":" + APP + ":" + CONFIG + "@mykey=myvalue";

        int exitSet = runMain("set", vector, "--create", "--file-format", "PROPERTIES",
                "--scope", "USER",
                "--allow-runtime-settings",
                "--debug");
        
        System.out.println("[DEBUG_LOG] Exit code: " + exitSet);
        System.out.println("[DEBUG_LOG] TESTMODE_DIRECTORIES: " + System.getProperty("TESTMODE_DIRECTORIES"));
        
        int exitGet = runMain("get", vector.split("@")[0] + "@mykey", 
                "--allow-runtime-settings", "--debug");
        System.out.println("[DEBUG_LOG] Get exit code: " + exitGet);

        assertEquals(0, exitSet, "Set command should succeed with --create flag even in test mode");
        assertTrue(Files.exists(dir), "Directory should have been created: " + dir);
        Path configFile = dir.resolve(CONFIG + ".properties");
        assertTrue(Files.exists(configFile), "Config file should have been created: " + configFile);
        }

    @Test
    @DisplayName("mconfig tool: set without --create in TEST_MODE fails if directory does not exist")
    void testSetFailsWithoutCreateInTestMode() throws IOException
        {
        Path dir = tempDir.resolve("another_non_existent_test_dir");
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + dir.toString());
        
        String vector = COMPANY + ":" + APP + ":" + CONFIG + "@mykey=myvalue";
        
        int exitSet = runMain("set", vector, "--file-format", "PROPERTIES",
                "--scope", "USER",
                "--allow-runtime-settings",
                "--debug");
        
        System.out.println("[DEBUG_LOG] Exit code: " + exitSet);
        assertNotEquals(0, exitSet, "Set command should fail when test directory does not exist and --create is missing");
        assertFalse(Files.exists(dir), "Directory should NOT have been created when --create is missing");
        }

    private int runMain(String... args)
        {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);

        String[] finalArgs = Main.normalizeArgs(cmd, args);
        
        return cmd.execute(finalArgs);
        }
}
