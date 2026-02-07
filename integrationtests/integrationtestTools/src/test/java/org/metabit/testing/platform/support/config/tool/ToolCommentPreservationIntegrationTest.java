package org.metabit.testing.platform.support.config.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.tool.Main;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolCommentPreservationIntegrationTest
{
    private static final String COMPANY = "myCompany";
    private static final String APP = "myApp";
    private static final String CONFIG = "myToolConfig";

    @TempDir
    Path tempDir;

    @AfterEach
    void clearTestModeProperty()
        {
        System.clearProperty("TESTMODE_DIRECTORIES");
        }

    @Test
    @DisplayName("mconfig set: preserve existing comments in TOML")
    void testSetPreservesCommentsTOML() throws IOException
        {
        Path dir = tempDir.resolve("toml_comments");
        Files.createDirectories(dir);
        Path file = dir.resolve(CONFIG + ".toml");
        
        String initialContent = "# Header\n" +
                                "# Key comment\n" +
                                "key = \"value\" # Inline\n";
        Files.writeString(file, initialContent, StandardCharsets.UTF_8);
        
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + dir.toString());
        
        int exit = runSetCommand("key", "newvalue", ConfigEntryType.STRING, "TOML");
        assertEquals(0, exit);
        
        String updatedContent = Files.readString(file, StandardCharsets.UTF_8);
        System.out.println("Updated TOML content:\n" + updatedContent);
        
        assertTrue(updatedContent.contains("# Header"), "Should preserve header");
        assertTrue(updatedContent.contains("# Key comment"), "Should preserve key leading comment");
        assertTrue(updatedContent.contains("# Inline"), "Should preserve inline comment");
        assertTrue(updatedContent.contains("key = \"newvalue\""), "Should update value");
        }

    @Test
    @DisplayName("mconfig set: preserve existing comments in Properties")
    void testSetPreservesCommentsProperties() throws IOException
        {
        Path dir = tempDir.resolve("props_comments");
        Files.createDirectories(dir);
        Path file = dir.resolve(CONFIG + ".properties");
        
        String initialContent = "# Header\n" +
                                "# Key comment\n" +
                                "key=value\n";
        Files.writeString(file, initialContent, StandardCharsets.UTF_8);
        
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + dir.toString());
        
        int exit = runSetCommand("key", "newvalue", ConfigEntryType.STRING, "PROPERTIES");
        assertEquals(0, exit);
        
        String updatedContent = Files.readString(file, StandardCharsets.UTF_8);
        System.out.println("Updated Properties content:\n" + updatedContent);
        
        assertTrue(updatedContent.contains("# Header"), "Should preserve header");
        assertTrue(updatedContent.contains("# Key comment"), "Should preserve key leading comment");
        assertTrue(updatedContent.contains("key=newvalue"), "Should update value");
        }

    private int runSetCommand(String key, String value, ConfigEntryType type, String format)
        {
        CommandLine cmd = new CommandLine(new Main());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);
        return cmd.execute("set",
                "-c", COMPANY,
                "-a", APP,
                "-C", CONFIG,
                "-K", key,
                "-S", "USER",
                "--file-format", format,
                "--type", type.name(),
                "--value", value
        );
        }
}
