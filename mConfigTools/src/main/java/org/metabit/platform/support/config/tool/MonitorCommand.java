package org.metabit.platform.support.config.tool;

import org.metabit.platform.support.config.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Command(name = "monitor", description = "Monitor configuration for changes.", mixinStandardHelpOptions = true)
public class MonitorCommand implements Callable<Integer>
{
    @ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Shortened syntax: [COMPANY:]APPLICATION[:CONFIGNAME]")
    private String shortened;

    @CommandLine.Mixin
    public CommonOptions commonOptions = new CommonOptions();

    @CommandLine.Option(names = {"--dump"}, description = "Dump current configuration before starting monitor.")
    private boolean dump;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Map<String, String> lastValues = new ConcurrentHashMap<>();

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

                // Initial snapshot
                captureSnapshot(cfg);

                if (dump)
                    {
                    printDump(ctx);
                    System.out.println("--------------------------------------------------------------------------------");
                    }

                System.out.println("Monitoring '"+ctx.configName+"' for changes. Press Ctrl+C to stop.");

                cfg.subscribeToUpdates(location->
                    {
                    String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
                    System.out.println("["+timestamp+"] Change detected in source: "+location.toLocationString());
                    if (commonOptions.verbose)
                        {
                        System.out.println("  Scope: "+location.getScope());
                        }

                    reportChanges(cfg);
                    });

                // Keep running until interrupted
                while (!Thread.currentThread().isInterrupted())
                    {
                    Thread.sleep(1000);
                    }
                }
            }
        catch (InterruptedException e)
            {
            // Expected on exit
            Thread.currentThread().interrupt();
            }
        catch (Exception e)
            {
            System.err.println("Error: "+e.getMessage());
            return 1;
            }
        return 0;
        }

    private void captureSnapshot(Configuration cfg)
        {
        lastValues.clear();
        Iterator<String> it = cfg.getEntryKeyTreeIterator();
        while (it.hasNext())
            {
            String key = it.next();
            try
                {
                ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                if (entry != null)
                    {
                    lastValues.put(key, entry.getValueAsString());
                    }
                }
            catch (Exception e)
                {
                // ignore
                }
            }
        }

    private synchronized void reportChanges(Configuration cfg)
        {
        Iterator<String> it = cfg.getEntryKeyTreeIterator();
        Set<String> currentKeys = new HashSet<>();
        while (it.hasNext())
            {
            String key = it.next();
            currentKeys.add(key);
            try
                {
                ConfigEntry entry = cfg.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
                String newValue = (entry != null) ? entry.getValueAsString() : null;
                String oldValue = lastValues.get(key);

                if (oldValue == null)
                    {
                    System.out.println("  [ADDED] "+key+" = "+newValue);
                    lastValues.put(key, newValue);
                    }
                else if (!Objects.equals(oldValue, newValue))
                    {
                    System.out.println("  [CHANGED] "+key+": "+oldValue+" -> "+newValue);
                    lastValues.put(key, newValue);
                    }
                }
            catch (Exception e)
                {
                // ignore
                }
            }

        // Detect deletions
        for (String key : new ArrayList<>(lastValues.keySet()))
            {
            if (!currentKeys.contains(key))
                {
                System.out.println("  [REMOVED] "+key);
                lastValues.remove(key);
                }
            }
        }

    private void printDump(Main.ConfigContext ctx)
        {
        System.out.println("Initial effective configuration for "+ctx.configName+":");
        List<String> sortedKeys = new ArrayList<>(lastValues.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys)
            {
            System.out.println(key+" = "+lastValues.get(key));
            }
        }
}
