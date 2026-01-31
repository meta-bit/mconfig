package org.metabit.platform.support.config.impl.format.json.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.metabit.platform.support.config.ConfigFeature;
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
import java.text.MessageFormat;
import java.util.List;

/**
 * read and write JSON configuration files.
 * This implementation uses the popular Jackson library.
 */
public class JSONwithJacksonFormat implements ConfigFileFormatInterface
{
    protected ConfigLoggingInterface logger;
    protected ConfigFactorySettings  settings;
    protected ObjectMapper           mapper;

    @Override
    public String getFormatID()
        {
        return "JSON";
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".json"); // maybe also .json5
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface configLoggingInterface)
        {
        this.logger = configLoggingInterface;
        this.settings = configFactorySettings;
        this.mapper = createObjectMapper();
        return true;
        }

    protected ObjectMapper createObjectMapper()
        {
        return new ObjectMapper();
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation configLocation)
        {
        try
            {
            JsonNode rootNode = mapper.readTree(file); // parse to tree
            if (rootNode == null)
                {
                logger.warn("JSON parsing failed on file "+file.getAbsolutePath());
                return null;
                }
            if (rootNode.isObject() || rootNode.isArray())
                {
                // JSON RFCs allow all token types at top level. does not make sense for configurations
                return getJsonJacksonConfigLayer(configLocation, rootNode, file.toPath());
                }
            logger.warn("JSON file "+file.getAbsolutePath()+" having top-level type other than object or array");
            // different constructors?
            }
        catch (JsonProcessingException ex)
            {
            // looks like this is not a valid JSON file!
            String msgstring = MessageFormat.format("not a valid JSON file: {0}:[{1},{2}] because of {3}", file.getAbsolutePath(), ex.getLocation().getLineNr(), ex.getLocation().getColumnNr(), ex.getOriginalMessage());
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
            JsonNode rootNode = mapper.readTree(inputStream); // parse to tree
            return getJsonJacksonConfigLayer(configLocation, rootNode, inputStream);
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
            JsonNode rootNode = mapper.createObjectNode();
            saveJson(rootNode, fileInstance);
            // no separate flush
            return getJsonJacksonConfigLayer(configLocation, rootNode, fileWithPath);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer)
            throws org.metabit.platform.support.config.ConfigCheckedException
        {
        if (layer instanceof JSONJacksonConfigLayer)
            {
            JSONJacksonConfigLayer jsonLayer = (JSONJacksonConfigLayer) layer;
            Path path = (Path) jsonLayer.getSource().getStorageInstanceHandle();
            JsonNode rootNode = jsonLayer.getRootNode();
            try
                {
                saveJson(rootNode, path);
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

    private void saveJson(JsonNode rootNode, Path path)
            throws IOException
        {
        if (settings.getBoolean(ConfigFeature.WRITE_CONDENSED_FORMAT))
            {
            mapper.writeValue(path.toFile(), rootNode);
            }
        else // prettyprinted
            {
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rootNode);
            }
        }

    private JSONJacksonConfigLayer getJsonJacksonConfigLayer(ConfigLocation configLocation, JsonNode rootNode, Object storageInstanceHandle)
        {
        switch (rootNode.getNodeType())
            {
            case OBJECT:
            case ARRAY:
                return new JSONJacksonConfigLayer(settings, logger, configLocation, this, rootNode, storageInstanceHandle);
            default:
                logger.warn("JSON toplevel token type "+rootNode.getNodeType()+" not supported");
                return null;
            }
        }
}
//___EOF___
