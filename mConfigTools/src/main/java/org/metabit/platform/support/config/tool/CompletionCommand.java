package org.metabit.platform.support.config.tool;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "__complete", hidden = true, description = "Generate shell completion script.")
public class CompletionCommand implements Callable<Integer>
{
    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", arity = "0..1", description = "Shell type: bash, zsh, fish", defaultValue = "bash")
    private String shell;

    @Option(names = {"-n", "--name"}, description = "Command name to register completion for; can be repeated or comma-separated")
    private List<String> names = new ArrayList<>();

    @Option(names = {"-o", "--output"}, description = "Write output to file instead of stdout")
    private File output;

    @Override
    public Integer call()
            throws Exception
        {
        CommandLine commandLine = spec.commandLine();
        while (commandLine.getParent() != null)
            {
            commandLine = commandLine.getParent();
            }

        List<String> commandNames = normalizeNames(names);
        if (commandNames.isEmpty())
            {
            commandNames.add("mconfig");
            }

        String script = buildScript(commandLine, commandNames, shell);

        if (output != null)
            {
            if (output.getParentFile() != null && !output.getParentFile().exists())
                {
                if (!output.getParentFile().mkdirs())
                    {
                    System.err.println("Error: Unable to create output directory: "+output.getParentFile());
                    return 1;
                    }
                }
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8)))
                {
                writer.print(script);
                }
            }
        else
            {
            System.out.print(script);
            }
        return 0;
        }

    private static List<String> normalizeNames(List<String> rawNames)
        {
        Set<String> unique = new LinkedHashSet<>();
        for (String name : rawNames)
            {
            if (name == null) continue;
            for (String part : name.split(","))
                {
                String trimmed = part.trim();
                if (!trimmed.isEmpty())
                    {
                    unique.add(trimmed);
                    }
                }
            }
        return new ArrayList<>(unique);
        }

    private static String buildScript(CommandLine commandLine, List<String> commandNames, String shell)
        {
        String normalized = shell == null ? "bash" : shell.trim().toLowerCase();
        StringBuilder script = new StringBuilder();
        boolean first = true;
        for (String name : commandNames)
            {
            if (!first)
                {
                script.append(System.lineSeparator());
                }
            first = false;
            script.append(generateCompletion(normalized, name, commandLine));
            }
        return script.toString();
        }

    private static String generateCompletion(String shell, String name, CommandLine commandLine)
        {
        switch (shell)
            {
            case "bash":
                return AutoComplete.bash(name, commandLine);
            case "zsh":
            case "fish":
                return invokeAutoComplete(shell, name, commandLine);
            default:
                throw new CommandLine.ParameterException(commandLine, "Unsupported shell: "+shell);
            }
        }

    private static String invokeAutoComplete(String shell, String name, CommandLine commandLine)
        {
        try
            {
            Method method = AutoComplete.class.getMethod(shell, String.class, CommandLine.class);
            Object result = method.invoke(null, name, commandLine);
            if (result instanceof String)
                {
                return (String) result;
                }
            throw new CommandLine.ParameterException(commandLine, "Unexpected completion result for shell: "+shell);
            }
        catch (NoSuchMethodException e)
            {
            throw new CommandLine.ParameterException(commandLine,
                    "Shell completion not available in this picocli version for: "+shell, e);
            }
        catch (Exception e)
            {
            throw new CommandLine.ParameterException(commandLine,
                    "Failed to generate completion for shell: "+shell, e);
            }
        }
}
