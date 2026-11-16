package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Compile the example and starter sources against the *current* API.
 * <p>
 * The starter projects under {@code examples/} are not part of the Maven reactor - they are
 * standalone copy-paste templates that pin a released mConfig version on purpose. That means
 * nothing ever compiled them, and they silently rotted: they referenced a {@code Config} type
 * and a {@code factory.getConfiguration()} method that no longer exist.
 * <p>
 * This test closes that gap without pulling the starters into the build. It runs the JDK
 * compiler in-process over their sources, using this module's own test classpath, so it needs
 * no network, no separate build tool, and no runtime environment - it only answers "does the
 * example code still compile against the API as it stands today?".
 * <p>
 * Each starter is compiled into its own output directory: all three declare the same
 * fully-qualified class {@code com.example.starter.Main} and cannot share a source root.
 * <p>
 * Note this deliberately does not build the starters' own pom.xml / build.gradle.kts files.
 * Those resolve released artifacts from the network and belong in a release-time job, not here.
 */
class ExampleSourcesCompileTest
{
    /** relative to the repository root. */
    private static final List<String> EXAMPLE_SOURCE_ROOTS = Arrays.asList(
            "examples/starters/maven-minimal/src/main/java",
            "examples/starters/gradle-kotlin/src/main/java",
            "examples/starters/jpms-maven/src/main/java",
            "examples/snippets",
            "examples");

    /** the templates file, and the directory with the sources its Java blocks are taken from. */
    private static final String  SNIPPETS_FILE        = "examples/snippets.md";
    private static final String  SNIPPETS_SOURCE_ROOT = "examples/snippets";
    private static final Pattern SNIPPET_REFERENCE    = Pattern.compile("^<!--\\s*snippet:\\s*([A-Za-z0-9_]+\\.java)#([a-z0-9-]+)\\s*-->$");
    private static final Pattern SNIPPET_START        = Pattern.compile("^\\s*// snippet:start ([a-z0-9-]+)\\s*$");
    private static final Pattern SNIPPET_END          = Pattern.compile("^\\s*// snippet:end ([a-z0-9-]+)\\s*$");

    @TempDir
    Path outputRoot;

    @Test
    @DisplayName("example and starter sources compile against the current API")
    void exampleSourcesCompile() throws IOException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "no JDK compiler available (running on a JRE?) - skipping");

        Path repoRoot = findRepositoryRoot();
        assumeTrue(repoRoot != null, "repository root with examples/ not found - skipping");

        String classpath = System.getProperty("java.class.path");
        // guard against a surefire setup where java.class.path is only a booter jar:
        // in that case we cannot compile against the library and would silently pass.
        assumeTrue(classpath != null && classpath.contains("mconfigcore"),
                "mConfig classes not on java.class.path - cannot verify, skipping");

        List<String> failures = new ArrayList<>();
        int compiledFiles = 0;

        for (String relativeRoot : EXAMPLE_SOURCE_ROOTS)
            {
            Path sourceRoot = repoRoot.resolve(relativeRoot);
            if (!Files.isDirectory(sourceRoot))
                {
                failures.add(relativeRoot + " -> source root does not exist");
                continue;
                }

            List<File> sources = collectJavaSources(sourceRoot);
            if (sources.isEmpty())
                {
                continue; // nothing to check in this root
                }
            compiledFiles += sources.size();

            String result = compile(compiler, sources, classpath,
                    Files.createDirectories(outputRoot.resolve(sanitize(relativeRoot))));
            if (result != null)
                {
                failures.add(relativeRoot + " ->\n" + result);
                }
            }

        assertTrue(compiledFiles > 0, "no example sources were found to compile - check EXAMPLE_SOURCE_ROOTS");

        if (!failures.isEmpty())
            {
            fail("example sources no longer compile against the current API.\n"
                 + "These are user-facing copy-paste templates; fix the examples, not this test.\n\n"
                 + String.join("\n\n", failures));
            }
    }

    /**
     * The Java blocks of examples/snippets.md are copies of marked regions in the compiled sources
     * under examples/snippets. A block preceded by {@code <!-- snippet: File.java#name -->} must
     * equal the lines between {@code // snippet:start name} and {@code // snippet:end name} in that
     * file (indentation aside), and every marked region must be shown in the file. So a template
     * cannot go stale without this test, or the compile test above, noticing.
     */
    @Test
    @DisplayName("Java blocks in examples/snippets.md match their compiled sources")
    void snippetsFileMatchesItsSources() throws IOException
    {
        Path repoRoot = findRepositoryRoot();
        assumeTrue(repoRoot != null, "repository root with examples/ not found - skipping");
        Path snippetsFile = repoRoot.resolve(SNIPPETS_FILE);
        Path sourceRoot = repoRoot.resolve(SNIPPETS_SOURCE_ROOT);
        assertTrue(Files.isRegularFile(snippetsFile), SNIPPETS_FILE + " does not exist");

        // all marked regions of all source files, as "File.java#name" -> lines
        Map<String, List<String>> regions = new TreeMap<>();
        for (File source : collectJavaSources(sourceRoot))
            {
            readRegions(source.toPath(), regions);
            }
        assertTrue(!regions.isEmpty(), "no snippet regions found under " + SNIPPETS_SOURCE_ROOT);

        List<String> failures = new ArrayList<>();
        Set<String> shown = new TreeSet<>();
        List<String> lines = Files.readAllLines(snippetsFile);
        for (int i = 0; i < lines.size(); i++)
            {
            Matcher reference = SNIPPET_REFERENCE.matcher(lines.get(i).trim());
            if (!reference.matches())
                {
                continue;
                }
            String id = reference.group(1) + "#" + reference.group(2);
            int fence = i + 1;
            while (fence < lines.size() && lines.get(fence).trim().isEmpty())
                {
                fence++;
                }
            if (fence >= lines.size() || !lines.get(fence).trim().equals("```java"))
                {
                failures.add(id + " (line " + (i + 1) + "): not followed by a ```java block");
                continue;
                }
            List<String> block = new ArrayList<>();
            int end = fence + 1;
            while (end < lines.size() && !lines.get(end).trim().equals("```"))
                {
                block.add(lines.get(end));
                end++;
                }
            List<String> expected = regions.get(id);
            if (expected == null)
                {
                failures.add(id + " (line " + (i + 1) + "): no such region in " + SNIPPETS_SOURCE_ROOT);
                continue;
                }
            shown.add(id);
            if (!normalize(expected).equals(normalize(block)))
                {
                failures.add(id + " (line " + (i + 1) + "): the block differs from its source. Source is:\n"
                             + String.join("\n", normalize(expected)));
                }
            }

        for (String id : regions.keySet())
            {
            if (!shown.contains(id))
                {
                failures.add(id + ": marked in the sources, but not shown in " + SNIPPETS_FILE);
                }
            }

        if (!failures.isEmpty())
            {
            fail(SNIPPETS_FILE + " and the sources under " + SNIPPETS_SOURCE_ROOT + " have drifted apart.\n"
                 + "The sources are compiled and authoritative; copy them into the file.\n\n"
                 + String.join("\n\n", failures));
            }
    }

    private static void readRegions(Path source, Map<String, List<String>> regions) throws IOException
    {
        String fileName = source.getFileName().toString();
        String open = null;
        List<String> current = null;
        for (String line : Files.readAllLines(source))
            {
            Matcher start = SNIPPET_START.matcher(line);
            Matcher end = SNIPPET_END.matcher(line);
            if (start.matches())
                {
                if (open != null)
                    {
                    fail(fileName + ": region " + start.group(1) + " starts inside region " + open);
                    }
                open = start.group(1);
                current = new ArrayList<>();
                }
            else if (end.matches())
                {
                if (!end.group(1).equals(open))
                    {
                    fail(fileName + ": end of region " + end.group(1) + " without its start");
                    }
                if (regions.put(fileName + "#" + open, current) != null)
                    {
                    fail(fileName + ": region " + open + " is marked twice");
                    }
                open = null;
                }
            else if (open != null)
                {
                current.add(line);
                }
            }
        if (open != null)
            {
            fail(fileName + ": region " + open + " is not closed");
            }
    }

    /** remove the common indentation and trailing whitespace; nothing else. */
    private static List<String> normalize(List<String> lines)
    {
        int indent = Integer.MAX_VALUE;
        for (String line : lines)
            {
            if (!line.trim().isEmpty())
                {
                indent = Math.min(indent, line.length() - line.stripLeading().length());
                }
            }
        List<String> result = new ArrayList<>();
        for (String line : lines)
            {
            result.add(line.trim().isEmpty() ? "" : line.substring(indent).stripTrailing());
            }
        return result;
    }

    /**
     * collect .java sources, excluding module-info.java: the JPMS starter's module descriptor
     * requires a module path setup that this plain-classpath check does not model.
     */
    private static List<File> collectJavaSources(Path root) throws IOException
    {
        try (Stream<Path> walk = Files.walk(root))
            {
            return walk.filter(Files::isRegularFile)
                    .filter(p->p.toString().endsWith(".java"))
                    .filter(p->!"module-info.java".equals(p.getFileName().toString()))
                    // only this directory level for the flat "examples" root, to avoid
                    // picking the starters up a second time under a different output dir
                    .filter(p->!root.getFileName().toString().equals("examples")
                               || p.getParent().equals(root))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            }
    }

    /**
     * @return null on success, or the collected compiler diagnostics
     */
    private static String compile(JavaCompiler compiler, List<File> sources, String classpath, Path outputDir)
            throws IOException
    {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null))
            {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

            List<String> options = Arrays.asList("-classpath", classpath, "-nowarn");
            boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null,
                    fileManager.getJavaFileObjectsFromFiles(sources)).call();

            if (ok)
                {
                return null;
                }
            return diagnostics.getDiagnostics().stream()
                    .filter(d->d.getKind() == Diagnostic.Kind.ERROR)
                    .map(d->"  " + (d.getSource() == null ? "?" : d.getSource().getName())
                            + ":" + d.getLineNumber() + " " + d.getMessage(null))
                    .collect(Collectors.joining("\n"));
            }
    }

    /** walk up from the working directory until the examples/starters directory is found. */
    private static Path findRepositoryRoot()
    {
        Path candidate = Paths.get("").toAbsolutePath();
        while (candidate != null)
            {
            if (Files.isDirectory(candidate.resolve("examples/starters")))
                {
                return candidate;
                }
            candidate = candidate.getParent();
            }
        return null;
    }

    private static String sanitize(String relativePath)
    {
        return relativePath.replaceAll("[^A-Za-z0-9]+", "_");
    }
}
