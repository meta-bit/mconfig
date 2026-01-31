package org.metabit.platform.support.config.impl.format.raw;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * This represents an entire text file as a single string.
 * The key for this string is the empty ""; all other values are denied for now.
 */
public class FileRawTextFormat implements ConfigFileFormatInterface
{
private ConfigLoggingInterface logger;
private Charset charset;


@Override
public String getFormatID()
    {
    return "text_file";
    }

@Override
public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger)
    {
    this.logger = logger;
    try
        {
        this.charset = Charset.forName(configFactorySettings.getString(ConfigFeature.DEFAULT_TEXTFILE_CHARSET));
        }
    catch (Exception ex)
        {
        logger.warn("default charset \"" + configFactorySettings.getString(ConfigFeature.DEFAULT_TEXTFILE_CHARSET) + "\" invalid, textfile disabled");
        return false;
        }
    return true;
    }

@Override
public List<String> getFilenameExtensions()
    {
    return List.of(".cfg", ".txt");
    }

@Override
public ConfigLayerInterface readFile(File file, ConfigLocation location)
    {
    FileRawConfigLayer instance = new FileRawConfigLayer(location, this, file.toPath());
    try
        {
        // language level 11 this.buffer = Files.readString(file.toPath());
        String buffer = new String(Files.readAllBytes(file.toPath()), this.charset); // standard charset  ISO8859_1; we could go with UTF-8 explicitly if set.
        instance.setContents(buffer);
        }
    catch (IOException ex)
        {
        throw new ConfigException(ex);
        }
    return instance;
    }


    /**
     * @param inputStream 
     * @param location
     * @return
     */
    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
        {
        FileRawConfigLayer instance = new FileRawConfigLayer(location, this, null);
        try
            {
            // language level 11 this.buffer = Files.readString(file.toPath());
            String buffer = new String(inputStream.readAllBytes(), this.charset); // standard charset  ISO8859_1; we could go with UTF-8 explicitly if set.
            instance.setContents(buffer);
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
            String buffer = rawLayer.getContents();
            if (buffer == null && rawLayer.getBinaryContents() != null)
                {
                buffer = new String(rawLayer.getBinaryContents(), this.charset);
                }
            if (buffer != null)
                {
                try
                    {
                    Files.write(path, buffer.getBytes(this.charset));
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
