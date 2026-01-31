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

import java.util.HashMap;
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

        storage.tryToReadConfigurationLayers("myconfig", location, layeredCfg);

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

        storage.tryToReadConfigurationLayers("myconfig", location, layeredCfg);

        ArgumentCaptor<org.metabit.platform.support.config.interfaces.ConfigLayerInterface> layerCaptor = ArgumentCaptor.forClass(org.metabit.platform.support.config.interfaces.ConfigLayerInterface.class);
        verify(layeredCfg).add(layerCaptor.capture(), eq(location));

        org.metabit.platform.support.config.interfaces.ConfigLayerInterface layer = layerCaptor.getValue();
        assertEquals("value", layer.getEntry("nested/key/here").getValueAsString());
    }
}
