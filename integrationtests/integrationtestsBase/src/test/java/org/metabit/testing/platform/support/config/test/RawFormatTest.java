package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RawFormatTest
{
    public static final String COMPANY_NAME     = "metabit";
    public static final String APPLICATION_NAME = "mConfigIT";

    @Test
    void testRawTextFormat()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                // Ensure text_file format is preferred or at least allowed
                .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, List.of("text_file"))
                .build())
            {
            Configuration cfg = factory.getConfig("rawText");
            assertNotNull(cfg, "Configuration 'rawText' should not be null");
            assertFalse(cfg.isEmpty(), "Configuration 'rawText' should not be empty");
            
            String content = cfg.getString("");
            assertEquals("Hello World\n", content);
            }
        }

    @Test
    void testRawBinaryFormat()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, List.of("binary_file"))
                .build())
            {
            Configuration cfg = factory.getConfig("rawBinary");
            assertNotNull(cfg, "Configuration 'rawBinary' should not be null");
            assertFalse(cfg.isEmpty(), "Configuration 'rawBinary' should not be empty");
            
            byte[] content = cfg.getBytes("");
            assertArrayEquals(new byte[]{1, 2, 3, 4, 10}, content); // 10 is the newline added by echo
            }
        }
}
