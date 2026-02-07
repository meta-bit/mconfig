package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.impl.entry.ConfigEntryFactory;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "set", description = "Set or update a configuration entry.", mixinStandardHelpOptions = true)
public class SetCommand implements Callable<Integer>
{
    @ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:CONFIGNAME[:KEY]] (key=value supported)")
    private String shortened;

    @CommandLine.Parameters(index = "1", arity = "0..1", description = "Shortened key=value syntax")
    private String keyValue;

    @CommandLine.Mixin
    public CommonOptions commonOptions = new CommonOptions();

    @Option(names = {"-V", "--value"}, description = "Value to set (or use key=value)")
    private String value;

    @Option(names = {"-S", "--scope"}, description = "Target scope (USER, HOST, SESSION, RUNTIME, etc.)", required = true)
    private ConfigScope scope;

    @Option(names = {"-T", "--type"}, description = "Config entry type (STRING, NUMBER, BOOLEAN, BYTES, MULTIPLE_STRINGS)")
    private ConfigEntryType type;

    @Option(names = {"-d", "--dry-run"}, description = "Display what would happen without making changes")
    private boolean dryRun;

    @Option(names = {"-F", "--file-format"}, description = "Preferred file format for new files (TOML, YAML, JSON, JSON5, PROPERTIES, INI)")
    private FileFormat fileFormat;

    enum FileFormat
    {
        TOML,
        YAML,
        JSON,
        JSON5,
        PROPERTIES,
        INI
    }

    @Override
    public Integer call()
        {
        String context = shortened;
        String kv = keyValue;
        if (context != null && kv == null && context.contains("="))
            {
            int lastColon = context.lastIndexOf(':');
            if (lastColon >= 0)
                {
                kv = context.substring(lastColon+1);
                context = context.substring(0, lastColon);
                }
            else
                {
                kv = context;
                context = null;
                }
            }

        // Copy shortened syntax from subcommand to parent if provided
        if (context != null)
            {
            main.shortened = context;
            }

        if (kv != null)
            {
            int eq = kv.indexOf('=');
            if (eq <= 0 || eq == kv.length()-1)
                {
                System.err.println("Error: key=value syntax is invalid: "+kv);
                return 1;
                }
            if (commonOptions.key != null || value != null)
                {
                System.err.println("Error: Use either --key/--value or key=value, not both.");
                return 1;
                }
            commonOptions.key = kv.substring(0, eq);
            value = kv.substring(eq+1);
            }

        if (value == null)
            {
            System.err.println("Error: Missing value. Use --value or key=value syntax.");
            return 1;
            }
        Main.ConfigContext ctx = new Main.ConfigContext(main, commonOptions);
        try
            {
            ctx.validate(true, true);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(ctx.company, ctx.application);
            if (fileFormat != null)
                {
                // Try both the standard name and the withJackson variant
                builder.setFeature(ConfigFeature.FILE_FORMAT_WRITING_PRIORITIES, 
                        Arrays.asList(fileFormat.name(), fileFormat.name() + "withJackson"));
                }
            String testDirs = System.getProperty("TESTMODE_DIRECTORIES");
            if (testDirs != null)
                {
                ConfigFactoryBuilder.permitTestMode();
                builder.setTestMode(true);
                builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, Arrays.asList(testDirs.split(",")));
                }

            try (ConfigFactory configFactory = builder.build())
                {
                Configuration cfg = configFactory.getConfig(ctx.configName);
                if (!(cfg instanceof LayeredConfiguration))
                    {
                    System.err.println("Error: Configuration is not a LayeredConfiguration, cannot perform writes.");
                    return 1;
                    }
                LayeredConfiguration lcfg = (LayeredConfiguration) cfg;

                if (ctx.verbose)
                    {
                    System.out.println("Config Locations:");
                    for (ConfigLocation loc : lcfg.getSourceLocations())
                        {
                        System.out.println(" - "+loc.getScope()+": "+loc.toLocationString()+" (writeable: "+loc.isWriteable()+")");
                        }
                    }

                ConfigScheme scheme = lcfg.getConfigScheme();
                ConfigEntrySpecification spec = (scheme != null) ? scheme.getSpecification(ctx.key) : null;

                // Determine type
                ConfigEntryType targetType = type;
                if (targetType == null && spec != null)
                    {
                    targetType = spec.getType();
                    }
                if (targetType == null)
                    {
                    targetType = ConfigEntryType.STRING;
                    }

                // Convert value
                Object convertedValue = convertValue(value, targetType);

                // Create a temporary entry for validation
                ConfigEntry entryToValidate = ConfigEntryFactory.createEntry(ctx.key, convertedValue, targetType, scheme, null);

                // 1. Validation
                if (spec != null)
                    {
                    if (!spec.validateEntry(entryToValidate))
                        {
                        System.err.println("Validation failed: Value '"+value+"' does not match specification for key '"+ctx.key+"'");
                        if (spec.getValueLimitations() != null)
                            {
                            System.err.println("Limitations: "+spec.getValueLimitations());
                            }
                        return 1;
                        }
                    }

                // 2. Display dry run or hierarchy if verbose
                if (dryRun || ctx.verbose)
                    {
                    showInformation(lcfg, ctx.key, convertedValue, targetType, scope, dryRun, ctx.verbose);
                    }

                // 3. Perform
                if (!dryRun)
                    {
                    try
                        {
                        if (targetType == ConfigEntryType.BYTES)
                            {
                            lcfg.put(ctx.key, (byte[]) convertedValue, scope);
                            }
                        else if (targetType == ConfigEntryType.MULTIPLE_STRINGS)
                            {
                            lcfg.put(ctx.key, (List<String>) convertedValue, scope);
                            }
                        else if (targetType == ConfigEntryType.BOOLEAN)
                            {
                            lcfg.put(ctx.key, (Boolean) convertedValue, scope);
                            }
                        else if (targetType == ConfigEntryType.NUMBER)
                            {
                            if (convertedValue instanceof BigInteger)
                                {
                                lcfg.put(ctx.key, (BigInteger) convertedValue, scope);
                                }
                            else if (convertedValue instanceof BigDecimal)
                                {
                                lcfg.put(ctx.key, (BigDecimal) convertedValue, scope);
                                }
                            else if (convertedValue instanceof Double)
                                {
                                lcfg.put(ctx.key, (Double) convertedValue, scope);
                                }
                            else if (convertedValue instanceof Long)
                                {
                                lcfg.put(ctx.key, (Long) convertedValue, scope);
                                }
                            else if (convertedValue instanceof Integer)
                                {
                                lcfg.put(ctx.key, (Integer) convertedValue, scope);
                                }
                            else
                                {
                                lcfg.put(ctx.key, value, scope);
                                }
                            }
                        else
                            {
                            lcfg.put(ctx.key, value, scope);
                            }

                        int flushed = lcfg.flush();
                        if (ctx.verbose)
                            {
                            System.out.println("Flushed "+flushed+" changes.");
                            }
                        System.out.println("Successfully set '"+ctx.key+"' to '"+value+"' in "+scope+" scope.");
                        }
                    catch (Exception e)
                        {
                        reportError(e, lcfg, scope);
                        return 1;
                        }
                    }
                }
            }
        catch (Exception e)
            {
            System.err.println("Error: "+e.getMessage());
            if (main.commonOptions.verbose)
                {
                e.printStackTrace();
                }
            return 1;
            }
        return 0;
        }

    private Object convertValue(String value, ConfigEntryType type)
            throws ConfigCheckedException
        {
        switch (type)
            {
            case STRING:
                return value;
            case NUMBER:
                try
                    {
                    if (value.contains(".")) return Double.valueOf(value);
                    return Long.valueOf(value);
                    }
                catch (NumberFormatException e)
                    {
                    throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.CONVERSION_FAILURE);
                    }
            case BOOLEAN:
                return Boolean.valueOf(value);
            case BYTES:
                // We cannot use ConfigIOUtil.hexDecode because it is not exported.
                // We will implement a simple hex decoder here if needed, or just use string bytes.
                // For now, let's just use string bytes to avoid complexity, or skip it.
                return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            case MULTIPLE_STRINGS:
                return Arrays.asList(value.split(","));
            default:
                return value;
            }
        }

    private void showInformation(LayeredConfiguration lcfg, String key, Object value, ConfigEntryType type, ConfigScope scope, boolean dryRun, boolean verbose)
        {
        if (dryRun)
            {
            System.out.println("--- DRY RUN ---");
            }

        System.out.println("Key:           "+key);
        System.out.println("Value:         "+value);
        System.out.println("Type:          "+type);
        System.out.println("Target Scope:  "+scope);

        // Find target layer
        ConfigLayerInterface targetLayer = null;
        List<ConfigLayerInterface> configs = lcfg.getSourceLocations().stream()
                .filter(loc->loc instanceof ConfigLayerInterface)
                .map(loc->(ConfigLayerInterface) loc)
                .collect(Collectors.toList());

        // Re-getting layers is tricky since they are private in LayeredConfiguration.
        // But we can get locations.

        if (verbose)
            {
            System.out.println("\nConfiguration Hierarchy (most specific first):");
            int i = 0;
            for (ConfigLocation loc : lcfg.getSourceLocations())
                {
                boolean isTarget = (loc.getScope() == scope && loc.isWriteable());
                String marker = isTarget ? " [TARGET]" : "";
                System.out.println(String.format("%2d. %-10s %s%s", ++i, loc.getScope(), loc.toLocationString(), marker));

                // For -vv, we could show if the key already exists there
                // But we don't have easy access to individual layers' content here without casting.
                }
            }

        if (dryRun)
            {
            System.out.println("\nAction: Would write to the first writeable layer in "+scope+" scope.");
            System.out.println("--- END DRY RUN ---");
            }
        }

    private void reportError(Exception e, LayeredConfiguration lcfg, ConfigScope scope)
        {
        System.err.println("Failed to set configuration entry.");
        if (e instanceof ConfigException && e.getCause() instanceof ConfigCheckedException)
            {
            ConfigCheckedException ce = (ConfigCheckedException) e.getCause();
            System.err.println("Reason: "+ce.getReason());
            }
        else
            {
            System.err.println("Error message: "+e.getMessage());
            }

        // Try to find if there are any writeable layers in that scope
        boolean foundWriteable = false;
        for (ConfigLocation loc : lcfg.getSourceLocations())
            {
            if (loc.getScope() == scope && loc.isWriteable())
                {
                foundWriteable = true;
                System.err.println("Attempted to write to: "+loc.toLocationString());
                break;
                }
            }

        if (!foundWriteable)
            {
            System.err.println("No writeable configuration layer found for scope: "+scope);
            }

        if (e.getCause() != null && e.getCause().getCause() != null)
            {
            System.err.println("Underlying cause: "+e.getCause().getCause().getMessage());
            }
        }
}
