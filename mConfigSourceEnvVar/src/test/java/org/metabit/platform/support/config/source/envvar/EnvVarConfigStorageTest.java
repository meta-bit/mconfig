package org.metabit.platform.support.config.source.envvar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.mockito.ArgumentCaptor;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvVarConfigStorageTest
{
    private EnvVarConfigStorage storage;
    private ConfigFactoryInstanceContext context;
    private ConfigFactorySettings settings;
    private Map<String, String> envVars;
    private ConfigLoggingInterface logger;

    @BeforeEach
    void setUp()
    {
        envVars = new HashMap<>();
        storage = new EnvVarConfigStorage(envVars);
        context = mock(ConfigFactoryInstanceContext.class);
        settings = mock(ConfigFactorySettings.class);
        logger = mock(ConfigLoggingInterface.class);

        when(context.getSettings()).thenReturn(settings);
        when(context.getLogger()).thenReturn(logger);
        when(settings.getString(ConfigFeature.APPLICATION_NAME)).thenReturn("MYAPP");

        storage.init(context);
    }

    @Test
    void testBasicRead() throws ConfigCheckedException
    {
        envVars.put("MYAPP_MYCONFIG_SERVER_PORT", "8080");
        envVars.put("MYAPP_MYCONFIG_SERVER_HOST", "localhost");

        LayeredConfigurationInterface layeredCfg = mock(LayeredConfigurationInterface.class);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, storage, null, null);

        storage.updateConfigurationLayers("myconfig", location, layeredCfg);

        ArgumentCaptor<org.metabit.platform.support.config.interfaces.ConfigLayerInterface> layerCaptor = ArgumentCaptor.forClass(org.metabit.platform.support.config.interfaces.ConfigLayerInterface.class);
        verify(layeredCfg).add(layerCaptor.capture(), eq(location));

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = layerCaptor.getValue();
        assertNotNull(layer.getEntry("server/port"));
        assertEquals("8080", layer.getEntry("server/port").getValueAsString());
        assertEquals("localhost", layer.getEntry("server/host").getValueAsString());
    }

    @Test
    void testNamingConventions() throws ConfigCheckedException
    {
        envVars.put("MYAPP_MYCONFIG_NESTED_KEY_HERE", "value");
        LayeredConfigurationInterface layeredCfg = mock(LayeredConfigurationInterface.class);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, storage, null, null);

        storage.updateConfigurationLayers("myconfig", location, layeredCfg);

        ArgumentCaptor<org.metabit.platform.support.config.interfaces.ConfigLayerInterface> layerCaptor = ArgumentCaptor.forClass(org.metabit.platform.support.config.interfaces.ConfigLayerInterface.class);
        verify(layeredCfg).add(layerCaptor.capture(), eq(location));

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = layerCaptor.getValue();
        assertEquals("value", layer.getEntry("nested/key/here").getValueAsString());
    }

    private org.metabit.platform.support.config.interfaces.ConfigLayerInterface openLayer()
    {
        LayeredConfigurationInterface layeredCfg = mock(LayeredConfigurationInterface.class);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, storage, null, null);
        storage.updateConfigurationLayers("myconfig", location, layeredCfg);
        ArgumentCaptor<org.metabit.platform.support.config.interfaces.ConfigLayerInterface> layerCaptor = ArgumentCaptor.forClass(org.metabit.platform.support.config.interfaces.ConfigLayerInterface.class);
        verify(layeredCfg).add(layerCaptor.capture(), eq(location));
        return layerCaptor.getValue();
    }

    private static List<String> keysOf(org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer)
    {
        List<String> keys = new ArrayList<>();
        layer.tryToGetKeyIterator().forEachRemaining(keys::add);
        Collections.sort(keys);
        return keys;
    }

    /** MLIB-138: "db/host" and "db_host" used to share one variable. */
    @Test
    void underscoreInKeyDoesNotCollideWithSeparator() throws ConfigCheckedException
    {
        envVars.put("MYAPP_MYCONFIG_DB_HOST", "separated");
        envVars.put("MYAPP_MYCONFIG_DB__HOST", "underscored");

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
        assertEquals("separated", layer.getEntry("db/host").getValueAsString());
        assertEquals("underscored", layer.getEntry("db_host").getValueAsString());
    }

    @Test
    void underscoreKeyIsNotReadFromTheSeparatorVariable()
    {
        envVars.put("MYAPP_MYCONFIG_DB_HOST", "separated");

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
        assertNull(layer.getEntry("db_host"));
    }

    @Test
    void enumeratedKeysMatchTheLookup() throws ConfigCheckedException
    {
        envVars.put("MYAPP_MYCONFIG_SERVER_PORT", "8080");
        envVars.put("MYAPP_MYCONFIG_MAX__CONNECTIONS", "5");
        envVars.put("MYAPP_MYCONFIG_POOL_IDLE__TIMEOUT_UNIT", "s");
        envVars.put("OTHERAPP_MYCONFIG_SERVER_PORT", "1");

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
        List<String> keys = keysOf(layer);
        assertEquals(Arrays.asList("max_connections", "pool/idle_timeout/unit", "server/port"), keys);
        for (String key : keys)
            { assertNotNull(layer.getEntry(key), key); }
    }

    /** In a Turkish locale, toUpperCase() without Locale.ROOT turns 'i' into a dotted capital I. */
    @Test
    void namesAreMappedIndependentlyOfTheDefaultLocale() throws ConfigCheckedException
    {
        Locale previous = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR"));
        try
            {
            when(settings.getString(ConfigFeature.APPLICATION_NAME)).thenReturn("minimal");
            envVars.put("MINIMAL_MYCONFIG_LIMITS_IDLE", "7");

            org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
            assertEquals("7", layer.getEntry("limits/idle").getValueAsString());
            assertEquals(Collections.singletonList("limits/idle"), keysOf(layer));
            }
        finally
            {
            Locale.setDefault(previous);
            }
    }

    /** Transition aid until 0.9: a variable in the naming of 0.8.16 and before is reported, once. */
    @Test
    void variableInOldNamingIsReportedOnce()
    {
        envVars.put("MYAPP_MYCONFIG_MAX_CONNECTIONS", "value-that-must-not-appear");

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
        assertNull(layer.getEntry("max_connections"));
        assertNull(layer.getEntry("max_connections"));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(logger, times(1)).warn(message.capture());
        assertTrue(message.getValue().contains("MYAPP_MYCONFIG_MAX_CONNECTIONS"), message.getValue());
        assertTrue(message.getValue().contains("MYAPP_MYCONFIG_MAX__CONNECTIONS"), message.getValue());
        assertFalse(message.getValue().contains("value-that-must-not-appear"), "the value must not be logged");
    }

    @Test
    void noWarningWithoutAVariableInOldNaming()
    {
        envVars.put("MYAPP_MYCONFIG_SERVER_PORT", "8080");

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
        assertNull(layer.getEntry("max_connections"));
        assertNull(layer.getEntry("server/host"));
        verify(logger, never()).warn(anyString());
    }

    /** Lookup is by UPPER CASE name; a lower-case variable must not produce a layer that finds nothing. */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void lowerCaseVariableIsNotPartOfTheLayer()
    {
        envVars.put("myapp_myconfig_server_port", "8080");

        LayeredConfigurationInterface layeredCfg = mock(LayeredConfigurationInterface.class);
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, storage, null, null);
        storage.updateConfigurationLayers("myconfig", location, layeredCfg);
        verify(layeredCfg, never()).add(any(), any());
    }

    /** Only the prefix used to be compared; MYAPP_MYCONFIG_Server_Port was listed as a key the lookup cannot read. */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void mixedCaseVariableIsNotListed() throws ConfigCheckedException
    {
        envVars.put("MYAPP_MYCONFIG_SERVER_PORT", "8080");
        envVars.put("MYAPP_MYCONFIG_Server_Host", "db");

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = openLayer();
        List<String> keys = keysOf(layer);
        assertEquals(Collections.singletonList("server/port"), keys);
        for (String key : keys)
            { assertNotNull(layer.getEntry(key), key); }
        assertNull(layer.getEntry("server/host"));
    }
}
