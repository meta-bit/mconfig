package org.metabit.platform.support.config.schema;

import org.metabit.platform.support.config.ConfigCheckedException;

public class ConfigSchemaException extends ConfigCheckedException
{
    private final String detail;
    private final int    line;
    private final int    column;

    /**
     * @param message what is wrong with the schema
     * @param line    line of the problem; line and column 0 if it concerns the schema as a whole
     * @param column  column of the problem
     */
    public ConfigSchemaException(String message, int line, int column)
        {
        super(ConfigExceptionReason.INPUT_INVALID);
        this.detail = message;
        this.line = line;
        this.column = column;
        }

    public ConfigSchemaException(String message, Throwable cause, int line, int column)
        {
        super(ConfigExceptionReason.INPUT_INVALID);
        this.detail = message;
        this.line = line;
        this.column = column;
        initCause(cause);
        }

    public int getLine()
        {
        return line;
        }

    public int getColumn()
        {
        return column;
        }

    @Override
    public String getMessage()
        {
        String text = (detail == null) ? super.getMessage() : super.getMessage()+": "+detail;
        if (line <= 0 && column <= 0)
            { return text; }
        return text+" at line "+line+", column "+column;
        }
}
//___EOF___
