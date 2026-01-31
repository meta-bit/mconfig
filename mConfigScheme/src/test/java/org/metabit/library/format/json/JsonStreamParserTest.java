package org.metabit.library.format.json;
import org.metabit.platform.support.config.scheme.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.scheme.impl.ext.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

class JsonStreamParserTest
{
    @Test
    void simpleNumberTest1()
        {
        JsonStreamParser jsp = new JsonStreamParser();
        String[] validTestStrings = {"4", "4711", "0", "-12345", "8.15"};
        String[] invalidTestStrings = {"007", "08.15", "--1", "+7"};

        JsonStreamParser.JsonStreamConsumer consumer = new DummyJsonStreamConsumer();

        for (String s : validTestStrings)
            {
            jsp.parse(s, consumer);
            }

        return;
        }

    // ParameterizedTest does not support arrays well
    @Test
    void simpleStringTest()
        {
        JsonStreamParser jsp = new JsonStreamParser();
        String[][] validTestStringPairs = {{"\"\"", ""}, {"\"test\"", "test"}, {"\"\0\"", "\0"}};
        // String[] invalidTestStrings = {"007","08.15","--1","+7"}; try invalid escapses, unicode, and stuff

        TestJsonStreamConsumer consumer = new TestJsonStreamConsumer();

        Arrays.stream(validTestStringPairs).iterator().forEachRemaining(sp->
            {
            consumer.setExpectedString(sp[1]);
            jsp.parse(sp[0], consumer);
            }
        );

        return;
        }


    @Test
    void objectStructureTest()
        {
        DummyJsonStreamConsumer consumer = new DummyJsonStreamConsumer();

        JsonStreamParser jsp = new JsonStreamParser();
        String testObject1 = "{}";
        String testObject2 = "{\"key\":\"value\"}";
        String testObject3 = "{\"key\":4711}";
        String testObject4 = " { \"key\" :  4711    }   ";

        jsp.parse(testObject1, consumer);
        jsp.parse(testObject2, consumer);
        jsp.parse(testObject3, consumer);
        jsp.parse(testObject4, consumer);

        }
}
//___EOF___
