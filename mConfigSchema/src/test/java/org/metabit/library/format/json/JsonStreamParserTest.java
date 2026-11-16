package org.metabit.library.format.json;
import org.metabit.platform.support.config.schema.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.schema.impl.ext.*;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonStreamParserTest
{
    @Test
    void simpleNumberTest1()
        {
        String[] validTestStrings = {"4", "4711", "0", "-12345", "8.15"};
        String[] invalidTestStrings = {"007", "08.15", "--1", "+7"};

        JsonStreamParser.JsonStreamConsumer consumer = new DummyJsonStreamConsumer();

        for (String s : validTestStrings)
            {
            JsonStreamParser.parseJson(s, consumer);
            }

        return;
        }

    // ParameterizedTest does not support arrays well
    @Test
    void simpleStringTest()
        {
        String[][] validTestStringPairs = {{"\"\"", ""}, {"\"test\"", "test"}, {"\"\0\"", "\0"}};
        // String[] invalidTestStrings = {"007","08.15","--1","+7"}; try invalid escapses, unicode, and stuff

        TestJsonStreamConsumer consumer = new TestJsonStreamConsumer();

        Arrays.stream(validTestStringPairs).iterator().forEachRemaining(sp->
            {
            consumer.setExpectedString(sp[1]);
            JsonStreamParser.parseJson(sp[0], consumer);
            }
        );

        return;
        }


    @Test
    void objectStructureTest()
        {
        assertEquals(Arrays.asList("{", "}"), record("{}"));
        assertEquals(Arrays.asList("{", "key:key", "str:value", "}"), record("{\"key\":\"value\"}"));
        assertEquals(Arrays.asList("{", "key:key", "num:4711", "}"), record("{\"key\":4711}"));
        assertEquals(Arrays.asList("{", "key:key", "num:4711", "}"), record(" { \"key\" :  4711    }   "));
        // a number directly followed by a comma must end the number, not swallow the comma
        assertEquals(Arrays.asList("{", "key:DEFAULT", "num:34", "key:PATTERN", "str:[22, 256]", "}"),
                record("{\"DEFAULT\":34,\"PATTERN\":\"[22, 256]\"}"));
        assertEquals(Arrays.asList("[", "num:34", "num:56", "num:78", "]"), record("[34,56,78]"));
        }

    private static List<String> record(final String json)
        {
        RecordingJsonStreamConsumer consumer = new RecordingJsonStreamConsumer();
        JsonStreamParser.parseJson(json, consumer);
        return consumer.events;
        }

    /** records structure, keys and values as a flat event list; numbers are recorded regardless of their Java type. */
    private static class RecordingJsonStreamConsumer implements JsonStreamParser.JsonStreamConsumer
    {
        private final List<String> events = new ArrayList<>();

        @Override public void consumeObjectStart(int line, int column, int level) { events.add("{"); }
        @Override public void consumeObjectEnd(int line, int column, int level) { events.add("}"); }
        @Override public void consumeArrayStart(int line, int column, int level) { events.add("["); }
        @Override public void consumeArrayEnd(int line, int column, int level) { events.add("]"); }
        @Override public void consumeObjectEntryStart(int line, int column, int level, String key) { events.add("key:"+key); }
        @Override public void consumeString(int line, int column, int level, String string) { events.add("str:"+string); }
        @Override public void consumeNumberInteger(int line, int column, int level, int i) { events.add("num:"+i); }
        @Override public void consumeNumberLong(int line, int column, int level, long l) { events.add("num:"+l); }
        @Override public void consumeNumberBigInteger(int line, int column, int level, BigInteger bigInteger) { events.add("num:"+bigInteger); }
    }
}
//___EOF___
