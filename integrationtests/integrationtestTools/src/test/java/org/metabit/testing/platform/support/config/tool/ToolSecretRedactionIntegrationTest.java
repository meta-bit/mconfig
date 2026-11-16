package org.metabit.testing.platform.support.config.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.tool.Main;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MLIB-130: the CLI never prints secret values (schema "SECRET": true), in any output format;
 * key, scope and location stay visible. get --reveal shows a single value on explicit request.
 * Secrets stored intentionally in the file itself stay intact when the file is written.
 */
class ToolSecretRedactionIntegrationTest
{
    private static final String COMPANY  = "secretco";
    private static final String APP      = "secretapp";
    private static final String CONFIG   = "secretconf";
    private static final String PASSWORD = "s3cr3t-pw-value";
    private static final String TOKEN    = "tok-0123456789";
    private static final String MARKER   = "[REDACTED]";

    @TempDir
    Path tempDir;

    private Path configFile;

    @BeforeEach
    void setUp() throws IOException
        {
        configFile = tempDir.resolve(CONFIG+".properties");
        Files.write(configFile, ("db/host=dbserver.example\n"+"db/password="+PASSWORD+"\n"+"api/token="+TOKEN+"\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("TESTMODE_DIRECTORIES", "USER:"+tempDir);
        }

    @AfterEach
    void tearDown()
        {
        System.clearProperty("TESTMODE_DIRECTORIES");
        }

    @Test
    void getRedactsInAllFormats()
        {
        for (String format : new String[]{"HUMAN", "JSON", "YAML", "TOML", "CSV"})
            {
            String out = run("get", "-c", COMPANY, "-a", APP, "-C", CONFIG, "-K", "db/password", "-f", format);
            assertFalse(out.contains(PASSWORD), format+": secret value printed: "+out);
            assertTrue(out.contains(MARKER), format+": marker missing: "+out);
            assertTrue(out.contains("USER"), format+": scope must stay visible: "+out);
            }
        String json = run("get", "-c", COMPANY, "-a", APP, "-C", CONFIG, "-K", "db/password", "-f", "JSON");
        assertTrue(json.replace(" ", "").contains("\"secret\":true"), "JSON get must flag the secret: "+json);
        }

    @Test
    void getRevealShowsSingleValue()
        {
        String out = run("get", "-c", COMPANY, "-a", APP, "-C", CONFIG, "-K", "db/password", "--reveal");
        assertTrue(out.contains(PASSWORD), "--reveal must show the value: "+out);
        }

    @Test
    void nonSecretValuesStayVisible()
        {
        String out = run("get", "-c", COMPANY, "-a", APP, "-C", CONFIG, "-K", "db/host");
        assertTrue(out.contains("dbserver.example"), out);
        assertFalse(out.contains(MARKER), out);
        }

    @Test
    void showRedactsInAllFormats()
        {
        for (String format : new String[]{"HUMAN", "JSON", "YAML", "TOML", "CSV"})
            {
            String out = run("show", "-c", COMPANY, "-a", APP, "-C", CONFIG, "-f", format);
            assertFalse(out.contains(PASSWORD), format+": secret value printed: "+out);
            assertFalse(out.contains(TOKEN), format+": secret value printed: "+out);
            assertTrue(out.contains(MARKER), format+": marker missing: "+out);
            assertTrue(out.contains("dbserver.example"), format+": non-secret value missing: "+out);
            }
        }

    @Test
    void setDoesNotEchoSecretsButKeepsThemInTheFile() throws IOException
        {
        String newPassword = "n3w-pw-value";
        String out = run("set", "-c", COMPANY, "-a", APP, "-C", CONFIG, "-K", "db/password", "-S", "USER", "--value", newPassword);
        assertFalse(out.contains(newPassword), "set must not echo a secret: "+out);

        // same location: secrets stored intentionally must stay intact
        String file = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
        assertTrue(file.contains(newPassword), "new secret must be written to the file: "+file);
        assertTrue(file.contains(TOKEN), "other secret in the file must stay intact: "+file);
        assertFalse(file.contains(MARKER), "the marker must never reach the file: "+file);
        }

    /* runs the CLI and returns stdout and stderr combined */
    private String run(String... args)
        {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        try
            {
            System.setOut(capture);
            System.setErr(capture);
            CommandLine cmd = new CommandLine(new Main());
            cmd.setCaseInsensitiveEnumValuesAllowed(true);
            int exit = cmd.execute(args);
            capture.flush();
            String out = buffer.toString(StandardCharsets.UTF_8);
            assertEquals(0, exit, "command failed: "+String.join(" ", args)+"\n"+out);
            return out;
            }
        finally
            {
            System.setOut(oldOut);
            System.setErr(oldErr);
            }
        }
}
//___EOF___
