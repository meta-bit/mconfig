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
                ProposeSchemeCommand.class,
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

    @Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:CONFIGNAME[:KEY]]")
    String shortened;

    public static void main(String[] args)
        {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);

        // Hide experimental commands by default
        CommandLine proposeSub = cmd.getSubcommands().get("propose-scheme");
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
                if ("propose-scheme".equals(subName) || "validate".equals(subName))
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
        public String            configName;
        public String            key;
        public Main.OutputFormat format;
        public boolean           verbose;
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
            this.language = (opts.language != null && !opts.language.equals("en")) ? opts.language : main.commonOptions.language;

            // parse shortened notation with : separator
            if (main.shortened != null && !main.shortened.isEmpty())
                {
                String[] parts = main.shortened.split(":");
                switch (parts.length)
                    {
                    case 4:
                        this.key = parts[3];
                        // fallthrough
                    case 3:
                        this.configName = parts[2];
                        // fallthrough
                    case 2:
                        this.application = parts[1];
                        // fallthrough
                    case 1:
                        if (parts.length == 1)
                            {
                            if (this.company != null && this.application != null && this.configName != null)
                                { this.key = parts[0]; }
                            else if (this.company != null && this.application != null)
                                { this.configName = parts[0]; }
                            else if (this.company != null)
                                { this.application = parts[0]; }
                            else
                                { this.application = parts[0]; }
                            }
                        else
                            {
                            this.company = parts[0];
                            }
                        break;
                    }
                }
            // fallback to ENV direct setting
            if (this.company == null) this.company = System.getenv("MCONFIG_COMPANY");
            if (this.application == null) this.application = System.getenv("MCONFIG_APP");
            if (this.configName == null) this.configName = System.getenv("MCONFIG_CONFIG");
            return;
            }

        public void validate(boolean requireConfig, boolean requireKey)
            {
            if (company == null) company = "";
            if (application == null || application.isEmpty()) throw new IllegalArgumentException("Application name missing. Use -a, MCONFIG_APP env var, or shortened syntax.");
            if (requireConfig && (configName == null || configName.isEmpty())) throw new IllegalArgumentException("Config name missing. Use --config, MCONFIG_CONFIG env var, or shortened syntax.");
            if (requireKey && (key == null || key.isEmpty())) throw new IllegalArgumentException("Key missing. Use --key (-K) or shortened syntax.");
            return; // validated OK
            }
    }
}
//___EOF___
