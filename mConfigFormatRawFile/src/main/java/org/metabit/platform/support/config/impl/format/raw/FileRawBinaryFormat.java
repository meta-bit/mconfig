package org.metabit.platform.support.config.impl.format.raw;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * This represents an entire text file as a single byte array.
 * The key for this string is the empty "".
 * <p>
 * Other possible keys are "hex" and "base64", but not implemented yet.
 * No conversion to other types except for bytes.
 */
public class FileRawBinaryFormat implements ConfigFileFormatInterface
{
    @Override
    public String getFormatID()
        {
        return "binary_file";
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger)
        {
        return true;
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".bin"); // or .bcfg? What is most widespread?
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation location)
        {
        FileRawConfigLayer instance = new FileRawConfigLayer(location, this, file.toPath());
        try
            {
            byte[] buffer = Files.readAllBytes(file.toPath());
            instance.setBinaryContents(buffer);
            }
        catch (IOException ex)
            {
            throw new ConfigException(ex);
            }
        return instance;
        }

    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
        {
        FileRawConfigLayer instance = new FileRawConfigLayer(location, this, null);
        try
            {
            byte[] buffer = inputStream.readAllBytes();
            instance.setBinaryContents(buffer);
            }
        catch (IOException ex)
            {
            throw new ConfigException(ex);
            }
        return instance;
        }

    /**
     * @param file
     * @param location
     * @return
     */
    @Override
    public ConfigLayerInterface createFile(Path file, ConfigLocation location)
        {
        try
            {
            Files.createFile(file);
            return new FileRawConfigLayer(location, this, file);
            }
        catch (IOException ex)
            {
            throw new ConfigException(ex);
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer) throws ConfigCheckedException
        {
        if (layer instanceof FileRawConfigLayer)
            {
            FileRawConfigLayer rawLayer = (FileRawConfigLayer) layer;
            Path path = (Path) rawLayer.getSource().getStorageInstanceHandle();
            byte[] bytes = rawLayer.getBinaryContents();
            if (bytes == null && rawLayer.getContents() != null)
                {
                bytes = rawLayer.getContents().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
            if (bytes != null)
                {
                try
                    {
                    Files.write(path, bytes);
                    }
                catch (IOException ex)
                    {
                    throw new ConfigCheckedException(ex);
                    }
                }
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }
}
