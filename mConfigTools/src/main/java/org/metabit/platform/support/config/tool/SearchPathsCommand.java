package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigLocation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(name = "search-paths", description = "Show the search list and where mConfig is looking.", mixinStandardHelpOptions = true)
public class SearchPathsCommand implements Callable<Integer>
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
                if (ctx.format == Main.OutputFormat.HUMAN)
                    {
                    System.out.println("Search paths for "+ctx.company+" / "+ctx.application+":");
                    for (ConfigLocation location : configFactory.getSearchList())
                        {
                        System.out.println(" - "+location);
                        }
                    }
                else if (ctx.format == Main.OutputFormat.CSV)
                    {
                    System.out.println("Scope,URI,Writeable");
                    for (ConfigLocation location : configFactory.getSearchList())
                        {
                        System.out.printf("%s,%s,%b%n", location.getScope(), location.toLocationString(), location.isWriteable());
                        }
                    }
                else if (ctx.format == Main.OutputFormat.JSON)
                    {
                    System.out.println("[");
                    int i = 0;
                    int size = configFactory.getSearchList().size();
                    for (ConfigLocation location : configFactory.getSearchList())
                        {
                        System.out.printf("  {\"scope\":\"%s\", \"uri\":\"%s\", \"writeable\":%b}%s%n",
                                location.getScope(), location.toLocationString(), location.isWriteable(),
                                (++i < size ? "," : ""));
                        }
                    System.out.println("]");
                    }
                else if (ctx.format == Main.OutputFormat.YAML || ctx.format == Main.OutputFormat.TOML)
                    {
                    java.util.List<java.util.Map<String, Object>> data = OutputFormatter.newLinkedList();
                    for (ConfigLocation location : configFactory.getSearchList())
                        {
                        java.util.Map<String, Object> row = OutputFormatter.newLinkedMap();
                        row.put("scope", location.getScope().toString());
                        row.put("uri", location.toLocationString());
                        row.put("writeable", location.isWriteable());
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
