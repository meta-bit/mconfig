package org.metabit.testing.platform.support.config.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.LayeredConfiguration;
import org.metabit.platform.support.config.tool.Main;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolFormatConversionIntegrationTest
{
    private static final String COMPANY = "myCompany";
    private static final String APP = "myApp";
    private static final String CONFIG = "myToolConfig";

    @TempDir
    Path tempDir;

    @AfterEach
    void clearTestModeProperty()
        {
        System.clearProperty("TESTMODE_DIRECTORIES");
        }

    @TestFactory
    @DisplayName("mConfigTools: format conversion matrix")
    Stream<DynamicTest> formatConversionMatrix()
        {
        List<Format> formats = List.of(Format.JSON, Format.TOML, Format.YAML, Format.PROPERTIES, Format.INI);
        return formats.stream()
                .flatMap(source -> formats.stream()
                        .filter(target -> target != source)
                        .map(target -> DynamicTest.dynamicTest(source + " -> " + target, () -> runConversion(source, target))));
        }

    private void runConversion(Format sourceFormat, Format targetFormat)
            throws Exception
        {
        Path sourceDir = tempDir.resolve(sourceFormat.name().toLowerCase() + "_source");
        Path targetDir = tempDir.resolve(targetFormat.name().toLowerCase() + "_target");
        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        writeSourceConfig(sourceFormat, sourceDir);

        Configuration sourceConfig = readConfigFrom(sourceDir);
        System.setProperty("TESTMODE_DIRECTORIES", "USER:" + targetDir.toString());

        for (KeySpec keySpec : KeySpec.DEFAULTS)
            {
            ConfigEntryType sourceType = expectedSourceType(sourceFormat, keySpec);
            String value = readValue(sourceConfig, keySpec, sourceType);
            int exit = runSetCommand(keySpec.key, value, sourceType, targetFormat);
            assertEquals(0, exit, "mconfig set failed for " + keySpec.key);
            }

        Configuration targetConfig = readConfigFrom(targetDir);
        for (KeySpec keySpec : KeySpec.DEFAULTS)
            {
            ConfigEntryType expectedTargetType = expectedTargetType(targetFormat, expectedSourceType(sourceFormat, keySpec));
            assertEntryValue(targetConfig, keySpec, expectedTargetType);
            assertEntryType(targetConfig, keySpec.key, expectedTargetType);
            }
        }

    private Configuration readConfigFrom(Path dir)
            throws ConfigCheckedException
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY, APP);
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES, List.of("USER:" + dir.toString()));
        ConfigFactory factory = builder.build();
        return factory.getConfig(CONFIG);
        }

    private int runSetCommand(String key, String value, ConfigEntryType type, Format format)
        {
        CommandLine cmd = new CommandLine(new Main());
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        cmd.setUnmatchedArgumentsAllowed(true);
        return cmd.execute("set",
                "-c", COMPANY,
                "-a", APP,
                "-C", CONFIG,
                "-K", key,
                "-S", "USER",
                "--file-format", format.name(),
                "--type", type.name(),
                "--value", value
        );
        }

    private ConfigEntryType expectedSourceType(Format format, KeySpec keySpec)
        {
        if (format == Format.PROPERTIES || format == Format.INI)
            {
            return ConfigEntryType.STRING;
            }
        return keySpec.type;
        }

    private ConfigEntryType expectedTargetType(Format format, ConfigEntryType sourceType)
        {
        if (format == Format.PROPERTIES || format == Format.INI)
            {
            return ConfigEntryType.STRING;
            }
        return sourceType;
        }

    private String readValue(Configuration cfg, KeySpec keySpec, ConfigEntryType type)
        {
        switch (type)
            {
            case NUMBER:
                if (keySpec.isFloating)
                    {
                    return String.valueOf(cfg.getDouble(keySpec.key));
                    }
                return String.valueOf(cfg.getInteger(keySpec.key));
            case BOOLEAN:
                return String.valueOf(cfg.getBoolean(keySpec.key));
            case STRING:
            default:
                return cfg.getString(keySpec.key);
            }
        }

    private void assertEntryValue(Configuration cfg, KeySpec keySpec, ConfigEntryType type)
        {
        switch (type)
            {
            case NUMBER:
                if (keySpec.isFloating)
                    {
                    assertEquals(keySpec.doubleValue, cfg.getDouble(keySpec.key), 0.0001);
                    }
                else
                    {
                    assertEquals(keySpec.intValue, cfg.getInteger(keySpec.key));
                    }
                break;
            case BOOLEAN:
                assertEquals(keySpec.boolValue, cfg.getBoolean(keySpec.key));
                break;
            case STRING:
            default:
                assertEquals(keySpec.stringValue, cfg.getString(keySpec.key));
                break;
            }
        }

    private void assertEntryType(Configuration cfg, String key, ConfigEntryType expectedType)
        {
        assertTrue(cfg instanceof LayeredConfiguration, "Expected LayeredConfiguration");
        LayeredConfiguration layered = (LayeredConfiguration) cfg;
        ConfigEntry entry = layered.getConfigEntryFromFullKey(key, EnumSet.allOf(ConfigScope.class));
        assertNotNull(entry, "Missing entry for key: " + key);
        assertEquals(expectedType, entry.getType(), "Type mismatch for key: " + key);
        }

    private void writeSourceConfig(Format format, Path dir)
            throws IOException
        {
        Path file = dir.resolve(CONFIG + format.extension);
        String content;
        switch (format)
            {
            case JSON:
                content = "{"
                        + "\"server\":{\"port\":8080,\"pi\":3.14,\"enabled\":true},"
                        + "\"owner\":{\"name\":\"Tom\"}"
                        + "}";
                break;
            case TOML:
                content = "[server]\n"
                        + "port = 8080\n"
                        + "pi = 3.14\n"
                        + "enabled = true\n"
                        + "\n"
                        + "[owner]\n"
                        + "name = \"Tom\"\n";
                break;
            case YAML:
                content = "server:\n"
                        + "  port: 8080\n"
                        + "  pi: 3.14\n"
                        + "  enabled: true\n"
                        + "owner:\n"
                        + "  name: \"Tom\"\n";
                break;
            case PROPERTIES:
                content = "server/port=8080\n"
                        + "server/pi=3.14\n"
                        + "server/enabled=true\n"
                        + "owner/name=Tom\n";
                break;
            case INI:
                content = "[server]\n"
                        + "port=8080\n"
                        + "pi=3.14\n"
                        + "enabled=true\n"
                        + "\n"
                        + "[owner]\n"
                        + "name=Tom\n";
                break;
            default:
                throw new IllegalArgumentException("Unsupported format " + format);
            }
        Files.writeString(file, content, StandardCharsets.UTF_8);
        }

    private enum Format
    {
        JSON(".json"),
        TOML(".toml"),
        YAML(".yaml"),
        PROPERTIES(".properties"),
        INI(".ini");

        private final String extension;

        Format(String extension)
            {
            this.extension = extension;
            }
        }

    private static final class KeySpec
        {
        static final List<KeySpec> DEFAULTS = List.of(
                new KeySpec("server/port", ConfigEntryType.NUMBER, 8080, 0.0, false, false, "8080"),
                new KeySpec("server/pi", ConfigEntryType.NUMBER, 0, 3.14, true, false, "3.14"),
                new KeySpec("server/enabled", ConfigEntryType.BOOLEAN, 0, 0.0, false, true, "true"),
                new KeySpec("owner/name", ConfigEntryType.STRING, 0, 0.0, false, false, "Tom")
        );

        final String key;
        final ConfigEntryType type;
        final int intValue;
        final double doubleValue;
        final boolean isFloating;
        final boolean boolValue;
        final String stringValue;

        KeySpec(String key, ConfigEntryType type, int intValue, double doubleValue, boolean isFloating, boolean boolValue, String stringValue)
            {
            this.key = key;
            this.type = type;
            this.intValue = intValue;
            this.doubleValue = doubleValue;
            this.isFloating = isFloating;
            this.boolValue = boolValue;
            this.stringValue = stringValue;
            }
        }
}
