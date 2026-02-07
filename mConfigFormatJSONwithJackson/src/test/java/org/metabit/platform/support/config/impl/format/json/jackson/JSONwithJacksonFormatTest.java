package org.metabit.platform.support.config.impl.format.json.jackson;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JSONwithJacksonFormatTest
{

    @Test
    void getFormatID()
        {
        JSONwithJacksonFormat format = new JSONwithJacksonFormat();
        assertEquals("JSONwithJackson", format.getFormatID(), "getFormatID should return 'JSON'");
        }

    @Test
    void getFilenameExtensions()
        {
        JSONwithJacksonFormat format = new JSONwithJacksonFormat();
        assertEquals(List.of(".json"), format.getFilenameExtensions(), "getFilenameExtensions should return '.json'");
        }

}