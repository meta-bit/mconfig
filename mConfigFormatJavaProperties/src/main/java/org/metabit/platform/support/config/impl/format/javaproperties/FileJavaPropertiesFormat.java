package org.metabit.platform.support.config.impl.format.javaproperties;

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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class FileJavaPropertiesFormat implements ConfigFileFormatInterface
{
    private ConfigLoggingInterface logger;
    private ConfigFactorySettings  settings;

    @Override
    public String getFormatID()
        {
        return "properties";
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
        return List.of(".properties");
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation location)
        {
        JavaPropertiesConfigLayer instance = new JavaPropertiesConfigLayer(settings, location, this, file.toPath());
        try
            {
            instance.load(new BufferedReader(new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)));
            }
        catch (FileNotFoundException ex)
            {
            logger.error("previously existing Java properties file denied its existence when reading"); //
            return null;
            }
        catch (IOException ex)
            {
            logger.warn("IOException when reading Java Properties");
            return null;
            }
        return instance;
        }

    /**
     * @param inputStream the input stream to read Java properties from
     * @param location the location of the configuration
     * @return the configuration layer instance
     */
    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
        {
        JavaPropertiesConfigLayer instance = new JavaPropertiesConfigLayer(settings, location, this, null);
        try
            {
            instance.load(new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)));
            }
        catch (FileNotFoundException ex)
            {
            logger.error("previously existing Java properties file denied its existence when reading"); //
            return null;
            }
        catch (IOException ex)
            {
            logger.warn("IOException when reading Java Properties");
            return null;
            }
        return instance;
        }

    /**
     * @param fileWithPath the path to the file to create
     * @param location the location of the configuration
     * @return the configuration layer instance
     */
    @Override
    public ConfigLayerInterface createFile(Path fileWithPath, ConfigLocation location)
        {
        try
            {
            Path fileInstance = Files.createFile(fileWithPath);
            if (! fileInstance.toFile().canWrite())
                {
                logger.error("freshly created file not writeable?!");
                return null;
                }
            JavaPropertiesConfigLayer instance = new JavaPropertiesConfigLayer(settings, location, this, fileInstance);
            // nothing to load() / read from freshly created file.
            return instance;
            }
        catch (IOException e)
            {
            logger.error("config creation failed at filesystem level",e);
            return null;
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer) throws ConfigCheckedException
        {
        if (layer instanceof JavaPropertiesConfigLayer)
            {
            JavaPropertiesConfigLayer propsLayer = (JavaPropertiesConfigLayer) layer;
            writeProperties(propsLayer);
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }

    public void writeProperties(JavaPropertiesConfigLayer layer) throws ConfigCheckedException
        {
        try
            {
            Path path = (Path) layer.getSource().getStorageInstanceHandle();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile()), java.nio.charset.StandardCharsets.UTF_8)))
                {
                for (String comment : layer.internalGetGlobalHeaderComments())
                    {
                    writer.write(comment);
                    writer.newLine();
                    }
                if (!layer.internalGetGlobalHeaderComments().isEmpty())
                    {
                    writer.newLine();
                    }
                for (String key : layer.internalGetOrdered().keySet())
                    {
                    List<String> comments = layer.internalGetLeadingComments().get(key);
                    if (comments != null)
                        {
                        for (String comment : comments)
                            {
                            writer.write(comment);
                            writer.newLine();
                            }
                        }
                    String value = layer.internalGetOrdered().get(key);
                    writer.write(escapeKey(key));
                    writer.write("=");
                    writer.write(escapeValue(value));
                    writer.newLine();
                    }
                for (String comment : layer.internalGetTrailingComments())
                    {
                    writer.write(comment);
                    writer.newLine();
                    }
                }
            }
        catch (IOException ex)
            {
            throw new ConfigCheckedException(ex);
            }
        }

    private String escapeKey(String key)
        {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++)
            {
            char ch = key.charAt(i);
            switch (ch)
                {
                case ' ':
                case '\\':
                case '=':
                case ':':
                case '#':
                case '!':
                    sb.append('\\').append(ch);
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    sb.append(ch);
                    break;
                }
            }
        return sb.toString();
        }

    private String escapeValue(String value)
        {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++)
            {
            char ch = value.charAt(i);
            switch (ch)
                {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case ' ':
                    if (i == 0)
                        {
                        sb.append("\\ ");
                        break;
                        }
                    sb.append(' ');
                    break;
                default:
                    sb.append(ch);
                    break;
                }
            }
        return sb.toString();
        }
}
