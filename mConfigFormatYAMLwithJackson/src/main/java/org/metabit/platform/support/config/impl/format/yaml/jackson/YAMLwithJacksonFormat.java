package org.metabit.platform.support.config.impl.format.yaml.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;

/**
 * The YAMLwithJacksonFormat class is an implementation of the ConfigFileFormatInterface
 * which facilitates the handling of configuration files in YAML format. This class
 * provides methods to read, write, and validate YAML configuration files leveraging the Jackson library.
 */
public class YAMLwithJacksonFormat implements ConfigFileFormatInterface
{
    private ConfigLoggingInterface logger;
    private ConfigFactorySettings  settings;

    @Override
    public String getFormatID()
        {
        return "YAMLwithJackson";
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".yaml", ".yml");
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface configLoggingInterface)
        {
        this.logger = configLoggingInterface;
        this.settings = configFactorySettings;
        return true;
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation configLocation)
        {
        try
            {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode rootNode = mapper.readTree(file);
            if (rootNode == null)
                {
                logger.warn("YAML parsing failed on file "+file.getAbsolutePath());
                return null;
                }
            if (rootNode.isObject() || rootNode.isArray())
                {
                return getYamlJacksonConfigLayer(configLocation, rootNode, file.toPath());
                }
            logger.warn("YAML file "+file.getAbsolutePath()+" having top-level type other than object or array");
            }
        catch (JsonProcessingException ex)
            {
            String msgstring = MessageFormat.format("not a valid YAML file: {0}:[{1},{2}] because of {3}", file.getAbsolutePath(), ex.getLocation().getLineNr(), ex.getLocation().getColumnNr(), ex.getOriginalMessage());
            logger.warn(msgstring);
            }
        catch (Exception e)
            {
            logger.error(e.getMessage(), e);
            }
        return null;
        }


    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation)
        {
        try
            {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode rootNode = mapper.readTree(inputStream);
            return getYamlJacksonConfigLayer(configLocation, rootNode, inputStream);
            }
        catch (JsonProcessingException ex)
            {
            logger.warn(ex.getMessage(), ex);
            }
        catch (Exception e)
            {
            logger.error(e.getMessage(), e);
            }
        return null;
        }

    @Override
    public ConfigLayerInterface createFile(Path fileWithPath, ConfigLocation configLocation)
        {
        try
            {
            Path fileInstance = Files.createFile(fileWithPath);
            if (!fileInstance.toFile().canWrite())
                {
                logger.error("freshly created file not writeable?!");
                return null;
                }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode rootNode = mapper.createObjectNode();

            if (settings.getBoolean(ConfigFeature.WRITE_CONDENSED_FORMAT))
                {
                mapper.writeValue(fileInstance.toFile(), rootNode);
                }
            else
                {
                ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
                writer.writeValue(fileInstance.toFile(), rootNode);
                }
            return getYamlJacksonConfigLayer(configLocation, rootNode, fileWithPath);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer) throws org.metabit.platform.support.config.ConfigCheckedException
        {
        if (layer instanceof YAMLJacksonConfigLayer)
            {
            YAMLJacksonConfigLayer yamlLayer = (YAMLJacksonConfigLayer) layer;
            Path path = (Path) yamlLayer.getSource().getStorageInstanceHandle();
            JsonNode rootNode = yamlLayer.getRootNode();
            try
                {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                if (settings.getBoolean(ConfigFeature.WRITE_CONDENSED_FORMAT))
                    {
                    mapper.writeValue(path.toFile(), rootNode);
                    }
                else
                    {
                    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
                    writer.writeValue(path.toFile(), rootNode);
                    }
                }
            catch (Exception ex)
                {
                throw new org.metabit.platform.support.config.ConfigCheckedException(ex);
                }
            }
        else
            {
            throw new org.metabit.platform.support.config.ConfigCheckedException(org.metabit.platform.support.config.ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }


    private YAMLJacksonConfigLayer getYamlJacksonConfigLayer(ConfigLocation configLocation, JsonNode rootNode, Object storageInstanceHandle)
        {
        switch (rootNode.getNodeType())
            {
            case OBJECT:
            case ARRAY:
                return new YAMLJacksonConfigLayer(settings, logger, configLocation, this, rootNode, storageInstanceHandle);
            default:
                logger.warn("YAML toplevel token type "+rootNode.getNodeType()+" not supported");
                return null;
            }
        }
}
