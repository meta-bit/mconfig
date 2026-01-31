package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.ConfigDiscoveryInfo;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "list", description = "List all available configurations.", mixinStandardHelpOptions = true)
public class ListCommand implements Callable<Integer>
{
    @ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION")
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
            ctx.validate(false, false);
            try (ConfigFactory configFactory = ConfigFactoryBuilder.create(ctx.company, ctx.application).build())
                {
                Set<ConfigDiscoveryInfo> configs = configFactory.listAvailableConfigurations();
                if (ctx.format == Main.OutputFormat.HUMAN)
                    {
                    System.out.println("Available configurations for "+ctx.company+" / "+ctx.application+":");
                    System.out.printf("%-20s %-15s %s%n", "Name", "Scope", "URI");
                    for (ConfigDiscoveryInfo info : configs)
                        {
                        System.out.printf("%-20s %-15s %s%n", info.getConfigName(), info.getScope(), info.getUri());
                        }
                    }
                else if (ctx.format == Main.OutputFormat.CSV)
                    {
                    System.out.println("Name,Scope,URI,Format,Writeable");
                    for (ConfigDiscoveryInfo info : configs)
                        {
                        System.out.printf("%s,%s,%s,%s,%b%n", info.getConfigName(), info.getScope(), info.getUri(), info.getFormatID(), info.isWriteable());
                        }
                    }
                else if (ctx.format == Main.OutputFormat.JSON)
                    {
                    // Minimal JSON manually to avoid extra dependencies for now
                    System.out.println("[");
                    int i = 0;
                    for (ConfigDiscoveryInfo info : configs)
                        {
                        System.out.printf("  {\"name\":\"%s\", \"scope\":\"%s\", \"uri\":\"%s\", \"format\":\"%s\", \"writeable\":%b}%s%n",
                                info.getConfigName(), info.getScope(), info.getUri(), info.getFormatID(), info.isWriteable(),
                                (++i < configs.size() ? "," : ""));
                        }
                    System.out.println("]");
                    }
                else if (ctx.format == Main.OutputFormat.YAML || ctx.format == Main.OutputFormat.TOML)
                    {
                    java.util.List<java.util.Map<String, Object>> data = OutputFormatter.newLinkedList();
                    for (ConfigDiscoveryInfo info : configs)
                        {
                        java.util.Map<String, Object> row = OutputFormatter.newLinkedMap();
                        row.put("name", info.getConfigName());
                        row.put("scope", info.getScope().toString());
                        row.put("uri", info.getUri().toString());
                        row.put("format", info.getFormatID());
                        row.put("writeable", info.isWriteable());
                        data.add(row);
                        }
                    if (ctx.format == Main.OutputFormat.YAML)
                        {
                        System.out.print(OutputFormatter.toYaml(data));
                        }
                    else
                        {
                        System.out.print(OutputFormatter.toToml(data));
                        }
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
