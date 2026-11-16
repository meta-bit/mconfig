package org.metabit.platform.support.config.impl.format.json.jackson;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.logging.NullLogging;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Parse errors are thrown as ConfigParseException: position and key path in the message, the parser's quote of the
 * content (here a secret value) only in the parser detail.
 */
class JSONParseErrorTest
{
    private static ConfigParseException parse(JSONwithJacksonFormat format, String content)
        {
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());
        return assertThrows(ConfigParseException.class,
                ()->format.readStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), mock(ConfigLocation.class)));
        }

    @Test
    void brokenValueReportsPositionAndKeyButNotTheContent()
        {
        ConfigParseException e = parse(new JSONwithJacksonFormat(), "{\n  \"db\": {\n    \"password\": s3cr3t-VALUE\n  }\n}");
        assertFalse(e.getMessage().contains("s3cr3t"), e.getMessage());
        assertEquals(3, e.getLine(), e.getMessage());
        assertEquals("db/password", e.getKeyPath(), e.getMessage());
        assertNotNull(e.getParserDetail());
        assertTrue(e.getParserDetail().contains("s3cr3t"), "the detail keeps the parser's text for the reporter");
        assertEquals(ConfigEvent.Kind.FAILED_GENERIC, e.getEventKind());
        }

    @Test
    void commentsAreReportedAsUnsupportedSyntax()
        {
        ConfigParseException e = parse(new JSONwithJacksonFormat(), "// comment\n{\"key\": \"value\"}");
        assertEquals(ConfigEvent.Kind.UNSUPPORTED_SYNTAX, e.getEventKind());
        assertEquals(ConfigEvent.Remediation.CHANGE_FORMAT, e.getRemediation());
        }

    @Test
    void scalarTopLevelIsRefused()
        {
        ConfigParseException e = parse(new JSONwithJacksonFormat(), "\"just a string\"");
        assertEquals("JSON_UNSUPPORTED_TOPLEVEL", e.getDetailCode());
        }

    @Test
    void emptyContentGivesAnEmptyLayer() throws Exception
        {
        JSONwithJacksonFormat format = new JSONwithJacksonFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());
        assertNotNull(format.readStream(new ByteArrayInputStream(new byte[0]), mock(ConfigLocation.class)));
        }
}
//___EOF___
