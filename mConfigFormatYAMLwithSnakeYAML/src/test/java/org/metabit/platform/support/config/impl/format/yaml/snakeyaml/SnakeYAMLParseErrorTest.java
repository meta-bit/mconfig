package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

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
 * SnakeYAML parse errors: position (1-based) in the message, the snippet of the content only in the parser detail.
 * SnakeYAML cannot tell the key at the error position.
 */
class SnakeYAMLParseErrorTest
{
    @Test
    void brokenContentReportsPositionButNotTheContent()
        {
        YAMLwithSnakeYAMLFormat format = new YAMLwithSnakeYAMLFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());
        String yaml = "db:\n  password: s3cr3t-VALUE: broken\n";

        ConfigParseException e = assertThrows(ConfigParseException.class,
                ()->format.readStream(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), mock(ConfigLocation.class)));
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertEquals(2, e.getLine(), e.getMessage());
        assertNull(e.getKeyPath());
        assertNotNull(e.getParserDetail());
        }
}
//___EOF___
