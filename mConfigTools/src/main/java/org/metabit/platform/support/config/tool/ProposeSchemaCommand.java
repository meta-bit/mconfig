package org.metabit.platform.support.config.tool;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * placeholder for the schema proposal command.
 * <p>
 * Disabled: the previous implementation printed secret values in plain text, used the
 * effective value of any scope as DEFAULT, and emitted a shape mConfig does not load.
 * It is being rewritten (MLIB-137, scheduled with the schema format work of MLIB-135).
 * The command stays registered so that callers get a clear message, not "unknown command".
 */
@Command(name = "propose-schema", description = "Propose a config schema for an existing configuration (currently disabled).", mixinStandardHelpOptions = true)
public class ProposeSchemaCommand implements Callable<Integer>
{
    @CommandLine.Unmatched
    private String[] ignored; // accept the former arguments and options, so the message below is shown

    @Override
    public Integer call()
        {
        System.err.println("propose-schema is disabled in this version; it is being rewritten. No output was produced.");
        return 1;
        }
}
//___EOF___
