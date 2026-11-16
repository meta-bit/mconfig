package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.Configuration;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Parameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * mConfig command line tool. entry point.
 */
@Command(name = "mconfig", mixinStandardHelpOptions = true,
        versionProvider = Main.VersionProvider.class,
        description = "mConfig command line tool.",
        subcommands = {
                ListCommand.class,
                ShowCommand.class,
                GetCommand.class,
                SearchPathsCommand.class,
                ProposeSchemaCommand.class,
                ValidateCommand.class,
                SetCommand.class,
                MonitorCommand.class,
                CompletionCommand.class,
                KubernetesRbacCommand.class
        })
public class Main implements Callable<Integer>
{
    @CommandLine.Mixin
    public CommonOptions commonOptions = new CommonOptions();

    public enum OutputFormat
    {HUMAN, JSON, CSV, YAML, TOML}

    @Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:SUBDIR...]:CONFIGNAME[@KEY]")
    public String shortened;

    public static String[] normalizeArgs(CommandLine cmd, String[] args)
        {
        if (args.length > 1)
            {
            String first = args[0];
            String second = args[1];
            if (!cmd.getSubcommands().containsKey(first) && !first.startsWith("-"))
                {
                // if second argument IS a subcommand, swap them
                if (cmd.getSubcommands().containsKey(second))
                    {
                    String[] swapped = new String[args.length];
                    swapped[0] = second;
                    swapped[1] = first;
                    System.arraycopy(args, 2, swapped, 2, args.length - 2);
                    return swapped;
                    }
                }
            }
        return args;
        }

    public static void main(String[] args)
        {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);

        args = normalizeArgs(cmd, args);

        if (!applyExperimentalGate(cmd, main, args))
            {
            System.exit(1);
            }

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
        }

    /**
     * commands that are not complete yet: hidden from the help, and refused without --experimental.
     * Remove a name in the same change that completes the command (MLIB-151).
     */
    static final Set<String> EXPERIMENTAL_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "validate",         // MLIB-151
            "propose-schema",   // rewrite in 0.9.2, MLIB-137
            "kubernetes-rbac"   // least-privilege generator in 0.9.3, MLIB-150
    )));

    /**
     * hide the experimental commands unless --experimental is given, and refuse calling one without it.
     *
     * @param cmd  the command line for main
     * @param main the Main instance bound to cmd
     * @param args the (normalized) arguments
     * @return false if an experimental command was called without --experimental; an error was printed.
     */
    static boolean applyExperimentalGate(CommandLine cmd, Main main, String[] args)
        {
        // parse just enough to see if experimental is set
        CommandLine.ParseResult parseResult = cmd.parseArgs(args);
        boolean experimental = main.commonOptions.experimental;
        if (parseResult.hasSubcommand())
            {
            // -X after the subcommand: an option where the subcommand has CommonOptions, otherwise unmatched
            CommandLine.ParseResult sub = parseResult.subcommand();
            if ((sub.commandSpec().findOption("--experimental") != null && sub.hasMatchedOption("--experimental"))
                    || sub.unmatched().contains("-X") || sub.unmatched().contains("--experimental"))
                {
                experimental = true;
                }
            }
        for (String name : EXPERIMENTAL_COMMANDS)
            {
            CommandLine sub = cmd.getSubcommands().get(name);
            if (sub != null) sub.getCommandSpec().usageMessage().hidden(!experimental);
            }
        if (!experimental && parseResult.hasSubcommand())
            {
            String subName = parseResult.subcommand().commandSpec().name();
            if (EXPERIMENTAL_COMMANDS.contains(subName))
                {
                System.err.println("Command '" + subName + "' is experimental and requires --experimental (-X) flag.");
                return false;
                }
            }
        return true;
        }

    @Override
    public Integer call()
        {
        CommandLine.usage(this, System.out);
        return 0;
        }

    public static class VersionProvider implements IVersionProvider
    {
        @Override
        public String[] getVersion()
            {
            // Reads version from configuration; returns fallback on failure
            try (ConfigFactory factory = ConfigFactoryBuilder
                    .create("metabit", "mconfig")
                    .setFeature(ConfigFeature.EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND, true)
                    .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, true)
                    .build())
                {
                Configuration config = factory.getConfig("version");
                String version = config.getString("version");
                return new String[]{"mconfig " + version};
                }
            catch (Exception e)
                { return new String[]{"mconfig version unknown"}; }
            }
    }

    public static class ConfigContext
    {
        public String            company;
        public String            application;
        public String            subPath;
        public String            configName;
        public String            key;
        public Main.OutputFormat format;
        public boolean           verbose;
        public boolean           debug;
        public boolean           showEvents;
        public String            language;

        public ConfigContext(Main main, CommonOptions opts)
            {
            // Subcommand options take precedence over Main options if set
            this.company = opts.company != null ? opts.company : main.commonOptions.company;
            this.application = opts.application != null ? opts.application : main.commonOptions.application;
            this.configName = opts.config != null ? opts.config : main.commonOptions.config;
            this.key = opts.key != null ? opts.key : main.commonOptions.key;
            this.format = opts.format != Main.OutputFormat.HUMAN ? opts.format : main.commonOptions.format;
            this.verbose = opts.verbose || main.commonOptions.verbose;
            this.debug = opts.debug || main.commonOptions.debug;
            this.showEvents = opts.showEvents || main.commonOptions.showEvents;
            this.language = (opts.language != null && !opts.language.equals("en")) ? opts.language : main.commonOptions.language;

            // parse shortened notation with : and @ separators
            if (main.shortened != null && !main.shortened.isEmpty())
                {
                String vector = main.shortened;
                // 1. handle @key
                int atIdx = vector.lastIndexOf('@');
                if (atIdx >= 0)
                    {
                    this.key = vector.substring(atIdx + 1);
                    vector = vector.substring(0, atIdx);
                    }

                // 2. handle company:app:subdir:config
                if (!vector.isEmpty())
                    {
                    boolean hasLeadingColon = vector.startsWith(":");
                    String[] parts = (hasLeadingColon ? vector.substring(1) : vector).split(":");

                    if (hasLeadingColon)
                        {
                        // Leading colon means company is empty (no company)
                        this.company = "";
                        if (parts.length >= 1) this.application = parts[0];
                        if (parts.length >= 2) this.configName = parts[parts.length - 1];
                        if (parts.length > 2)
                            {
                            this.subPath = String.join("/", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
                            }
                        }
                    else
                        {
                        // No leading colon
                        if (parts.length == 1)
                            {
                            // Single segment is application
                            this.application = parts[0];
                            }
                        else if (parts.length == 2)
                            {
                            // Two segments: company:app
                            this.company = parts[0];
                            this.application = parts[1];
                            }
                        else if (parts.length >= 3)
                            {
                            this.company = parts[0];
                            this.application = parts[1];
                            this.configName = parts[parts.length - 1];
                            if (parts.length > 3)
                                {
                                this.subPath = String.join("/", java.util.Arrays.copyOfRange(parts, 2, parts.length - 1));
                                }
                            }
                        }
                    }
                }
            // fallback to ENV direct setting
            if (this.company == null) this.company = System.getenv("MCONFIG_COMPANY");
            if (this.application == null) this.application = System.getenv("MCONFIG_APP");
            if (this.configName == null) this.configName = System.getenv("MCONFIG_CONFIG");
            
            // normalization
            if (this.company == null || this.company.equals("null")) this.company = "";
            if (this.application != null && this.application.equals("null")) this.application = "";
            if (this.configName != null && this.configName.equals("null")) this.configName = "";

            return;
            }

        public void validate(boolean requireConfig, boolean requireKey)
            {
            if (application == null || application.isEmpty()) throw new IllegalArgumentException("Application name missing. Use -a, MCONFIG_APP env var, or shortened syntax.");
            if (requireConfig && (configName == null || configName.isEmpty())) throw new IllegalArgumentException("Config name missing. Use --config, MCONFIG_CONFIG env var, or shortened syntax.");
            if (requireKey && (key == null || key.isEmpty())) throw new IllegalArgumentException("Key missing. Use --key (-K) or shortened syntax.");
            return; // validated OK
            }

        public ConfigFactoryBuilder createBuilder()
            {
            ConfigFactoryBuilder builder = (subPath != null && !subPath.isEmpty())
                    ? ConfigFactoryBuilder.create(company, application, subPath)
                    : ConfigFactoryBuilder.create(company, application);
            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.COMMENTS_READING, true);
            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.COMMENTS_WRITING, true);

            // Handle additional directories from environment (CLI-only mapping)
            String addUserDirs = System.getenv("MCONFIG_ADDITIONAL_USER_DIRECTORIES");
            if (addUserDirs != null && !addUserDirs.isEmpty())
                {
                builder.setFeature(org.metabit.platform.support.config.ConfigFeature.ADDITIONAL_USER_DIRECTORIES, 
                        java.util.Arrays.asList(addUserDirs.split(",")));
                }

            String addRuntimeDirs = System.getenv("MCONFIG_ADDITIONAL_RUNTIME_DIRECTORIES");
            if (addRuntimeDirs != null && !addRuntimeDirs.isEmpty())
                {
                builder.setFeature(org.metabit.platform.support.config.ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES, 
                        java.util.Arrays.asList(addRuntimeDirs.split(",")));
                }

            // Enable test mode only when explicitly requested via system property (used by integration tests)
            String testDirs = System.getProperty("TESTMODE_DIRECTORIES");
            if (testDirs != null && !testDirs.isEmpty())
                {
                org.metabit.platform.support.config.ConfigFactoryBuilder.permitTestMode();
                builder.setTestMode(true);
                java.util.List<String> dirs = java.util.Arrays.asList(testDirs.split(","));
                builder.setFeature(org.metabit.platform.support.config.ConfigFeature.TESTMODE_DIRECTORIES, dirs);
                }

            // Apply debug logging if requested via CLI flag
            if (this.debug)
                {
                builder.setFeature(org.metabit.platform.support.config.ConfigFeature.LOGLEVEL_NUMBER, 5);
                // only the internal logger reads LOGLEVEL_NUMBER; slf4j filters by its own backend's level
                builder.setFeature(org.metabit.platform.support.config.ConfigFeature.LOGGING_REDIRECT_TARGET, "stderr");
                }

            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.COMMENTS_READING, true);
            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.COMMENTS_WRITING, true);

            return builder;
            }
    }
}
//___EOF___
