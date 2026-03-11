package org.metabit.testing.platform.support.config.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolVectorIntegrationTest
{
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("mconfig tool: vector with subdirectories and @key")
    void testVectorWithSubdirsAndKey() throws IOException
        {
        Path dir = tempDir.resolve("sub/dir");
        Files.createDirectories(dir);
        Path file = dir.resolve("myconf.properties");
        Files.writeString(file, "some.key=somevalue\n", StandardCharsets.UTF_8);

        // We use MCONFIG_ADDITIONAL_USER_DIRECTORIES to point to our temp dir
        // The path structure for USER scope is: <dir>/<company>/<app>/<subPath>/<conf>.<ext>
        // But mConfigSourceFilesystem adds <dir> as a root.
        // If we set subPath = "sub/dir", and application = "myapp", it looks for:
        // <dir>/.config/<company>/myapp/sub/dir/myconf.properties
        
        Path configRoot = tempDir.resolve("configroot");
        Path fullPath = configRoot.resolve(".config/mycompany/myapp/sub/dir/myconf.properties");
        Files.createDirectories(fullPath.getParent());
        Files.writeString(fullPath, "some.key=somevalue\n", StandardCharsets.UTF_8);

        // Run command: mconfig get mycompany:myapp:sub:dir:myconf@some.key
        // We need to set the environment variable for the process or mock it.
        // Since we are running in same JVM, we might need to rely on system property if we can't set env.
        // Wait, Main.java uses System.getenv().
        
        // For testing, I'll temporarily use System.setProperty and modify Main.java if needed,
        // but let's see if I can use a simpler approach.
        // Actually, I can just call the command and see if it parses correctly.
        
        CommandLine cmd = new CommandLine(new org.metabit.platform.support.config.tool.Main());
        // Use -c -a etc to bypass discovery for now just to test parsing
        String vector = "mycompany:myapp:sub:dir:myconf@some.key";
        
        org.metabit.platform.support.config.tool.Main main = new org.metabit.platform.support.config.tool.Main();
        main.shortened = vector;
        org.metabit.platform.support.config.tool.Main.ConfigContext ctx = new org.metabit.platform.support.config.tool.Main.ConfigContext(main, new org.metabit.platform.support.config.tool.CommonOptions());
        
        assertEquals("mycompany", ctx.company);
        assertEquals("myapp", ctx.application);
        assertEquals("sub/dir", ctx.subPath);
        assertEquals("myconf", ctx.configName);
        assertEquals("some.key", ctx.key);
        }

    @Test
    @DisplayName("mconfig tool: vector with leading colon (no company)")
    void testVectorNoCompany()
        {
        String vector = ":myapp:sub:dir:myconf@some.key";
        org.metabit.platform.support.config.tool.Main main = new org.metabit.platform.support.config.tool.Main();
        main.shortened = vector;
        org.metabit.platform.support.config.tool.Main.ConfigContext ctx = new org.metabit.platform.support.config.tool.Main.ConfigContext(main, new org.metabit.platform.support.config.tool.CommonOptions());
        
        assertEquals("", ctx.company);
        assertEquals("myapp", ctx.application);
        assertEquals("sub/dir", ctx.subPath);
        assertEquals("myconf", ctx.configName);
        assertEquals("some.key", ctx.key);
        }

    @Test
    @DisplayName("mconfig tool: vector swap verb/vector")
    void testVerbVectorSwap()
        {
        CommandLine cmd = new CommandLine(new org.metabit.platform.support.config.tool.Main());
        String[] args = {"mycompany:myapp:myconf@mykey", "get"};
        String[] normalized = org.metabit.platform.support.config.tool.Main.normalizeArgs(cmd, args);
        
        assertEquals("get", normalized[0]);
        assertEquals("mycompany:myapp:myconf@mykey", normalized[1]);
        }
}
