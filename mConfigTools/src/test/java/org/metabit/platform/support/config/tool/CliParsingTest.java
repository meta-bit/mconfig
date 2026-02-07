package org.metabit.platform.support.config.tool;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigScope;
import picocli.CommandLine;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CliParsingTest
{
    @Test
    void testGetScopeParsingCaseInsensitive()
    {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);

        CommandLine.ParseResult result = cmd.parseArgs(
                "get",
                "myco:myapp:myconfig",
                "mykey",
                "--scope",
                "user,HOST"
        );

        CommandLine.ParseResult sub = result.subcommand();
        assertNotNull(sub);

        ConfigScope[] scopes = sub.matchedOptionValue("--scope", null);
        assertNotNull(scopes);
        assertEquals(2, scopes.length);
        assertTrue(Arrays.asList(scopes).contains(ConfigScope.USER));
        assertTrue(Arrays.asList(scopes).contains(ConfigScope.HOST));
        }

    @Test
    void testOutputFormatYamlTomlParsing()
    {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);

        CommandLine.ParseResult yamlResult = cmd.parseArgs("get", "myco:myapp:myconfig:mykey", "-f", "yaml");
        CommandLine.ParseResult yamlSub = yamlResult.subcommand();
        assertEquals(Main.OutputFormat.YAML, yamlSub.matchedOptionValue("-f", null));

        CommandLine.ParseResult tomlResult = cmd.parseArgs("get", "myco:myapp:myconfig:mykey", "-f", "Toml");
        CommandLine.ParseResult tomlSub = tomlResult.subcommand();
        assertEquals(Main.OutputFormat.TOML, tomlSub.matchedOptionValue("-f", null));
        }
}
