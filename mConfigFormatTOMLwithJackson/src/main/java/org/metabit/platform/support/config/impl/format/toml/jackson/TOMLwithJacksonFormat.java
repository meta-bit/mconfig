package org.metabit.platform.support.config.impl.format.toml.jackson;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.toml.TomlMapper;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.format.json.jackson.JSONJacksonConfigLayer;
import org.metabit.platform.support.config.impl.format.json.jackson.JSONwithJacksonFormat;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;

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
    protected String syntaxName()
        {
        return "TOML";
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
            throws ConfigParseException
        {
        try
            {
            return getTomlJacksonConfigLayer(configLocation, mapper.readTree(file), file.toPath());
            }
        catch (JacksonException ex)
            {
            throw toParseException(ex);
            }
        catch (RuntimeException e)
            {
            throw readFailure(e);
            }
        }

    @Override
    public ConfigLayerInterface readStream(java.io.InputStream inputStream, ConfigLocation configLocation)
            throws ConfigParseException
        {
        try
            {
            return getTomlJacksonConfigLayer(configLocation, mapper.readTree(inputStream), inputStream);
            }
        catch (JacksonException ex)
            {
            throw toParseException(ex);
            }
        catch (RuntimeException e)
            {
            throw readFailure(e);
            }
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

    /** TOML always has a table at top level; empty content gives an empty layer. */
    private ConfigLayerInterface getTomlJacksonConfigLayer(ConfigLocation configLocation, JsonNode rootNode, Object storageInstanceHandle)
        {
        if (rootNode == null || rootNode.isMissingNode())
            { rootNode = mapper.createObjectNode(); }
        return new TOMLJacksonConfigLayer(settings, logger, configLocation, this, rootNode, storageInstanceHandle);
        }
}
//___EOF___
