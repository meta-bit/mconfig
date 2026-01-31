package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.Callable;

@Command(name = "propose-scheme", description = "Propose a config scheme for an existing configuration.", mixinStandardHelpOptions = true)
public class ProposeSchemeCommand implements Callable<Integer>
{
    @ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:CONFIGNAME]")
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
            try (ConfigFactory configFactory = ConfigFactoryBuilder.create(ctx.company, ctx.application).build())
                {
                Configuration cfg = configFactory.getConfig(ctx.configName);
                if (ctx.format == Main.OutputFormat.HUMAN)
                    {
                    System.out.println("// Proposed ConfigScheme for "+ctx.configName);
                    System.out.println("{");
                    System.out.println("  \"configurations\": {");
                    System.out.println("    \""+ctx.configName+"\": {");
                    System.out.println("      \"entries\": {");

                    Iterator<String> it = cfg.getEntryKeyTreeIterator();
                    while (it.hasNext())
                        {
                        String key = it.next();
                        ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                        String type = entry.getType().toString();

                        System.out.println("        \""+key+"\": {");
                        System.out.println("          \"type\": \""+type+"\",");
                        System.out.println("          \"description\": \"TODO: description for "+key+"\",");
                        System.out.println("          \"default\": \""+entry.getValueAsString()+"\"");
                        System.out.println("        }"+(it.hasNext() ? "," : ""));
                        }

                    System.out.println("      }");
                    System.out.println("    }");
                    System.out.println("  }");
                    System.out.println("}");
                    }
                else
                    {
                    // For JSON/CSV we could just output the entries list
                    System.err.println("Only HUMAN (JSON-like) output supported for propose-scheme for now.");
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
