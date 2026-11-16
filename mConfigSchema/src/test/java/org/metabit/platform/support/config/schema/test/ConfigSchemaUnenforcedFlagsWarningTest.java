package org.metabit.platform.support.config.schema.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.schema.impl.ext.JsonConfigSchemaParser;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * schema loading warns about FLAGS values that are not enforced (MLIB-146, M4 stopgap).
 */
public class ConfigSchemaUnenforcedFlagsWarningTest
{
    private ConfigLoggingInterface       logger;
    private ConfigFactoryInstanceContext ctx;

    @BeforeEach
    void setUp()
        {
        logger = mock(ConfigLoggingInterface.class);
        ctx = new ConfigFactoryInstanceContext(new ConfigFactorySettings());
        ctx.setLogger(logger);
        }

    private List<String> warningsFor(String json) throws ConfigCheckedException
        {
        JsonConfigSchemaParser.parseJSON(json, ctx);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeast(0)).warn(captor.capture());
        return captor.getAllValues();
        }

    @Test
    void secretFlagAloneWarnsThatValueIsUnprotected() throws ConfigCheckedException
        {
        List<String> warnings = warningsFor("[{\"KEY\": \"db/password\", \"TYPE\": \"STRING\", \"FLAGS\": [\"SECRET\"]}]");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("db/password"));
        assertTrue(warnings.get(0).contains("NOT protected"));
        assertTrue(warnings.get(0).contains("\"SECRET\": true"));
        }

    @Test
    void secretFlagNextToSecretPropertyIsNotReported() throws ConfigCheckedException
        {
        List<String> warnings = warningsFor("[{\"KEY\": \"db/password\", \"TYPE\": \"STRING\", \"SECRET\": true, \"FLAGS\": [\"SECRET\"]}]");
        assertTrue(warnings.isEmpty(), "unexpected warnings: " + warnings);
        }

    @Test
    void unenforcedFlagsAreListedInOneWarning() throws ConfigCheckedException
        {
        List<String> warnings = warningsFor("[{\"KEY\": \"user/email\", \"TYPE\": \"STRING\", \"FLAGS\": [\"SENSITIVE\", \"READ_ONLY\", \"WRITE_ONLY\"]}]");
        assertEquals(1, warnings.size());
        String warning = warnings.get(0);
        assertTrue(warning.contains("user/email"));
        assertTrue(warning.contains("SENSITIVE"));
        assertTrue(warning.contains("READ_ONLY"));
        assertTrue(warning.contains("WRITE_ONLY"));
        }

    @Test
    void mandatoryFlagIsNotReported() throws ConfigCheckedException
        {
        List<String> warnings = warningsFor("[{\"KEY\": \"server/port\", \"TYPE\": \"NUMBER\", \"FLAGS\": [\"MANDATORY\"]}]");
        assertTrue(warnings.isEmpty(), "unexpected warnings: " + warnings);
        }

    @Test
    void namedFormIsCoveredToo() throws ConfigCheckedException
        {
        List<String> warnings = warningsFor("{\"NAME\": \"network\", \"ENTRIES\": [{\"KEY\": \"api/key\", \"TYPE\": \"STRING\", \"FLAGS\": [\"SECRET\"]}]}");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("api/key"));
        }
}
//___EOF___
