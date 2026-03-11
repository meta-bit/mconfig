package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "show", description = "Print effective configuration.", mixinStandardHelpOptions = true)
public class ShowCommand implements Callable<Integer>
{
    @ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:SUBDIR...]:CONFIGNAME")
    private String shortened;

    @CommandLine.Mixin
    public CommonOptions commonOptions = new CommonOptions();

    @Override
    public Integer call()
        {
        if (shortened != null)
            {
            main.shortened = shortened;
            }
        Main.ConfigContext ctx = new Main.ConfigContext(main, commonOptions);
        try
            {
            ctx.validate(true, false);
            try (ConfigFactory configFactory = ctx.createBuilder().build())
                {
                Configuration cfg = configFactory.getConfig(ctx.configName);
                if (ctx.format == Main.OutputFormat.HUMAN)
                    {
                    if (cfg.getConfigSchema().isNullSchema())
                        {
                        System.out.println("WARNING: No config scheme defined for '"+ctx.configName+"'. Effective configuration may be incomplete or invalid.");
                        System.out.println("Consider providing a config scheme to enable validation and documentation.");
                        System.out.println();
                        }
                    System.out.println("Effective configuration for "+ctx.configName+":");
                    Map<String, Object> data = OutputFormatter.newLinkedMap();
                    Iterator<String> it = cfg.getEntryKeyTreeIterator();
                    while (it.hasNext())
                        {
                        String key = it.next();
                        ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                        OutputFormatter.putNestedValue(data, key, OutputFormatter.entryValue(entry));
                        }
                    OutputFormatter.printHierarchical(data, 0, ctx.verbose, cfg, null);
                    }
                else if (ctx.format == Main.OutputFormat.CSV)
                    {
                    System.out.println("Key,Value,Scope,URI");
                    Iterator<String> it = cfg.getEntryKeyTreeIterator();
                    while (it.hasNext())
                        {
                        String key = it.next();
                        ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                        System.out.printf("%s,%s,%s,%s%n", key, entry.getValueAsString(), entry.getScope(), entry.getLocation());
                        }
                    }
                else if (ctx.format == Main.OutputFormat.JSON || ctx.format == Main.OutputFormat.YAML || ctx.format == Main.OutputFormat.TOML)
                    {
                    Map<String, Object> data = OutputFormatter.newLinkedMap();
                    Iterator<String> it = cfg.getEntryKeyTreeIterator();
                    while (it.hasNext())
                        {
                        String key = it.next();
                        ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                        OutputFormatter.putNestedValue(data, key, OutputFormatter.entryValue(entry));
                        }
                    if (ctx.format == Main.OutputFormat.JSON)
                        {
                        System.out.println(OutputFormatter.toJson(data, commonOptions.whitesmiths));
                        }
                    else if (ctx.format == Main.OutputFormat.YAML)
                        {
                        System.out.print(OutputFormatter.toYaml(data, commonOptions.whitesmiths));
                        }
                    else
                        {
                        System.out.print(OutputFormatter.toToml(data, commonOptions.whitesmiths));
                        }
                    }
                if (ctx.showEvents)
                    {
                    OutputFormatter.reportEvents(configFactory.getEvents(), ctx.format);
                    OutputFormatter.reportEvents(cfg.getEvents(), ctx.format);
                    }
                }
            }
        catch (Exception e)
            {
            System.err.println("Error: "+e.getMessage());
            return 1;
            }
        return 0;
        }
}
