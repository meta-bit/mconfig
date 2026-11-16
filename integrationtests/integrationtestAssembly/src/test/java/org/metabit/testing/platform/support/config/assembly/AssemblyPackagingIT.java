package org.metabit.testing.platform.support.config.assembly;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MLIB-156: single-JAR packaging with maven-assembly-plugin.
 * <p>
 * The JARs are built in the package phase (see pom.xml); this test starts each in its own JVM with
 * {@code java -jar}, as an application would be started.
 */
class AssemblyPackagingIT
{
    @Test
    void unmergedServiceFilesFailAtStartupWithPackagingReport() throws Exception
        {
        Result r = run(jar("assembly.jar.unmerged"));
        assertNotEquals(0, r.exitCode, "must not start with overwritten service files:\n"+r.output);
        assertTrue(r.output.contains("JAVA_SERVICE_CONFIGURATION_ERROR"), r.output);
        assertTrue(r.output.contains("faulty JAR packaging"), r.output);
        assertTrue(r.output.contains("META-INF/services/"), r.output);
        assertTrue(r.output.contains("Packaging as a single JAR"), "points to the documentation:\n"+r.output);
        assertFalse(r.output.contains("port="), r.output);
        }

    @Test
    void unmergedServiceFilesStartWithWarningWhenAllowed() throws Exception
        {
        Result r = run(jar("assembly.jar.unmerged"), "allow");
        assertEquals(0, r.exitCode, r.output);
        assertTrue(r.output.contains("faulty JAR packaging"), "report is logged as a warning:\n"+r.output);
        assertTrue(r.output.contains("started"), r.output);
        }

    @Test
    void mergedServiceFilesWork() throws Exception
        {
        Result r = run(jar("assembly.jar.merged"));
        assertEquals(0, r.exitCode, r.output);
        assertFalse(r.output.contains("faulty JAR packaging"), r.output);
        assertTrue(r.output.contains("port=8123"), "bundled default read via the JAR source:\n"+r.output);
        }

    // ---------------------------------------------------------------------------------------------------------

    private static Path jar(String property)
        {
        String name = System.getProperty(property);
        assertNotNull(name, "system property "+property+" not set; run via maven-failsafe-plugin");
        Path path = Paths.get(name);
        assertTrue(Files.isRegularFile(path), "assembly not built: "+path);
        return path;
        }

    private static Result run(Path jar, String... args) throws IOException, InterruptedException
        {
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(jar.toString());
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream())
            {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) >= 0)
                {
                out.write(buffer, 0, n);
                }
            }
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "process did not end");
        return new Result(process.exitValue(), out.toString(StandardCharsets.UTF_8.name()));
        }

    private static final class Result
    {
        final int    exitCode;
        final String output;

        Result(int exitCode, String output)
            {
            this.exitCode = exitCode;
            this.output = output;
            }
    }
}
//___EOF___
