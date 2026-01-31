package org.metabit.testing.platform.support.config.test.json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metabit.library.format.json.JsonStreamParser;
import org.metabit.library.format.json.JsonStreamWriter;


import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonStringConversionTests
{
@Test
void testHardcodedBasics()
    {
    JsonStreamParser jsp = new JsonStreamParser();
    JsonTestStringExtractor tmpOutput = new JsonTestStringExtractor();

    String test1 =  "{\"testKey\":\"testValue\"}";
    jsp.parse(test1,tmpOutput);
    assertEquals("testKey", tmpOutput.getKey());
    assertEquals("testValue", tmpOutput.getBuffer());

    String test2 = "{\"testKey\":\"" + JsonStreamWriter.jsonEscapeStringContent("a \"naughty\" value with a quote in it") + "\"}";
    jsp.parse(test2,tmpOutput);
    assertEquals("testKey", tmpOutput.getKey());
    assertEquals("a \"naughty\" value with a quote in it", tmpOutput.getBuffer());
    }

    @Test
    void testControlCharacterEscaping()
        {
        JsonStreamParser jsp = new JsonStreamParser();
        JsonTestStringExtractor tmpOutput = new JsonTestStringExtractor();

        final char[] controlCharacters = new char[32];
        for (int i=0; i<32; i++)
            controlCharacters[i] = (char) i; //
        String testString = Arrays.toString(controlCharacters);

        String encoded = JsonStreamWriter.jsonEscapeStringContent(testString);
        String testJson = "{\"testKey\":\"" + encoded + "\"}";
        // decode again
        jsp.parse(testJson, tmpOutput); // would throw an  JsonStreamParser.JsonParsingException on parsing errors.
        // check expected key we provided
        assertEquals("testKey", tmpOutput.getKey());
        // compare original input and the encoded-decoded result
        assertEquals(testString, tmpOutput.getBuffer());
        return;
        }


    @Test void testByteArrayEncodingToBase64()
        {
        final byte[] test = {(byte) 0xAB, (byte) 0xAD, (byte) 0xFE, (byte) 0xED};
        final byte[] value = test;
        String encoded = "base64:" + Base64.getEncoder().encodeToString(value);
        System.out.println(encoded);
        String trimmed = encoded.replace("base64:", "");
        byte[] decoded = Base64.getDecoder().decode(trimmed);
        assertArrayEquals(value,decoded);
        }

    // or to hex.

    /**
     * and for good measure, do some fuzzing on JSON Strings.
     */
 @ParameterizedTest
 @ValueSource(ints = { 100,255,65536 })
    void escapingWeirdUnicodeCharacters(int maxInt)
     {
     int numLoops = 1000;
     // int maxStringLength = 1000;
     int maxStringLength = 1000;

     Random rng = new Random();
     char[] buffer = new char[maxStringLength];

     JsonStreamParser jsp = new JsonStreamParser();
     JsonTestStringExtractor tmpOutput = new JsonTestStringExtractor();

     for (int i=0; i<numLoops; i++)
         {
         // generate test string of variable length with random content
         int testStringLength = rng.nextInt(maxStringLength);
         int j = 0;
         while (j <testStringLength)
             {
             int ic = 32+rng.nextInt(maxInt-32); // excluding control characters here; a different tests checks for these.
             if (Character.isValidCodePoint(ic) && Character.isDefined(ic))
                 {
                 buffer[j] = (char)ic;
                 j++;
                 }
             // else keep looping
             }
         String testString = String.valueOf( buffer,0,testStringLength);
         // encode for JSON
         String encoded = JsonStreamWriter.jsonEscapeStringContent(testString);
         String testJson = "{\"testKey\":\"" + encoded + "\"}";
         // decode again
         jsp.parse(testJson, tmpOutput); // would throw an  JsonStreamParser.JsonParsingException on parsing errors.
         // check expected key we provided
         assertEquals("testKey", tmpOutput.getKey());
         // compare original input and the encoded-decoded result
         assertEquals(testString, tmpOutput.getBuffer());
         }
     return;
     }


    // ignore all inputs. can be used as a util class, e.g. for tests
    static class JsonTestStringExtractor extends TestJsonConsumer implements JsonStreamParser.JsonStreamConsumer
    {
        private String buffer;
        private String key;

        public String getBuffer() { return buffer; }
        public String getKey() { return key; }


        @Override
        public void consumeString(int line, int column, int level, String string)
                throws JsonStreamParser.JsonParsingException
            {
            this.buffer = string;
            }


        public void consumeObjectEntryStart(int line, int column, int level, String key)
                throws JsonStreamParser.JsonParsingException
            {
             this.key = key;
            }

        @Override
        public void consumeObjectEntryEnd(int line, int column, int level)
                throws JsonStreamParser.JsonParsingException
            {

            }


    }

}
