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
            writeINI(iniLayer);
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }

    public void writeINI(INIConfigLayer layer) throws ConfigCheckedException
        {
        Path path = (Path) layer.getSource().getStorageInstanceHandle();
        Map<String, Map<String, String>> data = layer.getData();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile())))
            {
            for (String comment : layer.getGlobalHeaderComments())
                {
                writer.write(comment);
                writer.newLine();
                }
            if (!layer.getGlobalHeaderComments().isEmpty())
                {
                writer.newLine();
                }
            for (Map.Entry<String, Map<String, String>> sectionEntry : data.entrySet())
                {
                String sectionName = sectionEntry.getKey();
                List<String> sectionLeading = layer.getSectionLeadingComments(sectionName);
                if (sectionLeading != null)
                    {
                    for (String comment : sectionLeading)
                        {
                        writer.write(comment);
                        writer.newLine();
                        }
                    }
                if (!sectionName.isEmpty())
                    {
                    writer.write("[" + sectionName + "]");
                    String sectionInline = layer.getSectionInlineComment(sectionName);
                    if (sectionInline != null)
                        {
                        writer.write(" " + sectionInline);
                        }
                    writer.newLine();
                    }
                for (Map.Entry<String, String> entry : sectionEntry.getValue().entrySet())
                    {
                    List<String> keyLeading = layer.getKeyLeadingComments(sectionName, entry.getKey());
                    if (keyLeading != null)
                        {
                        for (String comment : keyLeading)
                            {
                            writer.write(comment);
                            writer.newLine();
                            }
                        }
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    String inlineComment = layer.getKeyInlineComment(sectionName, entry.getKey());
                    if (inlineComment != null)
                        {
                        writer.write(" " + inlineComment);
                        }
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
