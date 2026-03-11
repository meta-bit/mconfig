package org.metabit.library.format.json;
import org.metabit.platform.support.config.schema.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.schema.impl.ext.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
        DummyJsonStreamConsumer consumer = new DummyJsonStreamConsumer();

        String testObject1 = "{}";
        String testObject2 = "{\"key\":\"value\"}";
        String testObject3 = "{\"key\":4711}";
        String testObject4 = " { \"key\" :  4711    }   ";

        JsonStreamParser.parseJson(testObject1, consumer);
        JsonStreamParser.parseJson(testObject2, consumer);
        JsonStreamParser.parseJson(testObject3, consumer);
        JsonStreamParser.parseJson(testObject4, consumer);

        }
}
//___EOF___
