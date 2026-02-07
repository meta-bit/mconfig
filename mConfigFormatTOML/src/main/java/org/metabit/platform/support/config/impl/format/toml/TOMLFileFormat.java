package org.metabit.platform.support.config.impl.format.toml;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
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
        {
        try
            {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            TomlParser parser = new TomlParser(content);
            TomlModel.TomlTable root = parser.parse();
            
            TOMLConfigLayer layer = new TOMLConfigLayer(settings, location, this, root, file.toPath());
            if (root.getHeaderComments() != null)
                {
                layer.getGlobalHeaderComments().addAll(root.getHeaderComments());
                }
            return layer;
            }
        catch (TomlParseException e)
            {
            logger.warn("TOML parsing failed at " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        catch (IOException e)
            {
            logger.warn("IOException when reading TOML file: " + file.getAbsolutePath());
            }
        catch (Exception e)
            {
            logger.error(e.getMessage(), e);
            }
        return null;
        }

    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
        {
        try
            {
            byte[] data = inputStream.readAllBytes();
            TomlParser parser = new TomlParser(new String(data, StandardCharsets.UTF_8));
            TomlModel.TomlTable root = parser.parse();
            TOMLConfigLayer layer = new TOMLConfigLayer(settings, location, this, root, null);
            if (root.getHeaderComments() != null)
                {
                layer.getGlobalHeaderComments().addAll(root.getHeaderComments());
                }
            return layer;
            }
        catch (TomlParseException e)
            {
            logger.warn("TOML parsing failed: " + e.getMessage());
            }
        catch (IOException e)
            {
            logger.warn("IOException when reading TOML stream");
            }
        catch (Exception e)
            {
            logger.error(e.getMessage(), e);
            }
        return null;
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
            logger.error("config creation failed at filesystem level: " + fileWithPath, e);
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
