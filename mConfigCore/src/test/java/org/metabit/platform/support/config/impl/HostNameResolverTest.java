package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFeature;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HOSTNAME is determined on first read and never fails the factory start (MLIB-138):
 * where the host cannot resolve its own name, the start went down with UnknownHostException.
 */
class HostNameResolverTest
{
    private static final Supplier<String> FAILING = ()->{ throw new IllegalStateException("lookup failed"); };

    @Test
    void firstUsableCandidateWins()
        {
        HostNameResolver resolver = new HostNameResolver(Arrays.asList(()->null, ()->"  ", FAILING, ()->" box-1\n", ()->"other"));
        assertEquals("box-1", resolver.resolve());
        }

    @Test
    void noUsableCandidateGivesNull()
        {
        HostNameResolver resolver = new HostNameResolver(Arrays.asList(()->null, FAILING, ()->"bad\u0000name", ()->"a/b"));
        assertNull(resolver.resolve());
        }

    @Test
    void dotSegmentsAreNotHostNames()
        {
        assertFalse(HostNameResolver.isUsable("."));
        assertFalse(HostNameResolver.isUsable(" ..\n"));
        assertTrue(HostNameResolver.isUsable("box-1.example.com"));
        HostNameResolver resolver = new HostNameResolver(Arrays.asList(()->"..", ()->".", ()->"box-1"));
        assertEquals("box-1", resolver.resolve());
        }

    @Test
    void hostNameIsDeterminedOnFirstReadOnly()
        {
        AtomicInteger calls = new AtomicInteger();
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setHostNameResolver(new HostNameResolver(Collections.singletonList(()->{ calls.incrementAndGet(); return "box-1"; })));
        assertEquals(0, calls.get());
        assertEquals("box-1", settings.getString(ConfigFeature.HOSTNAME));
        assertEquals("box-1", settings.getString(ConfigFeature.HOSTNAME));
        assertEquals(1, calls.get());
        }

    @Test
    void undeterminableHostNameIsNullNotAnError()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setHostNameResolver(new HostNameResolver(Collections.singletonList(FAILING)));
        assertNull(settings.getString(ConfigFeature.HOSTNAME));
        assertFalse(settings.isSet(ConfigFeature.HOSTNAME));
        }

    @Test
    void explicitValueWinsWithoutLookup()
        {
        ConfigFactorySettings settings = new ConfigFactorySettings();
        settings.setHostNameResolver(new HostNameResolver(Collections.singletonList(()->fail("must not be called"))));
        settings.setString(ConfigFeature.HOSTNAME, "given");
        assertEquals("given", settings.getString(ConfigFeature.HOSTNAME));
        }
}
//___EOF___
