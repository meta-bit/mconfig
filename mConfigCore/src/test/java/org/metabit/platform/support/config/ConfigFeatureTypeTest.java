package org.metabit.platform.support.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.metabit.platform.support.config.ConfigFeature.*;

class ConfigFeatureTypesTest
{

    private ConfigFactoryBuilder newBuilder()
        {
        // minimal valid constructor arguments
        return ConfigFactoryBuilder.create("metabit", "CONFIGTEST");
        }

    // Helper to verify that a runnable throws ConfigException
    private void expectConfigException(Runnable r, String msg)
        {
        ConfigException ex = assertThrows(ConfigException.class, r::run, msg);
        assertNotNull(ex);
        }

    @Test
    void testValidTypeAcceptedForEachFeature()
        {
        ConfigFactoryBuilder b = newBuilder();

        for (ConfigFeature f : ConfigFeature.values())
            {
            switch (f.getType())
                {
                case BOOLEAN:
                {
                // pick a boolean value to set
                assertDoesNotThrow(()->b.setFeature(f, Boolean.TRUE), ()->"Expected BOOLEAN accepted for "+f.name());
                break;
                }
                case STRING:
                {
                // use simple, valid strings; for SUB_PATH avoid edge cases
                String val;
                switch (f)
                    {
                    case SUB_PATH:
                        val = "validSubPath";
                        break;
                    case DEFAULT_TEXTFILE_CHARSET:
                        val = "UTF-8";
                        break;
                    default:
                        val = "test";
                        break;
                    }
                assertDoesNotThrow(()->b.setFeature(f, val), ()->"Expected STRING accepted for "+f.name());
                break;
                }
                case NUMBER:
                {
                // an integer value
                assertDoesNotThrow(()->b.setFeature(f, 1), ()->"Expected NUMBER accepted for "+f.name());
                break;
                }
                case STRINGLIST:
                {
                // a simple list of strings
                List<String> list = Arrays.asList("a", "b");
                assertDoesNotThrow(()->b.setFeature(f, list), ()->"Expected STRINGLIST accepted for "+f.name());
                break;
                }
                case SPECIAL_CLASS:
                {
                // SPECIAL_CLASS features are not set via generic setFeature; use special setters if available.
                switch (f)
                    {
                    case TESTMODE_PARAMETERS:
                    {
                    Map<String, String> ok = new HashMap<>();
                    ok.put("key", "value");
                    assertDoesNotThrow(()->b.setTestParameters(ok), "Expected setTestParameters to accept Map<String,String>");
                    break;
                    }
                    case CONFIG_SCHEME_LIST:
                    {
                    Map<String, org.metabit.platform.support.config.scheme.ConfigScheme> ok = new HashMap<>();
                    // just verify the signature acceptance; actual contents are not part of this type test
                    assertDoesNotThrow(()->b.setSchemes(ok), "Expected setSchemes to accept Map<String, ConfigScheme>");
                    break;
                    }
                    case CURRENT_PLATFORM_OS:
                    {
                    // No public setter exposed; just validate the type reporting API
                    assertTrue(f.isSpecialClassType(org.metabit.platform.support.osdetection.OperatingSystem.class),
                            "CURRENT_PLATFORM_OS should be SPECIAL_CLASS OperatingSystem");
                    break;
                    }
                    case SECRETS_PROVIDER_CONFIG:
                    {
                    Map<String, Object> ok = new HashMap<>();
                    ok.put("key", "value");
                    assertDoesNotThrow(()->b.setSecretsProviderConfig(ok), "Expected setSecretsProviderConfig to accept Map<String,Object>");
                    break;
                    }
                    case ADDITIONAL_SECRETS_PROVIDERS:
                    {
                    Map<String, Object> config = new HashMap<>();
                    assertDoesNotThrow(()->b.addSecretsProvider("test-provider", config, ConfigScope.APPLICATION), "Expected addSecretsProvider to be accepted");
                    break;
                    }
                    default:
                        fail("Unhandled SPECIAL_CLASS feature in test: "+f.name());
                    }
                break;
                }
                default:
                    fail("Unhandled ValueType for feature "+f.name());
                }
            }
        }

    @Test
    void testInvalidTypesRejectedForEachFeature()
        {
        ConfigFactoryBuilder b = newBuilder();

        for (ConfigFeature f : ConfigFeature.values())
            {
            switch (f.getType())
                {
                case BOOLEAN:
                {
                // Try wrong types
                expectConfigException(()->b.setFeature(f, "notBoolean"),
                        "Expected STRING rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, 1),
                        "Expected NUMBER rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, Arrays.asList("a")),
                        "Expected STRINGLIST rejected for "+f.name());
                break;
                }
                case STRING:
                {
                // Try wrong types
                expectConfigException(()->b.setFeature(f, Boolean.TRUE),
                        "Expected BOOLEAN rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, 1),
                        "Expected NUMBER rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, Arrays.asList("a")),
                        "Expected STRINGLIST rejected for "+f.name());
                break;
                }
                case NUMBER:
                {
                // Try wrong types
                expectConfigException(()->b.setFeature(f, Boolean.TRUE),
                        "Expected BOOLEAN rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, "str"),
                        "Expected STRING rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, Arrays.asList("a")),
                        "Expected STRINGLIST rejected for "+f.name());
                break;
                }
                case STRINGLIST:
                {
                // Try wrong types
                expectConfigException(()->b.setFeature(f, Boolean.TRUE),
                        "Expected BOOLEAN rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, 1),
                        "Expected NUMBER rejected for "+f.name());
                expectConfigException(()->b.setFeature(f, "str"),
                        "Expected STRING rejected for "+f.name());
                break;
                }
                case SPECIAL_CLASS:
                {
                // All generic setFeature overloads must refuse SPECIAL_CLASS
                expectConfigException(()->b.setFeature(f, Boolean.TRUE),
                        "Expected BOOLEAN rejected via generic setter for "+f.name());
                expectConfigException(()->b.setFeature(f, 1),
                        "Expected NUMBER rejected via generic setter for "+f.name());
                expectConfigException(()->b.setFeature(f, "str"),
                        "Expected STRING rejected via generic setter for "+f.name());
                expectConfigException(()->b.setFeature(f, Arrays.asList("a")),
                        "Expected STRINGLIST rejected via generic setter for "+f.name());
                break;
                }
                default:
                    fail("Unhandled ValueType for feature "+f.name());
                }
            }
        }

    @Test
    void testSpecialClassSetterTypeValidation()
        {
        // TESTMODE_PARAMETERS: accepts Map<String,String> via setTestParameters
        ConfigFactoryBuilder b1 = newBuilder();
        Map<String, String> ok = new HashMap<>();
        ok.put("key", "value");
        // Ensure test mode is permitted/active before setting test parameters
        b1.setFeature(PERMIT_TEST_MODE, true);
        b1.setFeature(TEST_MODE, true);
        assertDoesNotThrow(()->b1.setTestParameters(ok), "setTestParameters should accept a Map<String,String>");

        // CONFIG_SCHEME_LIST: accepts Map<String, ConfigScheme> via setSchemes
        ConfigFactoryBuilder b2 = newBuilder();
        Map<String, org.metabit.platform.support.config.scheme.ConfigScheme> schemes = new HashMap<>();
        assertDoesNotThrow(()->b2.setSchemes(schemes), "setSchemes should accept a Map<String,ConfigScheme>");
        }

    @Test
    void testTypeReportingHelpers()
        {
        // Basic sanity checks on type helpers
        assertTrue(COMPANY_NAME.isStringType());
        assertTrue(APPLICATION_NAME.isStringType());
        assertTrue(SUB_PATH.isStringType());
        assertTrue(CURRENT_USER_ID.isStringType());
        assertTrue(PERMIT_TEST_MODE.isBooleanType());
        assertTrue(TEST_MODE.isBooleanType());
        assertTrue(QUIET.isBooleanType());
        assertTrue(LOGGING_TO_USE_IN_CONFIGLIB.isStringType());
        assertTrue(LOGLEVEL_NUMBER.isNumberType());
        assertTrue(ADDITIONAL_RUNTIME_DIRECTORIES.isStringListType());
        assertTrue(ADDITIONAL_USER_DIRECTORIES.isStringListType());
        assertTrue(FILE_FORMAT_READING_PRIORITIES.isStringListType());
        assertTrue(FILE_FORMAT_WRITING_PRIORITIES.isStringListType());
        assertTrue(FILE_FORMAT_READING_ALLOW_ALL_FORMATS.isBooleanType());
        assertTrue(FILE_FORMAT_WRITING_ALLOW_ALL_FORMATS.isBooleanType());
        assertTrue(TRIM_TEXTVALUE_SPACES.isBooleanType());
        assertTrue(CONFIG_SCHEME_LIST.isSpecialClassType(Map.class));
        assertTrue(SCHEME_STRICT_MODE.isBooleanType());
        assertTrue(SCHEME_RESETS_DEFAULTS.isBooleanType());
        assertTrue(USE_CONTEXT_CLASS_LOADER.isBooleanType());
        assertTrue(USE_SYSTEM_CLASS_LOADER.isBooleanType());
        assertTrue(WRITE_SYNC.isBooleanType());
        assertTrue(FALLBACKS_ACROSS_SCOPES.isBooleanType());
        assertTrue(WRITE_FALLBACK_ACROSS_SCOPES.isBooleanType());
        assertTrue(UPDATE_CHECK_FREQUENCY_MS.isNumberType());
        assertTrue(WRITE_CONDENSED_FORMAT.isBooleanType());
        assertTrue(UPDATE_CHECK_SCOPES.isStringListType());
        assertTrue(EXCEPTION_ON_MISSING_ENTRY.isBooleanType());
        assertTrue(EXCEPTION_WHEN_CONFIGURATION_NOT_FOUND.isBooleanType());
        assertTrue(FILENAME_EXTENSION_MAPPINGS.isStringListType());
        assertTrue(FREE_CONFIGURATION.isBooleanType());
        assertTrue(NO_DEFAULT_DIRECTORIES.isBooleanType());
        assertTrue(DEFAULT_TEXTFILE_CHARSET.isStringType());
        assertTrue(CURRENT_PLATFORM_OS.isSpecialClassType(org.metabit.platform.support.osdetection.OperatingSystem.class));
        assertTrue(HOSTNAME.isStringType());
        assertTrue(AUTOMATIC_CONFIG_LOADING.isBooleanType());
        assertTrue(AUTOMATIC_CONFIG_CREATION.isBooleanType());
        assertTrue(ALLOW_MCONFIG_RUNTIME_SETTINGS.isBooleanType());
        assertTrue(SECRETS_PROVIDER_ID.isStringType());
        assertTrue(SECRETS_PROVIDER_CONFIG.isSpecialClassType(Map.class));
        }
}
//___EOF___
