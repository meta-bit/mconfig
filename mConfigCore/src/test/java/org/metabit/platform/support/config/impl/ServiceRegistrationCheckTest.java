package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MLIB-156: core's own service registrations replaced by another module's (fat JAR with unmerged META-INF/services).
 */
class ServiceRegistrationCheckTest
{
    private static final String STORAGE_IF   = "org.metabit.platform.support.config.interfaces.ConfigStorageInterface";
    private static final String LOGGING_IF   = "org.metabit.platform.support.config.interfaces.ConfigLoggingInterface";
    private static final String FILE_STORAGE = "org.metabit.platform.support.config.impl.source.filesystem.FileConfigStorage";

    @TempDir
    Path tmp;

    @Test
    void coreRegistrationsPresentPass() throws IOException
        {
        writeCoreServiceFiles(tmp);
        try (URLClassLoader cl = isolatedLoader(tmp))
            {
            assertNull(ServiceRegistrationCheck.findMissingRegistrations(cl));
            }
        }

    @Test
    void separateJarsPass() throws IOException
        {
        // regular classpath: one service file per module, all visible
        Path core = Files.createDirectory(tmp.resolve("core"));
        Path fs = Files.createDirectory(tmp.resolve("fs"));
        writeCoreServiceFiles(core);
        writeServiceFile(fs, STORAGE_IF, FILE_STORAGE);
        try (URLClassLoader cl = new URLClassLoader(new URL[]{core.toUri().toURL(), fs.toUri().toURL()}, null))
            {
            assertNull(ServiceRegistrationCheck.findMissingRegistrations(cl));
            }
        }

    @Test
    void currentClasspathPasses()
        {
        assertNull(ServiceRegistrationCheck.findMissingRegistrations(getClass().getClassLoader()));
        }

    @Test
    void replacedCoreFileIsReported() throws IOException
        {
        writeCoreServiceFiles(tmp);
        writeServiceFile(tmp, STORAGE_IF, FILE_STORAGE); // the filesystem module's file replaced core's
        try (URLClassLoader cl = isolatedLoader(tmp))
            {
            String report = ServiceRegistrationCheck.findMissingRegistrations(cl);
            assertNotNull(report);
            assertTrue(report.startsWith("mConfig did not find all of its components, likely due to faulty JAR packaging"), report);
            assertTrue(report.contains(ServiceRegistrationCheck.DOCUMENTATION), report);
            assertTrue(report.contains("META-INF/services/"+STORAGE_IF+" lacks "), report);
            assertTrue(report.contains("DefaultLayerStorage"), report);
            assertTrue(report.contains(tmp.toUri().toURL()+"META-INF/services/"+STORAGE_IF), "names the surviving file: "+report);
            assertFalse(report.contains(LOGGING_IF), "logging registrations are intact: "+report);
            }
        }

    @Test
    void survivingCoreFileMissingOtherModuleIsReported() throws IOException
        {
        writeCoreServiceFiles(tmp);
        Path cls = tmp.resolve("org/metabit/platform/support/config/impl/source/filesystem/FileConfigStorage.class");
        Files.createDirectories(cls.getParent());
        Files.write(cls, new byte[]{0});
        try (URLClassLoader cl = isolatedLoader(tmp))
            {
            String report = ServiceRegistrationCheck.findMissingRegistrations(cl);
            assertNotNull(report);
            assertTrue(report.startsWith("mConfig did not find all of its components, likely due to faulty JAR packaging"), report);
            assertTrue(report.contains("FileConfigStorage"), report);
            }
        }

    @Test
    void commentsAndBlankLinesAreIgnored() throws IOException
        {
        writeServiceFile(tmp, STORAGE_IF, "# comment", "",
                "org.metabit.platform.support.config.source.core.DefaultLayerStorage  # trailing",
                "org.metabit.platform.support.config.source.core.RuntimeInMemoryLayerSource",
                "org.metabit.platform.support.config.source.core.SessionInMemoryLayerSource");
        writeServiceFile(tmp, LOGGING_IF,
                "org.metabit.platform.support.config.impl.logging.NullLogging",
                "org.metabit.platform.support.config.impl.logging.ConsoleLogging");
        try (URLClassLoader cl = isolatedLoader(tmp))
            {
            assertNull(ServiceRegistrationCheck.findMissingRegistrations(cl));
            }
        }

    @Test
    void checkFailsByDefault() throws IOException
        {
        try (URLClassLoader cl = isolatedLoader(tmp))
            {
            List<String> warnings = new ArrayList<>();
            ConfigException e = assertThrows(ConfigException.class,
                    ()->ServiceRegistrationCheck.check(cl, new ConfigFactorySettings(), capturingLogger(warnings)));
            assertEquals(ConfigException.ConfigExceptionReason.JAVA_SERVICE_CONFIGURATION_ERROR, e.getReason());
            assertTrue(e.getMessage().contains("faulty JAR packaging"), e.getMessage());
            assertTrue(warnings.isEmpty());
            }
        }

    @Test
    void checkWarnsWhenAllowed() throws IOException
        {
        try (URLClassLoader cl = isolatedLoader(tmp))
            {
            ConfigFactorySettings settings = new ConfigFactorySettings();
            settings.setBoolean(ConfigFeature.ALLOW_INCOMPLETE_SERVICE_REGISTRATION, true);
            List<String> warnings = new ArrayList<>();
            assertDoesNotThrow(()->ServiceRegistrationCheck.check(cl, settings, capturingLogger(warnings)));
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).contains("faulty JAR packaging"), warnings.get(0));
            }
        }

    // ---------------------------------------------------------------------------------------------------------

    private static URLClassLoader isolatedLoader(Path root) throws IOException
        {
        return new URLClassLoader(new URL[]{root.toUri().toURL()}, null); // bootstrap parent: sees none of the real service files
        }

    /**
     * the provider names in KNOWN_PROVIDERS are strings; a wrong one is never checked and fails silently.
     * This compares them, both ways, with the service files the reactor's modules ship.
     */
    @Test
    void knownProvidersMatchTheModulesServiceFiles() throws IOException
        {
        Path repoRoot = Paths.get("..").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repoRoot.resolve("mConfigCore/src/main/resources/META-INF/services")),
                "not run inside the mConfig source tree");
        for (Map.Entry<Class<?>, List<String>> entry : ServiceRegistrationCheck.KNOWN_PROVIDERS.entrySet())
            {
            String serviceFile = "src/main/resources/"+ServiceRegistrationCheck.SERVICES_PREFIX+entry.getKey().getName();
            Set<String> shipped = new TreeSet<>();
            try (DirectoryStream<Path> modules = Files.newDirectoryStream(repoRoot))
                {
                for (Path module : modules)
                    {
                    Path file = module.resolve(serviceFile);
                    if (Files.isRegularFile(file))
                        {
                        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8))
                            {
                            String name = line.replaceAll("#.*", "").trim();
                            if (!name.isEmpty())
                                {
                                shipped.add(name);
                                }
                            }
                        }
                    }
                }
            for (Class<?> core : ServiceRegistrationCheck.CORE_PROVIDERS.get(entry.getKey()))
                {
                shipped.remove(core.getName());
                }
            assertEquals(shipped, new TreeSet<>(entry.getValue()), "KNOWN_PROVIDERS for "+entry.getKey().getSimpleName());
            }
        }

    private static void writeCoreServiceFiles(Path root) throws IOException
        {
        for (Map.Entry<Class<?>, List<Class<?>>> e : ServiceRegistrationCheck.CORE_PROVIDERS.entrySet())
            {
            List<String> names = new ArrayList<>();
            e.getValue().forEach(c->names.add(c.getName()));
            writeServiceFile(root, e.getKey().getName(), names.toArray(new String[0]));
            }
        }

    private static void writeServiceFile(Path root, String iface, String... lines) throws IOException
        {
        Path file = root.resolve(ServiceRegistrationCheck.SERVICES_PREFIX+iface);
        Files.createDirectories(file.getParent());
        Files.write(file, Arrays.asList(lines), StandardCharsets.UTF_8);
        }

    private static ConfigLoggingInterface capturingLogger(List<String> warnings)
        {
        return (ConfigLoggingInterface) Proxy.newProxyInstance(ConfigLoggingInterface.class.getClassLoader(),
                new Class<?>[]{ConfigLoggingInterface.class}, (proxy, method, args)->
                    {
                    if (method.getName().equals("warn") && args != null && args.length == 1)
                        {
                        warnings.add((String) args[0]);
                        }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return Boolean.FALSE;
                    if (rt == int.class) return 0;
                    return null;
                    });
        }
}
//___EOF___
