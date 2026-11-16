package org.metabit.platform.support.config.impl.format.yaml.jackson;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.logging.NullLogging;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Parse errors are thrown as ConfigParseException: position in the message, the parser's quote of the content only in
 * the parser detail.
 */
class YAMLParseErrorTest
{
    private static YAMLwithJacksonFormat format()
        {
        YAMLwithJacksonFormat format = new YAMLwithJacksonFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());
        return format;
        }

    @Test
    void brokenContentReportsPositionButNotTheContent()
        {
        String yaml = "db:\n  password: \"s3cr3t-VALUE\n  host: x\n";
        ConfigParseException e = assertThrows(ConfigParseException.class,
                ()->format().readStream(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), mock(ConfigLocation.class)));
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertTrue(e.getLine() > 0, e.getMessage());
        assertNotNull(e.getParserDetail());
        }

    @Test
    void emptyContentGivesAnEmptyLayer() throws Exception
        {
        assertNotNull(format().readStream(new ByteArrayInputStream(new byte[0]), mock(ConfigLocation.class)));
        }
}
//___EOF___
