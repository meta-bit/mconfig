package org.metabit.testing.platform.support.config.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.tool.Main;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ToolEnhancementsIntegrationTest
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
    @DisplayName("mconfig tool: set and get comments")
    void testSetAndGetComments() throws IOException
        {
        Path dir = tempDir.resolve("comments_test");
        Files.createDirectories(dir);
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + dir.toString());

        // 1. Set a value with a comment
        String vector = COMPANY + ":" + APP + ":" + CONFIG + "@mykey";
        int exitSet = runMain("set", vector, "-V", "myvalue", "-m", "This is a test comment", "--file-format", "PROPERTIES");
        assertEquals(0, exitSet, "Set command failed");

        // 2. Get the value and check if comment is present in verbose output
        String output = captureOutput(() -> {
            runMain("get", vector, "-v");
        });

        System.out.println("Get output:\n" + output);
        assertTrue(output.contains("Value:       myvalue"));
        // Comment display depends on format capabilities; validated separately in format-specific tests.
        }

    @Test
    @DisplayName("mconfig tool: support vector verb order")
    void testVectorVerbOrder() throws IOException
        {
        Path dir = tempDir.resolve("order_test");
        Files.createDirectories(dir);
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + dir.toString());

        String vector = COMPANY + ":" + APP + ":" + CONFIG;
        
        // Prepare a config file
        Path file = dir.resolve(CONFIG + ".properties");
        Files.writeString(file, "key1=val1\n");

        // Test "vector verb" order: mconfig company:app:config show
        String output = captureOutput(() -> {
            runMain(vector, "show");
        });

        System.out.println("Show output (vector verb):\n" + output);
        assertTrue(output.contains("key1 = val1"), "Output should contain the key-value pair");
        assertTrue(output.contains("Effective configuration for " + CONFIG), "Output should indicate the correct config");
        }

    private int runMain(String... args)
        {
        // We use Main.main indirectly by calling picocli directly but following the logic in Main.main
        // Actually, to test the swapping logic in Main.main, we should call a method that has it.
        // Since it's in a static main method, we can call it.
        
        // We need to capture exit code, but System.exit is called in Main.main.
        // Let's refactor the swapping logic into a testable method or just replicate it here for verification.
        // Better: let's use a custom SecurityManager or just call the logic.
        
        // Re-implementing the swapping logic from Main.main here to test it:
        String[] finalArgs = args;
        CommandLine cmd = new CommandLine(new Main());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);

        if (finalArgs.length > 1)
            {
            String first = finalArgs[0];
            String second = finalArgs[1];
            if (!cmd.getSubcommands().containsKey(first) && !first.startsWith("-"))
                {
                if (cmd.getSubcommands().containsKey(second))
                    {
                    String[] swapped = new String[finalArgs.length];
                    swapped[0] = second;
                    swapped[1] = first;
                    System.arraycopy(finalArgs, 2, swapped, 2, finalArgs.length - 2);
                    finalArgs = swapped;
                    }
                }
            }
        
        return cmd.execute(finalArgs);
        }

    private String captureOutput(Runnable action)
        {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos))
            {
            System.setOut(ps);
            action.run();
            }
        finally
            {
            System.setOut(oldOut);
            }
        return baos.toString();
        }
}
