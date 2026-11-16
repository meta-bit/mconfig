package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFactoryBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An invalid company, application or sub-path argument names the argument in the exception message.
 */
class BuilderNameArgumentTest
{
    @Test
    void nullCompanyNameIsNamed()
        {
        ConfigException e = assertThrows(ConfigException.class, ()->ConfigFactoryBuilder.create(null, "app"));
        assertEquals(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID, e.getReason());
        assertTrue(e.getMessage().contains("COMPANY_NAME"), e.getMessage());
        }

    @Test
    void nullApplicationNameIsNamed()
        {
        ConfigException e = assertThrows(ConfigException.class, ()->ConfigFactoryBuilder.create("company", null));
        assertEquals(ConfigException.ConfigExceptionReason.ARGUMENT_INVALID, e.getReason());
        assertTrue(e.getMessage().contains("APPLICATION_NAME"), e.getMessage());
        }

    @Test
    void invalidSubPathIsNamedAndEscaped()
        {
        ConfigException e = assertThrows(ConfigException.class, ()->ConfigFactoryBuilder.create("company", "app", "a/../b\n"));
        assertTrue(e.getMessage().contains("SUB_PATH"), e.getMessage());
        assertFalse(e.getMessage().contains("\n"), e.getMessage());
        }
}
//___EOF___
