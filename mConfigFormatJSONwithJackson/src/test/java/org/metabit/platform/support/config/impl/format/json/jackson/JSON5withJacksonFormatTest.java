package org.metabit.platform.support.config.impl.format.json.jackson;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.core.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JSON5withJacksonFormatTest {

    @Test
    void testJSON5Parsing() throws Exception {
        JSON5withJacksonFormat format = new JSON5withJacksonFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());

        String json5 = "{\n" +
                "  // comments\n" +
                "  unquoted: 'single quotes',\n" +
                "  trailing: \"comma\",\n" +
                "  leadingDot: .5,\n" +
                "  plusSign: +10\n" +
                "}";

        InputStream is = new ByteArrayInputStream(json5.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = mock(ConfigLocation.class);

        ConfigLayerInterface layer = format.readStream(is, location);
        assertNotNull(layer);
        assertEquals("single quotes", layer.getEntry("unquoted").getValueAsString());
        assertEquals("comma", layer.getEntry("trailing").getValueAsString());
        assertEquals("0.5", layer.getEntry("leadingDot").getValueAsString());
        assertEquals("10", layer.getEntry("plusSign").getValueAsString());
    }
}
