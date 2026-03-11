package org.metabit.platform.support.config.tool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

/**
 * A Jackson {@link com.fasterxml.jackson.core.PrettyPrinter} that implements the "Moderate Whitesmiths" indentation style.
 * <p>
 * Key characteristics of Whitesmiths style in this implementation:
 * <ul>
 *   <li>Opening and closing braces/brackets are indented at the same level as the content they contain.</li>
 *   <li>Each level of nesting adds indentation.</li>
 * </ul>
 */
public class ModerateWhitesmithsPrettyPrinter extends DefaultPrettyPrinter
{
    private static final long serialVersionUID = 1L;

    public ModerateWhitesmithsPrettyPrinter()
        {
        super();
        _objectIndenter = new WhitesmithsIndenter();
        _arrayIndenter = new WhitesmithsIndenter();
        }

    public ModerateWhitesmithsPrettyPrinter(ModerateWhitesmithsPrettyPrinter base)
        {
        super(base);
        }

    @Override
    public ModerateWhitesmithsPrettyPrinter createInstance()
        {
        return new ModerateWhitesmithsPrettyPrinter(this);
        }

    @Override
    public void writeRootValueSeparator(JsonGenerator g) throws IOException
        {
        g.writeRaw(System.lineSeparator());
        }

    @Override
    public void writeStartObject(JsonGenerator g) throws IOException
        {
        if (_nesting > 0)
            {
            _objectIndenter.writeIndentation(g, _nesting);
            }
        _nesting++;
        g.writeRaw('{');
        }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException
        {
        _nesting--;
        if (nrOfEntries > 0)
            {
            _objectIndenter.writeIndentation(g, _nesting);
            }
        g.writeRaw('}');
        }

    @Override
    public void writeStartArray(JsonGenerator g) throws IOException
        {
        if (_nesting > 0)
            {
            _arrayIndenter.writeIndentation(g, _nesting);
            }
        _nesting++;
        g.writeRaw('[');
        }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException
        {
        _nesting--;
        if (nrOfValues > 0)
            {
            _arrayIndenter.writeIndentation(g, _nesting);
            }
        g.writeRaw(']');
        }

    /**
     * Custom indenter for Whitesmiths style.
     */
    public static class WhitesmithsIndenter extends FixedSpaceIndenter
    {
        @Override
        public void writeIndentation(JsonGenerator g, int level) throws IOException
            {
            g.writeRaw(System.lineSeparator());
            for (int i = 0; i < level; i++)
                {
                g.writeRaw("    "); // 4 spaces per level
                }
            }

        @Override
        public boolean isInline() { return false; }
    }
}
