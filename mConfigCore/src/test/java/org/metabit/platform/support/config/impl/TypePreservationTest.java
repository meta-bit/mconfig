package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TypePreservationTest
{

    @Test
    public void testTypePreservationInLayeredConfiguration()
            throws ConfigCheckedException
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "testapp");
        ConfigFactory factory = builder.build();
        Configuration cfg = factory.getConfig("testconfig"); // returns empty LayeredConfiguration

        cfg.put("intKey", 42, ConfigScope.SESSION);
        cfg.put("boolKey", true, ConfigScope.SESSION);
        cfg.put("doubleKey", 3.14, ConfigScope.SESSION);
        cfg.put("bigIntKey", new BigInteger("12345678901234567890"), ConfigScope.SESSION);
        cfg.put("bigDecKey", new BigDecimal("123.456"), ConfigScope.SESSION);

        ConfigEntry intEntry = cfg.getConfigEntryFromFullKey("intKey", null);
        ConfigEntry boolEntry = cfg.getConfigEntryFromFullKey("boolKey", null);
        ConfigEntry doubleEntry = cfg.getConfigEntryFromFullKey("doubleKey", null);
        ConfigEntry bigIntEntry = cfg.getConfigEntryFromFullKey("bigIntKey", null);
        ConfigEntry bigDecEntry = cfg.getConfigEntryFromFullKey("bigDecKey", null);

        assertInstanceOf(GenericConfigEntryLeaf.class, intEntry);
        assertEquals(ConfigEntryType.NUMBER, intEntry.getType());
        assertEquals(42, intEntry.getValueAsInteger());

        assertInstanceOf(GenericConfigEntryLeaf.class, boolEntry);
        assertEquals(ConfigEntryType.BOOLEAN, boolEntry.getType());
        assertEquals(true, boolEntry.getValueAsBoolean());

        assertInstanceOf(GenericConfigEntryLeaf.class, doubleEntry);
        assertEquals(ConfigEntryType.NUMBER, doubleEntry.getType());
        assertEquals(3.14, doubleEntry.getValueAsDouble());

        assertInstanceOf(GenericConfigEntryLeaf.class, bigIntEntry);
        assertEquals(new BigInteger("12345678901234567890"), bigIntEntry.getValueAsBigInteger());

        assertInstanceOf(GenericConfigEntryLeaf.class, bigDecEntry);
        assertEquals(new BigDecimal("123.456"), bigDecEntry.getValueAsBigDecimal());
        }
}
