package org.metabit.platform.support.config.scheme;

import org.metabit.platform.support.config.ConfigCheckedException;

public class ConfigSchemeException extends ConfigCheckedException
{
    private final int line;
    private final int column;

    public ConfigSchemeException(String message, int line, int column)
        {
        super(ConfigExceptionReason.INPUT_INVALID);
        this.line = line;
        this.column = column;
        }

    public ConfigSchemeException(String message, Throwable cause, int line, int column)
        {
        super(ConfigExceptionReason.INPUT_INVALID);
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
        return super.getMessage()+" at line "+line+", column "+column;
        }
}
