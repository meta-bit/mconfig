package org.metabit.testing.platform.support.config.test.json;

import org.metabit.library.format.json.JsonStreamParser;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DebugJsonStreamConsumer implements JsonStreamParser.JsonStreamConsumer
{
    @Override
    public void consumeNull(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume null at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeFalse(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume false at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeTrue(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume true at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeString(int line, int column, int level, String string)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume string at ["+line+":"+column+", ^ "+level+"] with content string \""+string+"\"");
        }

    @Override
    public void consumeNumberInteger(int line, int column, int level, int i)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberInteger at line "+line+" column "+column+" level "+level+" i "+i);
        }

    @Override
    public void consumeNumberLong(int line, int column, int level, long l)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberLong at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeNumberDouble(int line, int column, int level, double v)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberDouble at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberBigInteger at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeNumberBigDecimal(int line, int column, int level, BigDecimal bigDecimal)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume numberBigDecimal at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeObjectEntryStart(int line, int column, int level, String key)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume objectEntryStart at ["+line+":"+column+", ^ "+level+"] with key \""+key+"\"");
        }

    @Override
    public void consumeObjectEntryEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume objectEntryEnd at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeArrayStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume arrayStart at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeArrayEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume arrayEnd at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeObjectEnd(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume objectEnd at line "+line+" column "+column+" level "+level);
        }

    @Override
    public void consumeObjectStart(int line, int column, int level)
            throws JsonStreamParser.JsonParsingException
        {
        System.err.println("consume objectStart at line "+line+" column "+column+" level "+level);
        }
}
