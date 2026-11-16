package org.metabit.platform.support.config.impl.format.toml;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.impl.format.toml.TomlParser.TomlParseException;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TOMLFileFormat implements ConfigFileFormatInterface
{
    private ConfigLoggingInterface logger;
    private ConfigFactorySettings settings;

    @Override
    public String getFormatID()
        {
        return "TOML";
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger)
        {
        this.logger = logger;
        this.settings = configFactorySettings;
        return true;
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".toml");
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation location)
            throws ConfigParseException
        {
        String content;
        try
            {
            content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
        catch (IOException|RuntimeException e)
            {
            throw readFailure(e);
            }
        return parse(content, location, file.toPath());
        }

    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
            throws ConfigParseException
        {
        String content;
        try
            {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        catch (IOException|RuntimeException e)
            {
            throw readFailure(e);
            }
        return parse(content, location, null);
        }

    /** parse the content into a layer; a syntax error becomes a ConfigParseException with position and key. */
    private ConfigLayerInterface parse(String content, ConfigLocation location, Path file)
            throws ConfigParseException
        {
        TomlParser parser = new TomlParser(content);
        try
            {
            TomlModel.TomlTable root = parser.parse();
            TOMLConfigLayer layer = new TOMLConfigLayer(settings, location, this, root, file);
            if (root.getHeaderComments() != null)
                {
                layer.getGlobalHeaderComments().addAll(root.getHeaderComments());
                }
            return layer;
            }
        catch (TomlParseException e)
            {
            // the parser's message may quote keys or values; it goes in as parser detail only
            throw ConfigParseException.builder(getFormatID(), "not valid TOML")
                    .position(e.getLine(), e.getColumn())
                    .keyPath(parser.currentKeyPath())
                    .parserCause(e)
                    .event(ConfigEvent.Kind.FAILED_GENERIC, "TOML_PARSE_FAILED")
                    .remediation(ConfigEvent.Remediation.FIX_SYNTAX, "Fix invalid TOML syntax.")
                    .build();
            }
        catch (RuntimeException e)
            {
            throw ConfigParseException.builder(getFormatID(), "not valid TOML")
                    .keyPath(parser.currentKeyPath())
                    .parserCause(e)
                    .event(ConfigEvent.Kind.FAILED_GENERIC, "TOML_PARSE_FAILED")
                    .remediation(ConfigEvent.Remediation.FIX_SYNTAX, "Fix invalid TOML syntax.")
                    .build();
            }
        }

    private ConfigParseException readFailure(Exception e)
        {
        return ConfigParseException.builder(getFormatID(), "could not read")
                .parserCause(e)
                .event(ConfigEvent.Kind.FAILED_GENERIC, "TOML_READ_FAILED")
                .remediation(ConfigEvent.Remediation.CHECK_PERMISSIONS, "Check that the content is readable.")
                .build();
        }

    @Override
    public ConfigLayerInterface createFile(Path fileWithPath, ConfigLocation location)
        {
        try
            {
            if (Files.notExists(fileWithPath))
                {
                if (fileWithPath.getParent() != null)
                    {
                    Files.createDirectories(fileWithPath.getParent());
                    }
                Files.createFile(fileWithPath);
                }
            return new TOMLConfigLayer(settings, location, this, new TomlModel.TomlTable(false), fileWithPath);
            }
        catch (IOException e)
            {
            logger.error("config creation failed at filesystem level: " + LogSafe.text(fileWithPath), e);
            return null;
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer) throws ConfigCheckedException
        {
        if (!(layer instanceof TOMLConfigLayer))
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        TOMLConfigLayer tomlLayer = (TOMLConfigLayer) layer;
        Object handle = tomlLayer.getSource().getStorageInstanceHandle();
        if (!(handle instanceof Path))
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        Path path = (Path) handle;
        try
            {
            TomlWriter writer = new TomlWriter();
            String content = writer.write(tomlLayer.getRoot());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            }
        catch (IOException e)
            {
            throw new ConfigCheckedException(e);
            }
        }
}
