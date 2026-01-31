package org.metabit.platform.support.config.util;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.metabit.platform.support.config.util.impl.OverridingConfiguration;
import org.metabit.platform.support.config.util.impl.RemappedConfiguration;
import org.metabit.platform.support.config.util.impl.PropertiesConfiguration;

/**
 * utility functions for using mConfig.
 */
public class ConfigUtil
{
    /**
     * legacy application support:
     * try to find a Path where files(!) can be placed.
     * If there are multiple matches, it returns the most specific.
     * <p>
     * Consider using configurations and, where needed, BLOB Configurations instead;
     * these can be stored networked etc.
     * This function specifically returns files, and is intended for legacy applications.
     *
     * @param configFactory    to work with.
     * @param scope            the ConfigScope we're looking for.
     * @param requireWriteable if true, read-only Paths are ignored.
     * @return existing, usable Path
     *
     * @throws ConfigCheckedException in case there is no path matching given parameters.
     *                                Caller needs to handle this case, hence checked exception.
     */
    static public Path whereToPutMyFiles(ConfigFactory configFactory, final ConfigScope scope, boolean requireWriteable)
            throws ConfigCheckedException
        {
        return FilePlacementFinder.find(configFactory, scope, requireWriteable);
        }

    /**
     * print config search locations to System.out.
     * Note: This method specifically targets console output for user convenience.
     *
     * @param companyName     company name for the configuration search
     * @param applicationName application name for the configuration search
     * @throws ConfigException on severe errors.
     */
    @SuppressWarnings("squid:S106") // Use of System.out is intentional here for console output.
    static public void printConfigPaths(final String companyName, final String applicationName)
            throws ConfigException
        {
        ConfigPathsPrinter.print(companyName, applicationName);
        }

    /**
     * extract a prefixed subset to a java.util.Properties object.
     * The prefix is removed from the result keys.
     *
     * @param cfg    the configuration to extract the subconfig from
     * @param prefix the string prefix in the flattened key names
     * @return a Properties object containing the subset of config entries whose keys were starting with the prefix before extraction
     *         MLIB-64
     */
    static public java.util.Properties copyPrefixedEntriesToJavaProperties(final Configuration cfg, final String prefix)
        {
        return PrefixEntryExtractor.extract(cfg, prefix);
        }

    /**
     * test a config scheme. allows you to test generated schemes etc.
     * before compiling them in.
     *
     * @param configSchemeInJsonFormat the scheme to be checked
     * @return true if it is OK, false if it has errors.
     */
    public boolean testConfigScheme(final String configSchemeInJsonFormat)
        {
        try
            { return ConfigSchemeFactory.create().testSchemeJSON(configSchemeInJsonFormat); }
        catch (Exception e)
            { return false; }
        }

    /**
     * "flattening" configurations to a Java.util Properties instance.
     *
     * @param cfg configuration to copy to a Properties instance.
     *            *must* have a ConfigScheme, otherwise we don't know which entries to look for.
     * @return the Properties object with all entries converted to String
     *
     * @throws UnsupportedOperationException if the configuration has no ConfigScheme
     */
    @Deprecated
    static public java.util.Properties copySchemeDefinedEntriesToJavaProperties(Configuration cfg)
            throws UnsupportedOperationException
        {
        return SchemeEntryExtractor.extract(cfg);
        }


    /**
     * A pre-defined EnumSet containing all possible configuration scopes defined by the {@link ConfigScope} enum,
     * as a convenience for some calls.
     */
    public static EnumSet<ConfigScope> ALL_SCOPES = EnumSet.allOf(ConfigScope.class);

    /**
     * get me a Config, quick!
     * <p>
     * convenience function if one does not want to use mConfig properly full-featured,
     * but just get their {@link Configuration} settings quick.
     * Some things it skips, other things it assumes. If you find the built-in
     * assumptions not matching yours, please use the regular mConfig calls.
     * <p>
     * All three parameters are mandatory, sorry. We can't guess too well
     * what your application, organization, or desired config are called.
     * <p>
     * (No, nice idea, but trying to deduce from caller package names fails too often. Not a good heuristic.)
     * <p>
     * If you use mConfig fully, however, with config schemes and defaults,
     * then all of this can be detected indeed. But that's different from
     * this call here.
     * <p>
     * <p>
     * FYI: If a testing framework is detected as caller, {@link ConfigFeature.TEST_MODE } is activated for your convenience.
     *
     *
     * @param orgOrCompany organization or company
     * @param application  application name
     * @param configName   name of the configuration to fetch
     * @return a configuration instance
     *
     * @throws ConfigException an unchecked Exceptions if things fail entirely.
     *                         <p>
     *                         Remember you can provide default settings. mConfig uses them automatically.
     *                         See documentation which one goes where.
     */
    public static Configuration quickConfig(final String orgOrCompany, final String application, final String configName)
            throws ConfigException
        {
        return QuickConfigFactory.create(orgOrCompany, application, configName);
        }

    /**
     * Creates an immutable {@link Configuration} view with prefix remapping.
     * <p>
     * Keys starting with {@code newPrefix} (trimmed) are remapped: strip {@code newPrefix}, prepend {@code oldPrefix}
     * (trimmed) for lookup in {@code source}. Other keys delegate directly.
     * Prefixes and keys trimmed aggressively. Uniform logic for all typed getters.
     * <p>
     * Inherits {@code source} scheme for validation. getAllConfigurationKeys* project/transform matching keys.
     * <p>
     * Example: remapped(appConfig, "super_db.", "db.";).getString("db.host") → appConfig.getString("super_db.host")
     *
     * @param source base configuration
     * @param oldPrefix prefix to prepend on mapped lookups
     * @param newPrefix prefix to match and strip on input keys
     * @return new immutable view
     */
    public static Configuration remapped(Configuration source, String oldPrefix, String newPrefix)
        {
        return new RemappedConfiguration(source, oldPrefix.trim(), newPrefix.trim());
        }

    /**
     * Creates an immutable {@link Configuration} view overlaying {@code overrides} on top of {@code parent}.
     * <p>
     * <em>consider using put to the RUNTIME scope instead (not immutable)</em>
     * <p>
     * Overrides take precedence. Values are type-converted lazily on access. If {@code parent} has a
     * {@link ConfigScheme}, overrides are validated against it (throws `IllegalArgumentException`
     * on invalid access).
     * <p>
     * Supports chaining, e.g., {@link #remapped(Configuration, String, String)} for prefixed overrides.
     * <p>
     * Candidate for moving to the Cheese module @CHECK
     *
     *
     * @param parent the base configuration
     * @param overrides the key-value overrides (String keys, Object values auto-converted)
     * @return new immutable Configuration view
     */
    public static Configuration withOverrides(Configuration parent, Map<String, Object> overrides)
    {
        return new OverridingConfiguration(parent, overrides);
    }

    /**
     * Adapts {@link Properties} to immutable {@link Configuration} view.
     * <p>
     * Typed getters parse {@code props.getProperty(key.trim())} lazily; return null on parse error/overflow.
     * No scheme, empty source locations.
     * getAllConfigurationKeysFlattened(scopes): props.stringPropertyNames() (scopes ignored).
     * <p>
     * For in-memory or legacy Properties use; chain with withOverrides/remapped.
     * <p>
     *
     * @param props Properties instance (defensive copy not made, view only)
     * @return new immutable view
     */
    public static Configuration fromProperties(Properties props)
    {
        return new PropertiesConfiguration(props);
    }

    private static class PrefixEntryExtractor
    {
        static Properties extract(final Configuration cfg, final String prefix)
            {
            Properties akku = new Properties();
            Iterator<String> entryKeyTreeIterator = cfg.getEntryKeyTreeIterator();
            while (entryKeyTreeIterator.hasNext())
                {
                String entryName = entryKeyTreeIterator.next();
                if (entryName.startsWith(prefix))
                { akku.put(entryName.substring(prefix.length()), cfg.getString(entryName)); }
                }
            return akku;
            }
    }

    private static class SchemeEntryExtractor
    {
        static Properties extract(Configuration cfg)
            throws UnsupportedOperationException
            {
            ConfigScheme scheme = cfg.getConfigScheme();
            if (scheme == null)
                throw new UnsupportedOperationException("iteration possible only for configurations with associated schemes");

            Properties akku = new Properties();
            for (String entryName : scheme.getEntryKeys())
                { akku.put(entryName, cfg.getString(entryName)); }
            return akku;
            }
    }

    private static class ConfigPathsPrinter
    {
        static void print(final String companyName, final String applicationName) throws ConfigException
            {
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(companyName, applicationName);
            try (final ConfigFactory configFactory = builder.build())
                {
                for (ConfigLocation cse : configFactory.getSearchList())
                    {
                    System.out.println(cse.toString());
                    }
                }
            }
    }

    private static class FilePlacementFinder
    {
        static Path find(ConfigFactory configFactory, final ConfigScope scope, boolean requireWriteable)
            throws ConfigCheckedException
            {
            List<Path> potentialMatches = new ArrayList<>();
            for (ConfigLocation cse : configFactory.getSearchList())
                {
                // from the search list, filter scope
                if (cse.getScope() != scope) continue;
                // geht the URI/URL
                URI uri = cse.getURI("", null);
                // filter for locations of type "file"; skip anything else
                if (!uri.getScheme().equals("file")) continue;
                // then check whether the potential match is a writeable Path.
                Path path = Paths.get(uri);
                potentialMatches.add(path);
                }
            if (potentialMatches.isEmpty())
                throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NO_MATCHING_DATA);
            for (Path path : potentialMatches)
                {
                //  option: do we require writeable, or is readable good enough?
                if (requireWriteable)
                    { if (path.toFile().canWrite()) continue; }
                // existing ones preferred in first loop; in second, we go with creation
                if (!path.toFile().exists()) continue; // no potentials, only existing -- option?
                if (path.toFile().isFile()) continue; // skip files, directories only.
                return path; // that's a match, then.
                }
            // second loop: we try to create non-existing ones.
            for (Path path : potentialMatches)
                {
                // skip the existing ones this time, they weren't any good in the first loop
                if (path.toFile().exists()) continue;
                // try to create, including parent directories.
                try
                    {
                    Path result = Files.createDirectories(path);
                    return result.toAbsolutePath(); // if successful
                    }
                catch (IOException e)
                    { throw new ConfigCheckedException(e); }
                }
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NO_MATCHING_DATA);
            }
    }

    private static class QuickConfigFactory
    {
        static Configuration create(final String orgOrCompany, final String application, final String configName) throws ConfigException
            {
            ConfigFactoryBuilder configFactoryBuilder = ConfigFactoryBuilder.create(orgOrCompany, application);
            if (TestDetector.isRunByTestingLibrary())
                configFactoryBuilder.setFeature(ConfigFeature.TEST_MODE, true);
            // more automatic stuff could go here.
            ConfigFactory configFactory = configFactoryBuilder.build();
            return configFactory.getConfig(configName);
            }
    }

}
//___EOF___