package org.metabit.platform.support.config.impl.format.ini;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * INI file format implementation without external dependencies.
 */
public class INIFileFormat implements ConfigFileFormatInterface
{
    private ConfigLoggingInterface logger;
    private ConfigFactorySettings  settings;

    @Override
    public String getFormatID()
        {
        return "INI";
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
        return List.of(".ini");
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation location)
        {
        INIConfigLayer instance = new INIConfigLayer(settings, location, this, file.toPath());
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
            instance.load(reader);
            }
        catch (IOException ex)
            {
            logger.warn("IOException when reading INI file: " + file.getAbsolutePath());
            return null;
            }
        return instance;
        }

    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
        {
        INIConfigLayer instance = new INIConfigLayer(settings, location, this, null);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
            {
            instance.load(reader);
            }
        catch (IOException ex)
            {
            logger.warn("IOException when reading INI stream");
            return null;
            }
        return instance;
        }

    @Override
    public ConfigLayerInterface createFile(Path fileWithPath, ConfigLocation location)
        {
        try
            {
            Files.createFile(fileWithPath);
            return new INIConfigLayer(settings, location, this, fileWithPath);
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
        if (layer instanceof INIConfigLayer)
            {
            INIConfigLayer iniLayer = (INIConfigLayer) layer;
            writeINI(iniLayer.getSource(), iniLayer.getData());
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }

    public void writeINI(ConfigSource source, Map<String, Map<String, String>> data) throws ConfigCheckedException
        {
        Path path = (Path) source.getStorageInstanceHandle();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile())))
            {
            for (Map.Entry<String, Map<String, String>> sectionEntry : data.entrySet())
                {
                String sectionName = sectionEntry.getKey();
                if (!sectionName.isEmpty())
                    {
                    writer.write("[" + sectionName + "]");
                    writer.newLine();
                    }
                for (Map.Entry<String, String> entry : sectionEntry.getValue().entrySet())
                    {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                    }
                writer.newLine();
                }
            }
        catch (IOException ex)
            {
            throw new ConfigCheckedException(ex);
            }
        }
}
