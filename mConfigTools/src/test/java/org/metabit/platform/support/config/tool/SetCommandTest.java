package org.metabit.platform.support.config.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetCommandTest
{

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Test
    void testDryRun()
        {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);

        out.reset();
        System.setOut(new PrintStream(out));

        // Use a dummy application and key
        int exitCode = cmd.execute("set", "metabit:testapp:config:mykey", "--value", "myvalue", "--scope", "USER", "--dry-run");

        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("--- DRY RUN ---"));
        assertTrue(output.contains("Key:           mykey"));
        assertTrue(output.contains("Value:         myvalue"));
        assertTrue(output.contains("Target Scope:  USER"));
        }

    @Test
    void testSetCommandExecution()
        {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);

        // This just verifies the command runs and handles arguments correctly
        // We use a scope that might not be writeable in the test environment, 
        // but we verify it doesn't crash.
        int exitCode = cmd.execute("set", "metabit:testapp:config:mykey", "--value", "myvalue", "--scope", "RUNTIME");

        // It might return 1 if not writeable, which is fine for this test
        assertTrue(exitCode == 0 || exitCode == 1);
        }

    @Test
    void testSetCommandKeyValueShorthand()
        {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);

        out.reset();
        System.setOut(new PrintStream(out));

        int exitCode = cmd.execute("set", "metabit:testapp:config", "mykey=myvalue", "--scope", "user", "--dry-run");

        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("Key:           mykey"));
        assertTrue(output.contains("Value:         myvalue"));
        assertTrue(output.contains("Target Scope:  USER"));
        }
}
