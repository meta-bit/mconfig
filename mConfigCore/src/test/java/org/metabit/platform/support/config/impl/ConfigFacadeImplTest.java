package org.metabit.platform.support.config.impl;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.mockups.MockConfigFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigFacadeImplTest
{

    @Test
    void hexConversion()
        {

        byte[] result;

        result = ConfigFacadeImpl.hexDecode("DEADBEEF");
        assertNotNull(result);
        // assertEquals...
        result = ConfigFacadeImpl.hexDecode("DE AD BE EF");
        assertNotNull(result);

        result = ConfigFacadeImpl.hexDecode("DEADBEEF  ; ended before this valid comment");
        assertNotNull(result);

        result = ConfigFacadeImpl.hexDecode("DEADBEEF");
        assertNotNull(result);
        }



        @Test void testBinaryPut()
            {
            // preparing a setup with mock factory and a "dummyTest" configuration.
            // instantiate default settings
            ConfigFactorySettings cfSettings = new ConfigFactorySettings();
            ConfigFactorySettings.initDefaults(new ConfigFactorySettings());
            // which go into a mock context
            ConfigFactoryInstanceContext mockCtx = new ConfigFactoryInstanceContext(cfSettings);
            ConfigFactory mockFactory = new MockConfigFactory(mockCtx);
            ConfigFacadeImpl cfiInstance = new ConfigFacadeImpl("dummyTest",null,mockCtx,mockFactory);
            // we're testing the facade implemetation, not the factory itself. still, checking we can get one and the setup is complete.
            Configuration cfg = cfiInstance.configFactory.getConfig("dummyTest");
            assertNotNull(cfg);
            // preparing the test setup complete.

            byte[] dataToPut = "DEADBEEF".getBytes(StandardCharsets.UTF_8);
            cfiInstance.put("binaryData1", dataToPut, ConfigScope.RUNTIME);

            return;
            }

    @Test
    void getIntegerNull()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.TEST_MODE, true)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false)
                .build())
            {
            Configuration cfg = factory.getConfig("doesntexist");

            Integer emptyInteger = cfg.getInteger("doesntexisteither");
            assertNull(emptyInteger);
            Long emptyLong = cfg.getLong("doesntexisteither");
            assertNull(emptyLong);
            Double emptyDouble = cfg.getDouble("doesntexisteither");
            assertNull(emptyDouble);
            BigInteger emptyBigInteger = cfg.getBigInteger("doesntexisteither");
            assertNull(emptyBigInteger);
            BigDecimal emptyBigDecimal = cfg.getBigDecimal("doesntexisteither");
            assertNull(emptyBigDecimal);
            byte[] emptyBytes = cfg.getBytes("doesntexisteither");
            assertNull(emptyBytes);
            }
        return;
        }


    public static final  String COMPANY_NAME     = "metabit";
    public static final  String APPLICATION_NAME = "mConfig";
    private static final String CONFIG_NAME      = "testconfig";
}