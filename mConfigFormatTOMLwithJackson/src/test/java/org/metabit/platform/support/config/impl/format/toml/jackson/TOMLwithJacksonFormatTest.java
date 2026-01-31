package org.metabit.platform.support.config.impl.format.toml.jackson;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TOMLwithJacksonFormatTest
{
    @Test
    void getFormatID()
        {
        TOMLwithJacksonFormat format = new TOMLwithJacksonFormat();
        assertEquals("TOML", format.getFormatID(), "getFormatID should return 'TOML'");
        }

    @Test
    void getFilenameExtensions()
        {
        TOMLwithJacksonFormat format = new TOMLwithJacksonFormat();
        assertEquals(List.of(".toml"), format.getFilenameExtensions(), "getFilenameExtensions should return '.toml'");
        }
}
