package org.metabit.platform.support.config.impl.format.yaml.jackson;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
            throws ConfigParseException
        {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try
            {
            return toLayer(mapper, mapper.readTree(file), configLocation, file.toPath());
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
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation)
            throws ConfigParseException
        {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try
            {
            return toLayer(mapper, mapper.readTree(inputStream), configLocation, inputStream);
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

    /** empty content gives an empty layer; a configuration needs a mapping or a sequence at top level. */
    private ConfigLayerInterface toLayer(ObjectMapper mapper, JsonNode rootNode, ConfigLocation configLocation, Object storageInstanceHandle)
            throws ConfigParseException
        {
        if (rootNode == null || rootNode.isMissingNode() || rootNode.isNull())
            { rootNode = mapper.createObjectNode(); }
        if (!rootNode.isObject() && !rootNode.isArray())
            {
            throw ConfigParseException.builder(getFormatID(), "top-level value is neither mapping nor sequence")
                    .errorType(rootNode.getNodeType().name())
                    .event(ConfigEvent.Kind.FAILED_GENERIC, "YAML_UNSUPPORTED_TOPLEVEL")
                    .remediation(ConfigEvent.Remediation.FIX_SYNTAX, "Put the configuration into a YAML mapping.")
                    .build();
            }
        return getYamlJacksonConfigLayer(configLocation, rootNode, storageInstanceHandle);
        }

    /** a Jackson error, with position and key path; the parser's message goes in as parser detail only. */
    private ConfigParseException toParseException(JacksonException ex)
        {
        if (ex instanceof JacksonIOException)
            { return readFailure(ex); }
        ConfigParseException.Builder builder = ConfigParseException.builder(getFormatID(), "not valid YAML")
                .parserCause(ex)
                .keyPath(keyPathAtError(ex))
                .event(ConfigEvent.Kind.FAILED_GENERIC, "YAML_PARSE_FAILED")
                .remediation(ConfigEvent.Remediation.FIX_SYNTAX, "Fix invalid YAML syntax.");
        if (ex.getLocation() != null)
            { builder.position(ex.getLocation().getLineNr(), ex.getLocation().getColumnNr()); }
        return builder.build();
        }

    /** the content could not be read at all (I/O, or an unexpected failure). */
    private ConfigParseException readFailure(RuntimeException ex)
        {
        return ConfigParseException.builder(getFormatID(), "could not read")
                .parserCause(ex)
                .event(ConfigEvent.Kind.FAILED_GENERIC, "YAML_READ_FAILED")
                .remediation(ConfigEvent.Remediation.CHECK_PERMISSIONS, "Check that the content is readable.")
                .build();
        }

    /** the JSON pointer of the parser's position when the error occurred, e.g. "/db/password"; null if unknown. */
    static String keyPathAtError(JacksonException ex)
        {
        Object processor = ex.processor();
        if (processor instanceof JsonParser)
            {
            try
                {
                return ((JsonParser) processor).streamReadContext().pathAsPointer().toString();
                }
            catch (RuntimeException ignored)
                {
                // no usable parser state; position and type are still reported
                }
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
