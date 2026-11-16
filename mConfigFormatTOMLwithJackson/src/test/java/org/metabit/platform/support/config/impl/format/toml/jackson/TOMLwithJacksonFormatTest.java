package org.metabit.platform.support.config.impl.format.toml.jackson;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.logging.NullLogging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TOMLwithJacksonFormatTest
{
    @Test
    void getFormatID()
        {
        TOMLwithJacksonFormat format = new TOMLwithJacksonFormat();
        assertEquals("TOMLwithJackson", format.getFormatID(), "getFormatID should return 'TOML'");
        }

    @Test
    void getFilenameExtensions()
        {
        TOMLwithJacksonFormat format = new TOMLwithJacksonFormat();
        assertEquals(List.of(".toml"), format.getFilenameExtensions(), "getFilenameExtensions should return '.toml'");
        }

    @Test
    void duplicateKeysRejected()
            throws ConfigCheckedException
        {
        TOMLwithJacksonFormat format = new TOMLwithJacksonFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());

        String toml = "key = \"first\"\n" +
                "key = \"second\"\n";

        InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = mock(ConfigLocation.class);

        assertThrows(ConfigParseException.class, ()->format.readStream(is, location), "TOML parsing should reject duplicate keys");
        }

    @Test
    void brokenValueReportsPositionButNotTheContent()
        {
        TOMLwithJacksonFormat format = new TOMLwithJacksonFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());

        String toml = "[db]\npassword = s3cr3t-VALUE\n";
        InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8));

        ConfigParseException e = assertThrows(ConfigParseException.class, ()->format.readStream(is, mock(ConfigLocation.class)));
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertEquals(2, e.getLine(), e.getMessage());
        }
}
//___EOF___
