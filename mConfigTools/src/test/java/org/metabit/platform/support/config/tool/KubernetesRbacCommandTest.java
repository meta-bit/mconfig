package org.metabit.platform.support.config.tool;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KubernetesRbacCommandTest
{
    @Test
    void schemaOptionIsOptional()
    {
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try
        {
            assertEquals(0, new CommandLine(new Main()).execute("kubernetes-rbac"));
        }
        finally
        {
            System.setOut(oldOut);
        }
    }

    @Test
    void testRbacGeneration() throws Exception
    {
        Path schemaPath = Files.createTempFile("test-schema", ".mconfig-schema.json");
        Files.writeString(schemaPath, "{}");

        Main main = new Main();
        CommandLine cmd = new CommandLine(main);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream oldOut = System.out;
        System.setOut(ps);

        try
        {
            int exitCode = cmd.execute("kubernetes-rbac", "--schema", schemaPath.toString(), "--namespace", "my-ns", "--service-account", "my-sa");
            assertEquals(0, exitCode);
            
            String output = baos.toString();
            assertTrue(output.contains("namespace: my-ns"));
            assertTrue(output.contains("name: my-sa"));
            assertTrue(output.contains("kind: Role"));
            assertTrue(output.contains("kind: RoleBinding"));
            // MLIB-150: the template must not claim least privilege, and must not grant list
            assertTrue(output.contains("NOT LEAST PRIVILEGE"));
            assertTrue(output.contains("verbs: [\"get\", \"watch\"]"));
            assertFalse(output.contains("\"list\""));
        }
        finally
        {
            System.setOut(oldOut);
            Files.deleteIfExists(schemaPath);
        }
    }
}
