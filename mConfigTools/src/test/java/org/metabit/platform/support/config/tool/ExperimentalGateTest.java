package org.metabit.platform.support.config.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * the --experimental gate of the mconfig CLI (MLIB-151).
 */
class ExperimentalGateTest
{
    private Main        main;
    private CommandLine cmd;
    private PrintStream oldErr;

    @BeforeEach
    void setUp()
        {
        // same setup as Main.main()
        main = new Main();
        cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);
        oldErr = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        }

    @AfterEach
    void tearDown()
        {
        System.setErr(oldErr);
        }

    private boolean gate(String... args)
        {
        return Main.applyExperimentalGate(cmd, main, Main.normalizeArgs(cmd, args));
        }

    @Test
    void experimentalCommandsAreTheExpectedOnes()
        {
        // removing a name means the command is complete; update MLIB-151 and the docs with it
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList("validate", "propose-schema", "kubernetes-rbac")), Main.EXPERIMENTAL_COMMANDS);
        for (String name : Main.EXPERIMENTAL_COMMANDS)
            {
            assertTrue(cmd.getSubcommands().containsKey(name), "not a registered command: " + name);
            }
        }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "propose-schema", "kubernetes-rbac"})
    void refusedWithoutExperimental(String name)
        {
        assertFalse(gate(name));
        assertTrue(cmd.getSubcommands().get(name).getCommandSpec().usageMessage().hidden());
        }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "propose-schema", "kubernetes-rbac"})
    void acceptedWithExperimentalBeforeTheCommand(String name)
        {
        assertTrue(gate("-X", name));
        assertFalse(cmd.getSubcommands().get(name).getCommandSpec().usageMessage().hidden());
        }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "propose-schema", "kubernetes-rbac"})
    void acceptedWithExperimentalAfterTheCommand(String name)
        {
        assertTrue(gate(name, "--experimental"));
        }

    @Test
    void regularCommandsAreNotAffected()
        {
        assertTrue(gate("search-paths"));
        assertFalse(cmd.getSubcommands().get("search-paths").getCommandSpec().usageMessage().hidden());
        }
}
//___EOF___
