package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.EnumSet;
import java.util.concurrent.Callable;

@Command(name = "get", description = "State value and metadata for a specific entry.", mixinStandardHelpOptions = true)
public class GetCommand implements Callable<Integer>
{
    @ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:CONFIGNAME[:KEY]]")
    private String shortened;

    @CommandLine.Parameters(index = "1", arity = "0..1", description = "Key (optional)")
    private String keyArg;

    @CommandLine.Mixin
    public CommonOptions commonOptions = new CommonOptions();

    @CommandLine.Option(names = {"-S", "--scope"}, description = "Scopes to search (comma-separated)", split = ",")
    private ConfigScope[] scopes;

    @Override
    public Integer call()
        {
        if (shortened != null)
            {
            main.shortened = shortened;
            }
        if (keyArg != null && commonOptions.key == null)
            {
            commonOptions.key = keyArg;
            }
        Main.ConfigContext ctx = new Main.ConfigContext(main, commonOptions);
        try
            {
            ctx.validate(true, true);
            try (ConfigFactory configFactory = ConfigFactoryBuilder.create(ctx.company, ctx.application).build())
                {
                Configuration cfg = configFactory.getConfig(ctx.configName);
                EnumSet<ConfigScope> scopeSet = EnumSet.allOf(ConfigScope.class);
                if (scopes != null && scopes.length > 0)
                    {
                    scopeSet = EnumSet.copyOf(java.util.Arrays.asList(scopes));
                    }
                ConfigEntry entry = cfg.getConfigEntryFromFullKey(ctx.key, scopeSet);
                if (entry == null)
                    {
                    System.err.println("Key not found: "+ctx.key);
                    return 1;
                    }

                if (ctx.format == Main.OutputFormat.HUMAN)
                    {
                    System.out.println("Key:         "+ctx.key);
                    System.out.println("Value:       "+entry.getValueAsString());
                    System.out.println("Type:        "+entry.getType());
                    System.out.println("Scope:       "+entry.getScope());
                    System.out.println("Source:      "+entry.getLocation());

                    try
                        {
                        ConfigEntrySpecification spec = entry.getSpecification();
                        if (spec != null)
                            {
                            String desc = spec.getDescription();
                            // Simple logic to try and parse a JSON-like description for multi-language
                            if (desc != null && desc.startsWith("{") && desc.contains(ctx.language))
                                {
                                try
                                    {
                                    // Extremely primitive parsing since we don't want to add a JSON parser dependency just for this
                                    String langKey = "\""+ctx.language+"\":";
                                    int start = desc.indexOf(langKey);
                                    if (start != -1)
                                        {
                                        start += langKey.length();
                                        int end = desc.indexOf("\"", start);
                                        if (end != -1)
                                            {
                                            end = desc.indexOf("\"", end+1);
                                            if (end != -1)
                                                {
                                                desc = desc.substring(desc.indexOf("\"", start)+1, end);
                                                }
                                            }
                                        }
                                    }
                                catch (Exception ignored) { }
                                }
                            System.out.println("Description: "+desc);
                            if (spec.getDefaultEntry() != null)
                                {
                                System.out.println("Default:     "+spec.getDefaultEntry().getValueAsString());
                                }
                            }
                        }
                    catch (ConfigCheckedException ignored) { }
                    }
                else if (ctx.format == Main.OutputFormat.CSV)
                    {
                    System.out.println("Key,Value,Type,Scope,URI");
                    System.out.printf("%s,%s,%s,%s,%s%n", ctx.key, entry.getValueAsString(), entry.getType(), entry.getScope(), entry.getLocation());
                    }
                else if (ctx.format == Main.OutputFormat.JSON)
                    {
                    System.out.printf("{\"key\":\"%s\", \"value\":\"%s\", \"type\":\"%s\", \"scope\":\"%s\", \"uri\":\"%s\"}%n",
                            ctx.key, entry.getValueAsString(), entry.getType(), entry.getScope(), entry.getLocation());
                    }
                else if (ctx.format == Main.OutputFormat.YAML || ctx.format == Main.OutputFormat.TOML)
                    {
                    java.util.Map<String, Object> data = OutputFormatter.newLinkedMap();
                    data.put("key", ctx.key);
                    data.put("value", OutputFormatter.entryValue(entry));
                    data.put("type", entry.getType().toString());
                    data.put("scope", entry.getScope().toString());
                    data.put("uri", entry.getLocation().toString());
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
