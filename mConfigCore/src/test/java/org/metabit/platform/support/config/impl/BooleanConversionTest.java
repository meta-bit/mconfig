package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.BooleanLiterals;


import static org.junit.jupiter.api.Assertions.*;

/**
 * since 0.8.17, getBoolean() no longer reads every value but "true" as false; LENIENT_BOOLEAN_CONVERSION restores that.
 */
class BooleanConversionTest
{
    private static ConfigFactory factory(String application, boolean lenient)
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", application);
        builder.setTestMode(true);
        if (lenient)
            { builder.setFeature(ConfigFeature.LENIENT_BOOLEAN_CONVERSION, true); }
        return builder.build();
        }

    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", "True", "yes", "YES", "on", "On", "1", " true ", "\tyes"})
    void trueLiterals(String text)
        { assertEquals(Boolean.TRUE, BooleanLiterals.parse(text)); }

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "no", "No", "off", "OFF", "0", " off "})
    void falseLiterals(String text)
        { assertEquals(Boolean.FALSE, BooleanLiterals.parse(text)); }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "maybe", "enabled", "2", "-1", "t", "y", "truee", "yes please"})
    void notBooleans(String text)
        { assertNull(BooleanLiterals.parse(text)); }

    @Test
    void strictByDefault() throws Exception
        {
        try (ConfigFactory factory = factory("BOOL_STRICT", false))
            {
            Configuration cfg = factory.getConfig("flags");
            cfg.put("a", "yes", ConfigScope.RUNTIME);
            cfg.put("b", "Off", ConfigScope.RUNTIME);
            cfg.put("c", "maybe-secret-text", ConfigScope.RUNTIME);
            cfg.put("d", "", ConfigScope.RUNTIME);

            assertEquals(Boolean.TRUE, cfg.getBoolean("a"));
            assertEquals(Boolean.FALSE, cfg.getBoolean("b"));
            ConfigException thrown = assertThrows(ConfigException.class, ()->cfg.getBoolean("c"));
            assertEquals(ConfigException.ConfigExceptionReason.CONVERSION_FAILURE, thrown.getReason());
            assertFalse(String.valueOf(thrown.getMessage()).contains("maybe-secret-text"), "the value stays out of the message");
            assertThrows(ConfigException.class, ()->cfg.getBoolean("d"), "an empty value is no Boolean");
            }
        }

    @Test
    void lenientAsPlainJava() throws Exception
        {
        try (ConfigFactory factory = factory("BOOL_LENIENT", true))
            {
            Configuration cfg = factory.getConfig("flags");
            cfg.put("a", "TRUE", ConfigScope.RUNTIME);
            cfg.put("b", "yes", ConfigScope.RUNTIME);
            cfg.put("c", "maybe", ConfigScope.RUNTIME);

            assertEquals(Boolean.TRUE, cfg.getBoolean("a"));
            assertEquals(Boolean.FALSE, cfg.getBoolean("b"), "Boolean.parseBoolean: only \"true\" is true");
            assertEquals(Boolean.FALSE, cfg.getBoolean("c"));
            }
        }

    @Test
    void featureValuesAreStrictToo()
        {
        assertTrue(SelfConfigurator.parseFeatureBoolean("yes"));
        assertFalse(SelfConfigurator.parseFeatureBoolean("0"));
        assertThrows(IllegalArgumentException.class, ()->SelfConfigurator.parseFeatureBoolean("ture"));
        }
}
//___EOF___
