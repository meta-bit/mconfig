package org.metabit.library.format.json;
import org.metabit.platform.support.config.scheme.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.scheme.impl.ext.*;

import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.math.BigInteger;

class TestJsonStreamConsumer implements JsonStreamParser.JsonStreamConsumer
{
    private String expected;

    void setExpectedString(final String expectedString)
        { this.expected = expectedString; }

    @Override
    public void consumeString(int line, int column, int level, String string)
        {
        Assertions.assertEquals(expected, string);
        }
    @Override
    public void consumeNull(int line, int column, int level)
        {

        }

    @Override
    public void consumeFalse(int line, int column, int level)
        {

        }

    @Override
    public void consumeTrue(int line, int column, int level)
        {

        }

    @Override
    public void consumeNumberInteger(int line, int column, int level, int i)
        {

        }

    @Override
    public void consumeNumberLong(int line, int column, int level, long l)
        {

        }

    @Override
    public void consumeNumberDouble(int line, int column, int level, double v)
        {

        }

    @Override
    public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
        {

        }

    @Override
    public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
        {

        }

    @Override
    public void consumeObjectEntryStart(int line, int column, int level, String key)
        {

        }

    @Override
    public void consumeObjectEntryEnd(int line, int column, int level)
        {

        }

    @Override
    public void consumeArrayStart(int line, int column, int level)
        {

        }

    @Override
    public void consumeArrayEnd(int line, int column, int level)
        {

        }

    @Override
    public void consumeObjectEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {

        }

    @Override
    public void consumeObjectStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {

        }
}
