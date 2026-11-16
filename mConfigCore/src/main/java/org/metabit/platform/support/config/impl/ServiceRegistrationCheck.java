package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.source.core.DefaultLayerStorage;
import org.metabit.platform.support.config.source.core.RuntimeInMemoryLayerSource;
import org.metabit.platform.support.config.source.core.SessionInMemoryLayerSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * detects faulty single-JAR packaging by core's own service registrations (MLIB-156).
 * <p>
 * Packaging that unpacks dependency JARs into one without merging their {@code META-INF/services} files keeps
 * only one file per service interface. Core ships such files for interfaces other modules also provide;
 * if core's providers are missing from them, another module's file has replaced core's, and other modules'
 * registrations are probably lost the same way. For the reverse case, core's file surviving, the check knows
 * the first-party providers by name ({@link #KNOWN_PROVIDERS}): a provider whose class is on the class path but
 * which no service file registers was lost. Third-party providers stay undetectable; the documentation covers it.
 * <p>
 * Internal implementation class.
 */
public final class ServiceRegistrationCheck
{
    static final String SERVICES_PREFIX = "META-INF/services/";
    static final String DOCUMENTATION   = "https://github.com/meta-bit/mconfig/blob/master/documentation/src/site/markdown/13_getting_started.md";

    /** core's own providers, by the service interface whose file other modules also ship. */
    static final Map<Class<?>, List<Class<?>>> CORE_PROVIDERS;

    /** known first-party optional providers, checked when their .class resource is present on the classpath. */
    static final Map<Class<?>, List<String>> KNOWN_PROVIDERS;

    static
        {
        Map<Class<?>, List<Class<?>>> m = new LinkedHashMap<>();
        m.put(ConfigStorageInterface.class, Arrays.<Class<?>>asList(DefaultLayerStorage.class, RuntimeInMemoryLayerSource.class, SessionInMemoryLayerSource.class));
        m.put(ConfigLoggingInterface.class, Arrays.<Class<?>>asList(NullLogging.class, ConsoleLogging.class));
        CORE_PROVIDERS = Collections.unmodifiableMap(m);

        Map<Class<?>, List<String>> k = new LinkedHashMap<>();
        k.put(ConfigStorageInterface.class, Arrays.asList(
                "org.metabit.platform.support.config.source.hardcoded.JARConfigSource",
                "org.metabit.platform.support.config.impl.source.filesystem.FileConfigStorage",
                "org.metabit.platform.support.config.source.envvar.EnvVarConfigStorage",
                "org.metabit.platform.support.config.source.vault.VaultConfigStorage",
                "org.metabit.platform.support.config.source.aws.AwsSecretsManagerConfigStorage",
                "org.metabit.platform.support.config.impl.source.kubernetes.KubernetesConfigStorage",
                "org.metabit.platform.support.config.impl.source.kubernetes.fabric8.KubernetesFabric8ConfigStorage",
                "org.metabit.platform.support.config.source.zookeeper.ZooKeeperConfigStorage",
                "org.metabit.platform.support.config.source.windowsregistry.WindowsRegistryConfigSource",
                "org.metabit.platform.support.config.source.windowsregistry.jni.WindowsRegistryJNISource"
        ));
        k.put(ConfigLoggingInterface.class, Arrays.asList(
                "org.metabit.platform.support.config.logging.Slf4j2Logger",
                "org.metabit.platform.support.config.util.loggers.JavaSystemLogger",
                "org.metabit.platform.support.config.util.loggers.StdErrLogger"
        ));
        KNOWN_PROVIDERS = Collections.unmodifiableMap(k);
        }

    private ServiceRegistrationCheck() { }

    /**
     * runs the check.
     *
     * @param classLoader the class loader the factory uses for its ServiceLoader lookups
     * @param settings    the factory settings
     * @param logger      the logger for the warning
     * @throws ConfigException (JAVA_SERVICE_CONFIGURATION_ERROR) unless {@link ConfigFeature#ALLOW_INCOMPLETE_SERVICE_REGISTRATION} is set
     */
    public static void check(final ClassLoader classLoader, final ConfigFactorySettings settings, final ConfigLoggingInterface logger)
            throws ConfigException
        {
        String report = findMissingRegistrations(classLoader);
        if (report == null)
            {
            return;
            }
        if (settings.getBoolean(ConfigFeature.ALLOW_INCOMPLETE_SERVICE_REGISTRATION))
            {
            logger.warn(report);
            return;
            }
        throw new ConfigException(ConfigException.ConfigExceptionReason.JAVA_SERVICE_CONFIGURATION_ERROR, report);
        }

    /**
     * @param classLoader the class loader to check
     * @return a report naming the missing registrations and the service files present, or null if there are none
     */
    static String findMissingRegistrations(final ClassLoader classLoader)
        {
        if (classLoader == null)
            {
            return null;
            }
        StringBuilder details = new StringBuilder();
        for (Map.Entry<Class<?>, List<Class<?>>> entry : CORE_PROVIDERS.entrySet())
            {
            String serviceFile = SERVICES_PREFIX+entry.getKey().getName();
            Set<String> registered = new HashSet<>();
            List<URL> serviceFileLocations = new ArrayList<>();
            readServiceFiles(classLoader, serviceFile, registered, serviceFileLocations);
            StringBuilder missing = new StringBuilder();
            for (Class<?> provider : entry.getValue())
                {
                if (!registered.contains(provider.getName()))
                    {
                    missing.append(missing.length() == 0 ? "" : ", ").append(provider.getName());
                    }
                }
            List<String> optional = KNOWN_PROVIDERS.get(entry.getKey());
            if (optional != null)
                {
                for (String providerName : optional)
                    {
                    String resource = providerName.replace('.', '/') + ".class";
                    if (classLoader.getResource(resource) != null && !registered.contains(providerName))
                        {
                        missing.append(missing.length() == 0 ? "" : ", ").append(providerName);
                        }
                    }
                }
            if (missing.length() > 0)
                {
                details.append("\n  ").append(serviceFile).append(" lacks ").append(missing)
                        .append("; present: ").append(LogSafe.text(serviceFileLocations));
                }
            }
        if (details.length() == 0)
            {
            return null;
            }
        return "mConfig did not find all of its components, likely due to faulty JAR packaging,"
                +" which also affects other modular libraries. See \"Packaging as a single JAR\" in "+DOCUMENTATION+details;
        }

    /** collects the class names listed in all service files of that name, as ServiceLoader reads them on the classpath. */
    private static void readServiceFiles(final ClassLoader classLoader, final String serviceFile, final Set<String> registered, final List<URL> locations)
        {
        Enumeration<URL> urls;
        try
            {
            urls = classLoader.getResources(serviceFile);
            }
        catch (IOException e)
            {
            return;
            }
        while (urls.hasMoreElements())
            {
            URL url = urls.nextElement();
            locations.add(url);
            try (InputStream in = url.openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                {
                String line;
                while ((line = reader.readLine()) != null)
                    {
                    int comment = line.indexOf('#');
                    if (comment >= 0)
                        {
                        line = line.substring(0, comment);
                        }
                    line = line.trim();
                    if (!line.isEmpty())
                        {
                        registered.add(line);
                        }
                    }
                }
            catch (IOException e)
                {
                // unreadable file: its entries count as not registered
                }
            }
        }
}
//___EOF___
