package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Validate a configuration against its scheme.", mixinStandardHelpOptions = true)
public class ValidateCommand implements Callable<Integer>
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
                if (cfg.getConfigScheme().isNullScheme())
                    {
                    System.out.println("No config scheme defined for '"+ctx.configName+"'. Validation is not possible.");
                    System.out.println("Use 'propose-scheme' to generate a scheme from current values.");
                    return 1;
                    }
                int errors = 0;
                java.util.List<java.util.Map<String, Object>> errorList = new java.util.ArrayList<>();

                Iterator<String> it = cfg.getEntryKeyTreeIterator();
                while (it.hasNext())
                    {
                    String key = it.next();
                    ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                    try
                        {
                        ConfigEntrySpecification spec = entry.getSpecification();
                        if (spec != null)
                            {
                            if (!spec.validateEntry(entry))
                                {
                                System.err.println("Validation failed for key: "+key+" (Value: "+entry.getValueAsString()+")");
                                errors++;
                                java.util.Map<String, Object> error = OutputFormatter.newLinkedMap();
                                error.put("key", key);
                                error.put("value", entry.getValueAsString());
                                error.put("message", "Value does not match specification");
                                errorList.add(error);
                                }
                            }
                        else if (ctx.verbose)
                            {
                            System.out.println("No specification for key: "+key);
                            }
                        }
                    catch (ConfigCheckedException e)
                        {
                        if (ctx.verbose) System.out.println("No specification for key: "+key);
                        }
                    }

                if (errors == 0)
                    {
                    if (ctx.format == Main.OutputFormat.HUMAN)
                        {
                        System.out.println("Configuration "+ctx.configName+" is valid.");
                        }
                    }
                else
                    {
                    if (ctx.format == Main.OutputFormat.HUMAN)
                        {
                        System.err.println("Configuration "+ctx.configName+" has "+errors+" validation errors.");
                        }
                    }
                if (ctx.format == Main.OutputFormat.CSV)
                    {
                    System.out.println("Key,Value,Message");
                    for (java.util.Map<String, Object> error : errorList)
                        {
                        System.out.printf("%s,%s,%s%n", error.get("key"), error.get("value"), error.get("message"));
                        }
                    }
                else if (ctx.format == Main.OutputFormat.JSON)
                    {
                    System.out.println("{");
                    System.out.printf("  \"config\": \"%s\",%n", ctx.configName);
                    System.out.printf("  \"valid\": %b,%n", errors == 0);
                    System.out.println("  \"errors\": [");
                    for (int i = 0; i < errorList.size(); i++)
                        {
                        java.util.Map<String, Object> error = errorList.get(i);
                        System.out.printf("    {\"key\":\"%s\", \"value\":\"%s\", \"message\":\"%s\"}%s%n",
                                error.get("key"), error.get("value"), error.get("message"),
                                (i+1 < errorList.size() ? "," : ""));
                        }
                    System.out.println("  ]");
                    System.out.println("}");
                    }
                else if (ctx.format == Main.OutputFormat.YAML || ctx.format == Main.OutputFormat.TOML)
                    {
                    java.util.Map<String, Object> data = OutputFormatter.newLinkedMap();
                    data.put("config", ctx.configName);
                    data.put("valid", errors == 0);
                    data.put("errors", errorList);
                    if (ctx.format == Main.OutputFormat.YAML)
                        {
                        System.out.print(OutputFormatter.toYaml(data));
                        }
                    else
                        {
                        System.out.print(OutputFormatter.toToml(data));
                        }
                    }
                return (errors == 0 ? 0 : 1);
                }
            }
        catch (Exception e)
            {
            System.err.println("Error: "+e.getMessage());
            return 1;
            }
        }
}
