package org.metabit.testing.platform.support.config.test.json;

import org.metabit.library.format.json.JsonStreamParser;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * util class for tests.
 * throws an exception on everything by default.
 * replace those functions which are expected.
 */
public class TestJsonConsumer implements JsonStreamParser.JsonStreamConsumer
{
    TestJsonConsumer()
        {
        // set logger if wanted
        }

    @Override
    public void consumeNull(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }

    @Override
    public void consumeFalse(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }

    @Override
    public void consumeTrue(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }



    public void consumeNumberInteger(int line, int column, int level, int i)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }


    @Override
    public void consumeNumberLong(int line, int column, int level, long l)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }

    @Override
    public void consumeNumberDouble(int line, int column, int level, double v)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }

    @Override
    public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }

    @Override
    public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
            throws JsonStreamParser.JsonParsingException
        { throw new UnsupportedOperationException("unexpected in this test"); }

    @Override
    public void consumeObjectEntryStart(int line, int column, int level, String key)
            throws JsonStreamParser.JsonParsingException
        {  }

    @Override
    public void consumeObjectEntryEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {  }

    @Override
    public void consumeArrayStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        { }

    @Override
    public void consumeArrayEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {  }

    @Override
    public void consumeObjectEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {  }

    @Override
    public void consumeObjectStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {  }

    @Override
    public void consumeString(int line, int column, int level, String string)
            throws JsonStreamParser.JsonParsingException
        {  }


}
