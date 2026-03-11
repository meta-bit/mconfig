package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.Configuration;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Parameters;

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
                CompletionCommand.class
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

        // Hide experimental commands by default
        CommandLine proposeSub = cmd.getSubcommands().get("propose-schema");
        if (proposeSub != null) proposeSub.getCommandSpec().usageMessage().hidden(true);
        CommandLine validateSub = cmd.getSubcommands().get("validate");
        if (validateSub != null) validateSub.getCommandSpec().usageMessage().hidden(true);

        // Parse just enough to see if experimental is set
        CommandLine.ParseResult parseResult = cmd.parseArgs(args);
        if (main.commonOptions.experimental)
            {
            if (proposeSub != null) proposeSub.getCommandSpec().usageMessage().hidden(false);
            if (validateSub != null) validateSub.getCommandSpec().usageMessage().hidden(false);
            }
        else
            {
            // If not experimental, check if user tried to call hidden commands
            if (parseResult.hasSubcommand())
                {
                String subName = parseResult.subcommand().commandSpec().name();
                if ("propose-schema".equals(subName) || "validate".equals(subName))
                    {
                    System.err.println("Command '" + subName + "' is experimental and requires --experimental (-X) flag.");
                    System.exit(1);
                    }
                }
            }

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
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
                }

            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.COMMENTS_READING, true);
            builder.setFeature(org.metabit.platform.support.config.ConfigFeature.COMMENTS_WRITING, true);

            return builder;
            }
    }
}
//___EOF___
