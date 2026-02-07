package org.metabit.platform.support.config.impl.format.toml.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.format.json.jackson.JSONJacksonConfigLayer;
import org.metabit.platform.support.config.impl.format.json.jackson.JSONwithJacksonFormat;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * read and write TOML configuration files.
 * This implementation uses the Jackson library with the TOML dataformat extension.
 */
public class TOMLwithJacksonFormat extends JSONwithJacksonFormat
{
    @Override
    public String getFormatID()
        {
        return "TOMLwithJackson";
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".toml");
        }

    @Override
    protected ObjectMapper createObjectMapper()
        {
        return new TomlMapper();
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
                // Ensure the parent directory exists
                if (path.getParent() != null)
                    {
                    java.nio.file.Files.createDirectories(path.getParent());
                    }
                saveToml(rootNode, path);
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

    private void saveToml(JsonNode rootNode, Path path)
            throws IOException
        {
        // TOML doesn't really have a "condensed" vs "pretty" format in the same way JSON does,
        // but we follow the setting for consistency if the mapper supports it.
        if (settings.getBoolean(ConfigFeature.WRITE_CONDENSED_FORMAT))
            {
            mapper.writeValue(path.toFile(), rootNode);
            }
        else
            {
            // TomlMapper has some limitations with pretty printing in some versions,
            // but let's try the default approach.
            mapper.writer().withDefaultPrettyPrinter().writeValue(path.toFile(), rootNode);
            }
        }

    // Since we can't easily override the private getJsonJacksonConfigLayer, 
    // we override the callers to use our custom layer.

    @Override
    public ConfigLayerInterface readFile(java.io.File file, ConfigLocation configLocation)
        {
        try
            {
            JsonNode rootNode = mapper.readTree(file);
            if (rootNode == null)
                {
                logger.warn("TOML parsing failed on file "+file.getAbsolutePath());
                return null;
                }
            return getTomlJacksonConfigLayer(configLocation, rootNode, file.toPath());
            }
        catch (Exception e)
            {
            logger.error(e.getMessage(), e);
            }
        return null;
        }

    @Override
    public ConfigLayerInterface readStream(java.io.InputStream inputStream, ConfigLocation configLocation)
        {
        try
            {
            JsonNode rootNode = mapper.readTree(inputStream);
            return getTomlJacksonConfigLayer(configLocation, rootNode, inputStream);
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
            if (java.nio.file.Files.notExists(fileWithPath))
                {
                if (fileWithPath.getParent() != null)
                    {
                    java.nio.file.Files.createDirectories(fileWithPath.getParent());
                    }
                java.nio.file.Files.createFile(fileWithPath);
                }
            JsonNode rootNode = mapper.createObjectNode();
            saveToml(rootNode, fileWithPath);
            return getTomlJacksonConfigLayer(configLocation, rootNode, fileWithPath);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    private ConfigLayerInterface getTomlJacksonConfigLayer(ConfigLocation configLocation, JsonNode rootNode, Object storageInstanceHandle)
        {
        return new TOMLJacksonConfigLayer(settings, logger, configLocation, this, rootNode, storageInstanceHandle);
        }
}
